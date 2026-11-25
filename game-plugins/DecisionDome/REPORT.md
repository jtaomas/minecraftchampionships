# DecisionDome rebuild — build report

**Date:** 2026-11-24  
**Scope:** rebuilt `game-plugins/DecisionDome/` against `MinecraftMondayCore:3.2.0`; no core, world,
AceRace, `plugins/` other than the dome jar, review-server or scratchpad changes.

## Build

- `mvn -B package` with Java 21: **success**
- Jar: `target/McmDecisionDome.jar`
- Installed copies: `game-plugins/_built/McmDecisionDome.jar`, `plugins/McmDecisionDome.jar`
- Removed the old `DomeVote`, `HubStage` and `VoteStage` classes; no pad-occupancy or weighted-random
  code remains in the vote path.

## Engine tests

`VoteEngineTest` is a plain `main`, no JUnit and no Bukkit:

```
java -cp target\classes;target\test-classes com.mcm.games.decisiondome.VoteEngineTest
```

Result: **83 assertions passed, 0 failed**. Covered:

- 8- and 6-wedge geometry, boundaries, hub/outside exclusion;
- plurality tally with signed values; frogs −1, mega chicken +3, mega frog −3;
- negative totals legal, least-negative can win when every wedge is negative, zero beats −1;
- tie detection restricted to leaders; 30 seeded tie runs only ever picked a leader and both leaders
  appeared;
- zero-vote fallback among all live wedges;
- audience inversion (least-voted wins);
- removal blanks the winning slot and removes its chickens; refill fills the slot from the pool;
- 15-game ceiling, 8-game dial with 2-game pool;
- removed-wedge votes are ignored;
- Confirmation Dome never enters `VOTING` and returns the only game;
- one egg -> one chicken, in-flight egg resolves during settle, no new throws after close;
- deterministic seed produces identical scores, winner and chicken positions;
- full phase clock (5/30/13/1.5/1/5/2/2/8 s; 1350 ticks) and every phase <=1 s in fast-test;
- headless simulator: 100 votes in ~50–110 ms, with ties and refills observed.

## Phases and presentation

- One boss bar: `VOTING BEGINS IN` -> `VOTING CLOSES IN` -> `GAME CHOSEN IN` -> `TELEPORTING IN`.
- No repeating titles or action-bar countdown. One reveal title only.
- Chat narration: `Counting chickens...`, `The chosen game is...`, then the game name. Tie line
  `Vote tied!` is printed when applicable.
- Vote counts stay hidden until the reveal.
- Fast-test clamps each phase to <=20 ticks; `skipWait` forces a tally and hands the result over.

## Power-ups

MVP implemented: Glacia, Mitosis, Waxing/Waning, Swapping, Sticking, Coin of Fate, Spellcheck,
Swinging Watch, Frog/Mega eggs. Phase 2 is implemented behind `powerups.phase-2`: Levitas, Infernus,
Petrification, Transmutation, Cage, Freezing, Dunking. Per-team items, Game 1/audience exclusion,
chat announcement with reason, and dunk stripping are wired. Pod state uses
`McmLobby.Venue.setPodState` / `setPodGate` when the v2 venue provides them.

## Venue and fallback

- `HubDialView` uses the v2 `McmLobby.Venue` methods and defaults when v2 is not fully implemented.
- `FallbackDialView` + `DomeArena` keep `mcc_dome` working: generated shell, 8/6 wedges, 10 simple
  pods, rising dividers, labels, reveal, and safe cleanup.
- Venue selection is `apiVersion() >= 2 ? hub : fallback`; a v1 hub can still be used by
  `/dome test hub` for a compatibility check.
- Session tags isolate cleanup (`mcm_dome:<session>:chicken|egg|ui`); stale sweeps run on enable.

## Open issues

1. **Team standing is not in `McmVoteRequest`.** Power-up count uses the event index (1 normally,
   2 from Game 4) instead of placement. This is the only approximation in the power-up grant.
2. **Cross-vote slot identity is reconstructed, not persisted.** The dial keeps 8/6 wedges and the
   correct number of blanks per vote, and refills at the configured vote, but after a restart the
   physical blank slot may shift. The chosen game itself never reappears.
3. **Audience vote** is implemented as least-voted-wins but the core does not currently send an
   audience request; there is no MCC Live integration.
4. **Power-up targeting UI** is right-click-to-random-target (spell/chicken/wedge, curse/random enemy
   pod). There is no inventory menu or custom HUD.
5. **Real chicken entities are visuals.** The deterministic logical chicken is authoritative, so a
   `/kill` or chunk unload cannot change the vote. Movement is synced at ~3 Hz, not every tick.
6. **`mcc_dome` pods are simple platforms**, not full rooms; the full pod geometry is the core's v2
   `HubVenue` build. Curses still read because effects target the pod index/model.
7. **Restart mid-vote** abandons the callback (the core's 90 s fallback handles the interval). Tagged
   leftovers are swept on enable; there is no session resume.
8. **Pose/scale attributes** rely on `GENERIC_SCALE`; extremely old Paper builds may ignore it.

## Core integration notes

- No core changes were made. The plugin overrides both `runVote(McmVoteRequest, Consumer)` and the
  v1 `runVote(List, Consumer)` fallback.
- `eventIndex` is treated as 1-based (the core currently sends `playlistIndex + 1`).
- `allowPowerUps`, `audienceVote`, `dialCapacity` and `teams` are consumed from the request.
- `skipWait`, `cancelVote`, `/dome end` and `/dome cancel` are wired to the core's contracts.
