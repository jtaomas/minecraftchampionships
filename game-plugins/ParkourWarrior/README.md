# Parkour Warrior (McmParkourWarrior) — spec v2

Game id: `parkourwarrior` — start with `/mcm play parkourwarrior`. World: `mcc_parkourwarrior` (void, created at plugin enable, old folder deleted on first load).
Design: `SPEC_v2.md` (owner-approved). The old three-lane design is gone.

## How it plays
- Everyone spawns on one start island and runs the same course individually. Players never collide (scoreboard team with collision `NEVER` on every runner's scoreboard, re-applied each tick because the core replaces scoreboards every second) and are shown as translucent ghosts (invisibility + "see friendly invisibles").
- Main line: 5 zones x 3 obstacles. Stepping on an obstacle's completion pad gives a stone medal (once), sets the checkpoint ("Spawn point updated" action bar + chat line "You completed <name> and earned a stone medal!"). The 3rd obstacle of a stage prints "You completed the <Zone> zone!".
- Each hub has a bonus path of 3 harder obstacles: bronze, silver, gold (each once). After the 3rd the player is returned to the main line automatically. The **Teleportation Scroll** (paper, slot 8) returns from a bonus path to the main-line checkpoint, medals kept; it is taken away with a chat notice at 30 s left.
- After stage 5: fork pavilion with Easy / Medium / Hard gates (2 obstacles each). The **Return to the Fork** compass (slot 9) lets a player switch ending until 30 s left (walking back to another gate also works until then). Finishing broadcasts "<player> finished the entire course!", adds +0.15 / +0.35 / +0.80 to the team multiplier and turns the player into a spectator (clickable overview links, `/pwview <n>`).
- 10:00 then 30 s overtime. Normal time: a fall teleports you to your last checkpoint (red chat line), nothing lost. Overtime: a fall ends your run (spectator).
- Game ends after overtime, or when no online runner is still active. `finish` is called once, after a short results screen.
- No damage, hunger, drops, item moves, block edits, door/trapdoor/button use in the world. Daylight goes morning -> sunset.
- Rejoining players resume at their checkpoint with their medals; new late joiners spectate.

## Scoring (points scale = research ÷ 5)
- Stone medal 1 each. Metal medals by count held (1..5): bronze 1/3/5/8/12, silver 2/5/9/14/20, gold 3/6/11/18/28 (totals; each new medal adds the difference). Perfect run 75. Medal points are awarded live with `ctx.addPoints`.
- At the end, per side: extra = sum(members' medal points) x (multiplier - 1), split by ending weight 0.40 / 0.45 / 0.65 / 1.10 (none / Easy / Medium / Hard), largest-remainder rounding, awarded per player. Quitters still count. Solo players are their own side.

## HUD
- Own per-player boss bar "TIME LEFT m:ss" (red "OVERTIME" in overtime) + medal row; the core boss bar also gets `setTimer`.
- Sidebar (core, same for everyone): MEDALS OBTAINED top 3, top team multiplier, finished count.
- Personal info the shared sidebar cannot show (main challenges left n/15, your team multiplier) is in the idle action bar; your medals are in the boss bar and hotbar counters (slots 1-4).

## Map contract
`CourseModel` (pure Java) is the contract with the map package; `map.CourseMap.generate(long seed)` is found by reflection (`map.generator: auto|coursemap|stub`); `stub.StubCourse` is a plain test course. The runtime only toggles `timed` blocks (particles + optional ghost block during `warnTicks` before they appear). Missing ids/hubs/finishes are logged at build.

## Admin
- `/pw list`, `/pw tp <id|start|fork|hub N|view N>`, `/pw complete <id> [player]`, `/pw time <seconds-left>` (negative = into overtime), `/pw info`, `/pw rebuild [seed]` (op). Mid-game, `/pw tp <id>` sets the runner's checkpoint to that obstacle's start (no medals); `/pw complete` awards it as if the pad was touched.
- Testing hooks (BRIEF "Admin testing hooks"): `skipWait()` ends the countdown or the results screen; `skipRound()` returns false (single course); fast-test mode (`McmApi.isFastTest()`, read by reflection) cuts the countdown, title card, bonus auto-return delay and end celebration to <= 1 s, not the 10:00 timer. Methods are declared without `@Override` for older cores.

## Defaults, not researched (config.yml)
Countdown 10 s; scroll/return destination = main-line checkpoint (= the hub of that bonus path in normal play); auto-return delay 30 ticks; finished / knocked-out players -> spectator with overview teleports; results screen 6 s; start leash 4 blocks; daylight 0 -> 12500; translucent ghosts on, full hiding of nearby players off (`players.hide-within`, it also hides tab entries); global fall line 8 below the lowest block; spectator bounds margin 40; obstacle lengths/jumps, moving-block timing and kill heights come from the map (kill-Y default lowest foothold - 6). Researched but configurable: 10:00 / 30 s, 30 s cut-off, scoring tables, multipliers, weights.

## Acceptance checklist mapping (SPEC_v2)
1 one course: map + start island, Progress graph has no lanes. 2 stone medal + checkpoint + feedback: `complete/award`. 3 bonus bronze/silver/gold once: `award`, `Run.completed`. 4 falls -> checkpoint within 1 tick, never ahead (only candidate obstacles of the current checkpoint count): `checkPlayer/fell/onFall`. 5 scroll + removal at 30 s: `useScroll/removeScrolls`. 6 three gates, switch until 30 s: fork item + `padCandidates`. 7 multiplier once per player, broadcast, spectator: `finishRun`. 8 overtime knock-out: `onFall/knockOut`. 9 end + `finish` once: `endGame/doFinish`. 10 scoring: `Scoring` (offline-checked: perfect 75, shares sum). 11 HUD: `updateHud`, `giveItems`. 12 rules: `GameListener`, ghost team. 13 timed blocks + indicator: `CourseWorld.updateTimed` (count is the map's job, warned if < 3). 14/15 difficulty + validators: map agent. 16 daylight: `secondTick`. 17 stop/quit/rejoin: `stop/cleanup`, `onQuit`, `onLateJoin`. 18 timing: owner playtest.

## Known limitations / untested
- Not run on a server yet; only compiled and the pure logic (graph walk, scoring) checked offline against the stub course.
- Sidebar is shared, so the "+ you" line lives in the action bar / boss bar.
- Pads are detected by feet position each tick; very fast movement through a 1-block-thick pad region is not an issue at normal speeds but the map should make pad boxes at least 2 high.
