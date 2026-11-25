# Decision Dome — Nocturne Orrery specification (for build)

**Owner decisions:** faithful MCC egg/chicken vote; **Nocturne Orrery** theme; extend the core API; core owns the permanent hub geometry.
**Research:** `game-plugins/research/dome/` (7 dossiers, MCC-only rules; 0 non-MCC rules).
**Status:** API v2 is in place; geometry and runtime are the next build.

---

## 1. Rules the builder must implement

1. **One egg per player**, thrown from the team pod. The egg hatches into a chicken; the chicken’s **wedge at tally time** is the vote. `[MCC]`
2. **30 s vote**, then a **~13 s settle** under `GAME CHOSEN IN` where items are still live; tally at the end of the settle. `[MCC]`
3. HUD uses three countdown bars in one place: `VOTING CLOSES IN` → `GAME CHOSEN IN` → `TELEPORTING IN`. `[MCC]`
4. **Plurality wins.** Ties are a **uniform random pick among the tied leaders only**. `[MCC]`
5. **Signed votes:** frog −1, mega chicken +3, mega frog −3; negative totals are legal and can win. `[MCC]`
6. The chosen game is removed; its wedge stays blank; the dial tops up mid-event (normally before vote 6). 8-game default, 6-game variant, 15-game ceiling. One game left = **Confirmation Dome** (no vote). `[MCC]`
7. **Power-ups** are per-team, scale with standing, do not carry between rounds, and are excluded from Game 1 and the audience vote. A dunked team loses its remaining power-ups. `[MCC]`
8. The audience vote is **inverted** (vote for the game you do not want; least-voted plays), distinct from the +6 bonus-vote variant. `[MCC]`
9. Pods are rooms, not pads: team colour, 4 seats, rear gate, low throw window. Curses/dunks only read if the target is a room. `[MCC]`
10. No live vote counts during voting; counts appear only after `Counting chickens...`. `[MCC]`

## 2. Phase clock

| Phase | Normal | Fast-test | Entry/exit |
|---|---:|---:|---|
| OPENING | 5 s | 1 s | Teleport to pods, hand out eggs, dial labels, boss bar |
| VOTING | 30 s | 1 s | Throws enabled; no live counts |
| SETTLE | 13 s | 1 s | Throws off; in-flight eggs resolve; items still live |
| DIVIDERS | 1.5 s | 1 tick | Dividers rise; chickens contained |
| TALLY | 1 s | 1 tick | Freeze, count, tie roll, log |
| REVEAL | 5 s | 1 s | Winning wedge, title, fireworks |
| REMOVE | 2 s | 1 tick | Winner wedge blanked, its chickens removed |
| REFILL | 2 s | 1 s | Top up from the remaining pool |
| TELEPORT | 8 s | 1 s | `TELEPORTING IN`, hand result to core |

Total ≈ 67.5 s, inside the core’s 90 s fallback. No power-up may extend the vote past 75 s.

## 3. Core API (already added)

- `com.mcm.api.McmVoteRequest` — game ids, team map, event index, power-up flag, audience flag, dial capacity.
- `McmVoteProvider.runVote(McmVoteRequest, Consumer<String>)` — default forwards to the id-only method.
- `McmLobby.Venue` v2 defaults: `apiVersion`, `dialCapacity`, `setDial`, `wedgeCenter`, `wedgeRadius`, `setWedge`, `podCount`, `podSpawn`, `setPodGate`, `setPodState`, `setDividers`, `clearRuntime`, `revealWedge`, `dialGeneration`, `revealSurface`, `interiorClock`.
- `MinecraftMondayPlugin.continueChampionship` now builds a `McmVoteRequest` with teams and event index and calls the full overload.

The geometry agent implements the v2 methods in `HubVenue`. The plugin must work with a v1 venue by falling back to its existing `DomeArena`.

## 4. Runtime architecture

Replace `DomeVote` with:

- `VoteEngine` — pure Java, no Bukkit: phases, egg/chicken values, tally, tie roll, removal, refill, deterministic seed, `VoteTally` output.
- `DomeSession` — one `runVote`: seed, request, dial, pods, eggs, chickens, power-ups, phase clock, callback.
- `Dial`, `Wedge`, `TeamPod`, `Egg`, `Chicken`, `VoteTally`, `PowerUp`, `Phase`.
- `DomeController` — thin `McmVoteProvider`; owns the tick task and exactly-once callback.
- `EggService` — egg item/PDC, throw, landing wedge.
- `ChickenService` — real chickens, PDC owner/team/weight, no AI/collision, logical position is the tally source, session-tagged cleanup.
- `PowerUpService` — MVP: Glacia, Mitosis, Waxing/Waning, Swapping, Sticking, Coin of Fate, Spellcheck, Swinging Watch, Frog/Mega eggs; phase 2: Levitas, Infernus, Petrify, Transmutation, Cage, Freezing, Dunking.
- `DomeVenue` interface — implemented by `HubDialView` (v2) and `FallbackDialView` (v1/`mcc_dome`).
- `DomeLog` — JSONL vote log; `DomeRng` — seeded, logged.
- `VoteEngineTest` — a plain `main` proving tally, tie, negative votes, removal/refill, and Confirmation Dome without Bukkit.

Ownership rules:
- The plugin owns the **result**; the core consumes it.
- The core/venue owns static geometry; the plugin never hardcodes a dome coordinate.
- The venue owns runtime block mutations and restores them.
- `DomeSession` owns entities; session tags prevent cross-vote cleanup.

## 5. Power-ups

MVP per-team items, announced with the reason (standing/round). No custom HUD: item + chat + world effects. Curse targets are team pods. A dunked team loses remaining power-ups. Exclude Game 1 and audience vote.

## 6. Admin/testing

`/dome test [hub|dome]`, `/dome end`, `/dome skipwait`, `/dome cancel`, `/dome tp`, `/dome simulate <n>`, `/dome status`. Fast-test cuts every phase to ≤1 s. A seeded simulate must prove tally/tie/removal/refill.

## 7. Acceptance checklist

- [ ] 10 pods, 4 players each, team colours, gates, throw windows.
- [ ] 8- and 6-wedge dials from the same room; wedge 1 on the entrance axis.
- [ ] 30 s vote, 13 s settle, three countdown bars.
- [ ] One egg per player; one chicken per egg; chicken wedge at tally.
- [ ] Plurality, tie among leaders only, negative votes legal.
- [ ] Chosen game removed; blank wedge; refill; Confirmation Dome.
- [ ] Power-ups per team; Game 1/audience excluded; dunk strips.
- [ ] Divider rise contains chickens; tally hidden until the reveal.
- [ ] Reveal cube/orrery + winning wedge; fireworks; clean cleanup.
- [ ] Exactly-once callback across cancel/disable/error.
- [ ] v1 fallback still works in `mcc_dome`.
- [ ] Fast-test and skipWait.
- [ ] No stale entities or blocks after a vote.
