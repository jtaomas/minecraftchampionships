# McmDecisionDome 2.0

A faithful **egg-and-chicken Decision Dome** vote provider for `MinecraftMondayCore` 3.2.0 and later.
It is not an `McmGame`; on enable it registers itself with `McmApi.setVoteProvider(...)` and the core
calls it between championship events.

The old pad-occupancy/weighted-random vote has been replaced. Players now throw **one egg** from their
**team pod**; the egg hatches into a **real chicken**, the chicken wanders, and the **wedge it occupies
when the dividers seal** is the vote.

---

## 1. Rules implemented

| Rule | Implementation |
|---|---|
| One egg per player | Tagged `Material.EGG` with session/kind/owner PDC, one handed out per voter |
| Throw at the dial | Real `Egg` projectile from the pod; vanilla 1/8 hatch cancelled |
| One chicken per egg | `EggService` -> `ChickenService`; one logical chicken, one real entity |
| Chicken's wedge is the vote | `VoteEngine` moves logical chickens; `VoteTally` maps the position at tally |
| 30 s vote + 13 s settle | `VOTING` then `SETTLE`; throws stop, in-flight eggs and power-ups stay live |
| Dividers seal the chickens | `DIVIDERS` raises the venue dividers and freezes logical movement |
| Plurality | Highest signed score wins; no weighted draw anywhere |
| Tie = uniform among leaders | `VoteTally.count` collects only the leaders and rolls uniformly among them |
| Signed votes | Chicken +1, frog −1, mega chicken +3, mega frog −3; negative totals legal |
| Chosen game removed | `REMOVE` blanks the winning wedge and removes its chickens |
| Dial refill | 8-game dial, 6-game variant, 15-game ceiling; top-up before the refill vote |
| Confirmation Dome | One game left -> no vote, no eggs, short confirmation presentation |
| Hidden counts | Labels show only names until `Counting chickens...`; counts appear at reveal |
| Power-ups | Per-team MVP set; Game 1 and audience vote excluded; a dunk strips the team |

## 2. Phase clock

Normal and fast-test (`/mcm fast on`) durations. Every wait goes through the engine and is clamped to
at most one second in fast-test.

| Phase | Normal | Fast-test | What happens |
|---|---:|---:|---|
| `OPENING` | 5 s | 1 s | Teleport to pods, eggs handed out, power-ups announced |
| `VOTING` | 30 s | 1 s | Throws enabled; no live counts |
| `SETTLE` | 13 s | 1 s | Throws off; in-flight eggs resolve; items still usable |
| `DIVIDERS` | 1.5 s | 0.5 s | Dividers rise; chickens sealed |
| `TALLY` | 1 s | 0.5 s | One immutable count, tie roll, log |
| `REVEAL` | 5 s | 1 s | Winning wedge, counts, title, fireworks |
| `REMOVE` | 2 s | 0.5 s | Winner wedge blanked; its chickens removed |
| `REFILL` | 2 s | 1 s | Refill presentation; the live top-up happens before the refill vote |
| `TELEPORT` | 8 s | 1 s | `TELEPORTING IN`; result handed to the core |

Total ≈ **67.5 s**, inside the core's 90 s vote fallback.

## 3. HUD

Exactly one boss bar, in one position:

```
VOTING BEGINS IN: 00:05
VOTING CLOSES IN: 00:30      (the only place the vote clock appears)
GAME CHOSEN IN: 00:16        (settle + dividers + tally)
TELEPORTING IN: 00:17        (reveal + remove + refill + teleport)
```

- No action bar and no repeating countdown titles.
- One title at the reveal: the game name + `Teleporting soon...`.
- Chat narration: `Counting chickens...`, `The chosen game is...`, then the game name.
- Vote counts are hidden on the dial until the reveal.

## 4. Dial, removal and refill

- `dial.capacity` selects **8** (default) or **6** wedges; the 15-game ceiling is enforced.
- Wedge 0 is centred on the 90° entrance axis in the fallback geometry. The hub view derives its base
  angle from `McmLobby.Venue.wedgeCenter(0)`.
- A chosen game is removed from its slot and the slot stays blank until the dial is topped up. Before
  the refill vote the number of blank slots follows the number of votes already played; from
  `dial.refill-vote` (default 6) the dial fills from the remaining pool as slots free up.
- Refill is announced with `New Game Added!` and the new game names.
- With one live game left the session becomes a **Confirmation Dome**: `OPENING` -> `REVEAL` ->
  `TELEPORT`, no `VOTING`.

## 5. Venues

`DomeController` prefers the core's hub venue when `McmLobby.venue().apiVersion() >= 2`:

- **`HubDialView`** uses `McmLobby.Venue` v2: `setDial`, `wedgeCenter`, `setWedge`, `podSpawn`,
  `setPodGate`, `setPodState`, `setDividers`, `clearRuntime`, `revealWedge`, `revealSurface`,
  `interiorClock`, `showCountdown`.
- **`FallbackDialView`** uses the plugin-owned `mcc_dome` world: generated dial, 10 simple pods,
  rising dividers, labels and reveal. It is also used for non-MCC events and whenever a v2 venue is
  not available yet.
- **v1 compatibility:** an older venue (`apiVersion() < 2`) falls back to `mcc_dome` automatically;
  `/dome test hub` can still exercise the v1 pad-compatible hub view.

The plugin never hardcodes a hub coordinate. The venue owns static geometry and runtime block
mutations; the plugin owns eggs, chickens, power-ups, labels and session cleanup.

## 6. Entities and cleanup

- Chickens/frogs are real entities with no AI, no awareness, no collision, no gravity, no despawning,
  no egg laying and no damage. A mega mob is **one** entity with `GENERIC_SCALE`, not three.
- PDC carries session, owner, team, kind and weight. Scoreboard tags carry
  `mcm_dome:<session>:chicken` / `:egg` / `:ui`.
- `DomeSession` removes only its own session tags, then `venue.clearRuntime()` restores divider,
  wedge, pod and label state.
- `ChickenService.sweepStale` removes tagged leftovers on plugin enable.
- The result callback is guarded by an `AtomicBoolean`: exactly once on normal completion, never on
  core cancel or plugin disable, and always once on an internal error.

## 7. Power-ups

Per team, one use, announced in chat with the reason. Excluded from the Game 1 vote and the audience
vote. A dunked pod loses all remaining power-ups.

**MVP (default):** Glacia, Mitosis, Waxing, Waning, Swapping, Sticking, Coin of Fate, Spellcheck,
Swinging Watch, Frog Egg, Mega Chicken Egg, Mega Frog Egg.

**Phase 2 (`powerups.phase-2: true`):** Levitas, Infernus, Petrification, Transmutation, Cage,
Freezing, Dunking. Pod curses use `setPodState` / `setPodGate` where the v2 venue provides them.

Power-up targeting is intentionally simple in this build: spells pick a random chicken or wedge,
curses pick a random enemy pod, relics affect the team/dome. The item + chat line + world effect are
the UI; there is no custom inventory menu and no boss-bar power-up HUD.

## 8. Commands (`mcm.dome.admin`, default op)

| Command | Behaviour |
|---|---|
| `/dome test [hub\|dome]` | Demo vote; no callback; players returned afterwards |
| `/dome end` | Close voting now; settle/dividers/tally still run |
| `/dome skipwait` | End now with the current/forced tally |
| `/dome cancel` | Test: stop and return; real: hand `null` to the core |
| `/dome status` | Phase, session, seed, venue, dial, chickens, eggs, power-ups, tally |
| `/dome tp [hub\|dome] [pod\|dial]` | Teleport to the active dome or a pod/dial |
| `/dome simulate <n>` | Headless seeded votes (tally/tie/removal/refill) |

## 9. Configuration

`config.yml` controls the dial, every phase duration, chicken limits and the power-up set. Defaults
match the timings in section 2. `debug` is reserved for verbose logging.

## 10. Source map

| File | Role |
|---|---|
| `VoteEngine` | Pure Java phase machine, movement, tally, tie, removal, refill |
| `Dial`, `Wedge`, `DialGeometry` | Board, slots, refill pool, wedge maths |
| `Egg`, `Chicken`, `VoteTally`, `PowerUp`, `Phase` | Pure model |
| `TeamPod` | Team room model (members, gate, cursed/dunked state) |
| `DomeRng`, `DomeLog` | Seeded RNG and append-only JSONL vote log |
| `DomeController` | `McmVoteProvider`, tick task, exactly-once delivery |
| `DomeSession` | One vote: pods, eggs, HUD, phase actions, cleanup |
| `EggService`, `ChickenService`, `PowerUpService` | Bukkit side of the model |
| `PowerUpRegistry` | Power-up catalogue and items |
| `PlayerStateService` | Inventory/location/effect snapshot and restore |
| `DomeVenue`, `HubDialView`, `FallbackDialView` | Venue port and two implementations |
| `DomeArena` | `mcc_dome` void world and static fallback geometry |
| `VoteEngineTest` | Plain `main` proof of the rules (no Bukkit) |

## 11. Build and test

```powershell
$env:JAVA_HOME='C:\Users\LeonS\Desktop\java'
C:\Users\LeonS\Desktop\java-tools\maven\bin\mvn.cmd -B package

# 83 assertions, no Bukkit:
java -cp target\classes;target\test-classes com.mcm.games.decisiondome.VoteEngineTest
```

The built jar is `target/McmDecisionDome.jar`; the install copies are
`game-plugins/_built/McmDecisionDome.jar` and `plugins/McmDecisionDome.jar`.

## 12. Known limitations / follow-ups

- Power-up count scales with the event index, not team standings: `McmVoteRequest` does not carry
  scores. One item is granted normally, two from Game 4 onward.
- The audience vote is implemented (least-voted wins) but the core does not currently call it.
- Cross-vote blank-slot identity is reconstructed from the event index/remaining count, not persisted;
  the number of blanks and the refill vote are correct, but the exact physical slot of an old winner
  may move after a server restart.
- Real chicken entities are visuals driven by the deterministic logical simulation; a `/kill` or a
  chunk unload cannot change the tally.
- Restart mid-vote abandons the vote: no callback is fabricated and tagged entities are swept on the
  next enable. The core's 90 s fallback/`/mcm skipwait` handles the interval.
