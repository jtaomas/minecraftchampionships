# MCM staging bot test - run 2026-11-23T10-38-45

Raw logs: `C:\Users\LeonS\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\bot-harness\logs\2026-11-23T10-38-45`
Bots: 6, pass1 watch 90s then /mcm skip; pass2 natural end (timeout 12 min).

- Jars copied: McmAceRace.jar, McmBattleBox.jar, McmBingo.jar, McmBuildMart.jar, McmDecisionDome.jar, McmDodgebolt.jar, McmFootRace.jar, McmGridRunners.jar, McmHoleInTheWall.jar, McmMeltdown.jar, McmParkourTag.jar, McmParkourWarrior.jar, McmRailroadRush.jar, McmRocketSpleef.jar, McmSandsOfTime.jar, McmSkyBattle.jar, McmTgttos.jar, MinecraftMondayCore.jar (core)

## Pass 1 (all games: play, watch, skip)

| id | name | result | started | ended | duration | bots in arena @10s / @60s | back in lobby | issues | bot kicks/errors |
|---|---|---|---|---|---|---|---|---|---|
| sg | Survival Games (Blitz SG) | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | - | 0 |
| parkour | Parkour Spiral | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | WARN bot-movement(benign) x6 | 0 |
| dropper | Multi-Stage Dropper | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | WARN bot-movement(benign) x64 | 0 |
| tntrun | TNT Run (Round 1/3) | **PASS** | yes | Last standing! (natural) | 27s | 6/0 | true | - | 0 |
| spleef | Spleef / Splegg (Round 1/3) | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | - | 0 |
| kitpvp | Battle Arena (KitPvP) (Round 1/2) | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | - | 0 |
| survival | Speed Survival (15m) | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | - | 0 |
| oneshot | One Shot (OITC) | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | WARN bot-movement(benign) x5 | 0 |
| tgttos | To Get To The Other Side | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | - | 0 |
| skybattle | Sky Battle | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | WARN bot-movement(benign) x25 | 0 |
| sandsoftime | Sands of Time | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | WARN server x1 | 0 |
| rocketspleef | Rocket Spleef Rush | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | - | 0 |
| railroadrush | Railroad Rush | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | WARN server x1 | 0 |
| parkourwarrior | Parkour Warrior | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | WARN bot-movement(benign) x2 | 0 |
| parkourtag | Parkour Tag | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | - | 0 |
| meltdown | Meltdown | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | - | 0 |
| hitw | Hole in the Wall | **PASS** | yes | Last one standing (natural) | 24s | 6/0 | true | - | 0 |
| gridrunners | Grid Runners | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | WARN server x1 | 0 |
| footrace | Foot Race | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | - | 0 |
| dodgebolt | Dodgebolt | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | - | 0 |
| buildmart | Build Mart | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | WARN bot-movement(benign) x1 | 0 |
| bingo | Bingo but Fast | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | - | 0 |
| lockout | Lockout Bingo | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | WARN bot-movement(benign) x17; WARN server x1 | 0 |
| battlebox | Battle Box | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | - | 0 |
| acerace | Ace Race | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | - | 0 |

## Pass 2 (MCC games to natural end)

| id | name | result | end reason | ended by | duration | bots in arena @10s/@60s | back in lobby | issues |
|---|---|---|---|---|---|---|---|---|
| tgttos | To Get To The Other Side | **PASS** | All 6 rounds complete | natural | 606s | 6/6 | true | - |
| skybattle | Sky Battle | **PASS** | Last side standing! | natural | 114s | 6/6 | true | WARN bot-movement(benign) x35 |
| sandsoftime | Sands of Time | **PASS** | Every team is out of the dungeon. | natural | 150s | 6/6 | true | - |
| rocketspleef | Rocket Spleef Rush | **PASS** | Rocket Spleef Rush complete | natural | 448s | 6/6 | true | - |
| railroadrush | Railroad Rush | **PASS** | Railroad Rush complete | natural | 489s | 6/6 | true | - |
| parkourwarrior | Parkour Warrior | **PASS** | Time's up! | natural | 433s | 6/6 | true | WARN bot-movement(benign) x9 |
| parkourtag | Parkour Tag | **PASS** | All rounds played | natural | 292s | 6/6 | true | - |
| meltdown | Meltdown | **PASS** | Last side standing | natural | 265s | 6/6 | true | - |
| hitw | Hole in the Wall | **PASS** | Last one standing | natural | 39s | 6/0 | true | - |
| gridrunners | Grid Runners | **PASS** | Time's up | natural | 610s | 6/6 | true | WARN bot-movement(benign) x2 |
| footrace | Foot Race | **PASS** | Time limit reached | natural | 310s | 6/6 | true | WARN bot-movement(benign) x4 |
| dodgebolt | Dodgebolt | **PASS** | Bot4 won Dodgebolt 3-0 | natural | 248s | 6/6 | true | - |
| buildmart | Build Mart | **PASS** | Time's up | natural | 490s | 6/6 | true | WARN bot-movement(benign) x2 |
| bingo | Bingo but Fast | **PASS** | Time's up | natural | 608s | 6/6 | true | - |
| lockout | Lockout Bingo | **PASS** | Skipped by Admin | timeout-skip | 720s | 6/6 | true | - |
| battlebox | Battle Box | **PASS** | Battle Box complete | natural | 570s | 6/6 | true | WARN bot-movement(benign) x1 |
| acerace | Ace Race | **PASS** | Time's up! | natural | 367s | 6/6 | true | WARN bot-movement(benign) x3 |

## Championship / Decision Dome

- Result: **PASS**; first game: Survival Games (Blitz SG); dome triggered: true; chosen: [Decision Dome] Battle Arena (KitPvP) was chosen!; next game started: Battle Arena (KitPvP) (Round 1/2)
- skipped first game after 45s to reach the between-games vote

```
[MCM] Head to the vote! The next event is chosen by the players.
[Decision Dome] Stand on the game you want to play next! (20s)
[Dome] 10 seconds left to vote!
[Dome] Votes: Parkour Spiral 0, Multi-Stage Dropper 0, TNT Run 0, Spleef / Splegg 0, Battle Arena (KitPvP) 1, Speed Survival (15m) 0, To Get To The Other Side 0, Sky Battle 1, Sands of Time 0, Rocket Spleef Rush 0, Railroad Rush 1, Parkour Warrior 0
[Decision Dome] Battle Arena (KitPvP) was chosen!
```

## Server lag ("Can't keep up")

- 21:56:12 pass1:sandsoftime: 3100 ms (62 ticks) **>2s**
- 21:59:42 pass1:railroadrush: 2544 ms (50 ticks) **>2s**
- 22:07:23 pass1:gridrunners: 3460 ms (69 ticks) **>2s**
- 22:16:11 pass1:lockout: 2998 ms (59 ticks) **>2s**

## Per-game details

### pass1: sg (Survival Games (Blitz SG)) - PASS

- positions lobby: Bot1 minecraft:overworld (7.8,148.0,9.9) survival; Bot2 minecraft:overworld (5.3,151.0,8.3) survival; Bot3 minecraft:overworld (-0.2,151.0,13.7) survival; Bot4 minecraft:overworld (-4.3,155.0,4.9) survival; Bot5 minecraft:overworld (0.1,156.0,4.0) survival; Bot6 minecraft:overworld (4.3,153.0,8.3) survival
- positions t10: Bot1 minecraft:sg_world (8.9,93.2,2.7) survival; Bot2 minecraft:sg_world (6.3,93.9,18.7) survival; Bot3 minecraft:sg_world (-4.9,94.0,19.6) survival; Bot4 minecraft:sg_world (-19.5,19.0,10.1) survival; Bot5 minecraft:sg_world (-6.0,94.0,-5.8) survival; Bot6 minecraft:sg_world (4.5,93.0,-2.3) survival
- positions t60: Bot1 minecraft:sg_world (1.3,93.0,2.3) survival; Bot2 minecraft:sg_world (8.2,94.0,42.0) survival; Bot3 minecraft:sg_world (22.6,95.3,35.9) survival; Bot4 minecraft:sg_world (-24.7,21.0,5.3) survival; Bot5 minecraft:sg_world (-24.3,91.0,-53.7) survival; Bot6 minecraft:sg_world (3.3,93.0,-2.1) survival
- positions after: Bot1 minecraft:overworld (-2.2,156.0,2.3) survival; Bot2 minecraft:overworld (7.3,154.0,4.3) survival; Bot3 minecraft:overworld (-3.4,149.2,20.2) survival; Bot4 minecraft:overworld (-5.3,148.3,10.7) survival; Bot5 minecraft:overworld (-2.7,147.0,17.3) survival; Bot6 minecraft:overworld (-23.3,143.0,20.8) survival

### pass1: parkour (Parkour Spiral) - PASS

- positions lobby: Bot1 minecraft:overworld (-2.2,156.0,2.3) survival; Bot2 minecraft:overworld (7.3,154.0,4.7) survival; Bot3 minecraft:overworld (-3.7,143.0,20.5) survival; Bot4 minecraft:overworld (-5.3,147.0,10.7) survival; Bot5 minecraft:overworld (-2.7,147.5,17.3) survival; Bot6 minecraft:overworld (-30.7,142.1,24.7) survival
- positions t10: Bot1 minecraft:parkour_world (-2.2,51.0,-53.0) survival; Bot2 minecraft:parkour_world (2.7,52.0,-42.3) survival; Bot3 minecraft:parkour_world (5.7,54.2,-45.7) survival; Bot4 minecraft:parkour_world (-3.7,53.0,-44.3) survival; Bot5 minecraft:parkour_world (4.7,52.0,-43.3) survival; Bot6 minecraft:parkour_world (-10.1,51.5,-57.0) survival
- positions t60: Bot1 minecraft:parkour_world (-6.4,51.0,-50.3) survival; Bot2 minecraft:parkour_world (-25.8,51.0,-91.3) survival; Bot3 minecraft:parkour_world (2.7,52.0,-42.3) survival; Bot4 minecraft:parkour_world (-23.2,51.0,-91.7) survival; Bot5 minecraft:parkour_world (45.5,51.1,-65.4) survival; Bot6 minecraft:parkour_world (-22.1,51.2,-90.0) survival
- positions after: Bot1 minecraft:overworld (4.7,158.0,0.9) survival; Bot2 minecraft:overworld (-17.7,141.0,14.7) survival; Bot3 minecraft:overworld (-5.7,157.0,-3.7) survival; Bot4 minecraft:overworld (7.1,154.0,5.7) survival; Bot5 minecraft:overworld (-0.8,149.5,15.3) survival; Bot6 minecraft:overworld (4.0,150.0,15.1) survival

**WARN** [bot-movement(benign)] x6: Bot3 moved too quickly! -10.988982537178822,1.0,9.04277643481415

```
Bot3 moved too quickly! -10.988982537178822,1.0,9.04277643481415

```

### pass1: dropper (Multi-Stage Dropper) - PASS

- positions lobby: Bot1 minecraft:overworld (4.7,158.0,0.9) survival; Bot2 minecraft:overworld (-17.7,141.0,14.7) survival; Bot3 minecraft:overworld (-5.7,157.0,-3.7) survival; Bot4 minecraft:overworld (8.2,154.0,4.7) survival; Bot5 minecraft:overworld (-7.3,147.0,12.7) survival; Bot6 minecraft:overworld (5.2,148.0,14.3) survival
- positions t10: Bot1 minecraft:dropper_world (19.3,201.0,5309.5) survival; Bot2 minecraft:dropper_world (21.1,202.2,5308.7) survival; Bot3 minecraft:dropper_world (19.1,202.3,5310.1) survival; Bot4 minecraft:dropper_world (20.8,201.0,5310.5) survival; Bot5 minecraft:dropper_world (19.5,201.0,5309.5) survival; Bot6 minecraft:dropper_world (19.3,201.0,5309.2) survival
- positions t60: Bot1 minecraft:dropper_world (20.0,201.0,5308.9) survival; Bot2 minecraft:dropper_world (19.5,201.0,5309.5) survival; Bot3 minecraft:dropper_world (19.5,201.0,5309.5) survival; Bot4 minecraft:dropper_world (19.5,202.0,5309.3) survival; Bot5 minecraft:dropper_world (19.5,183.5,5311.7) survival; Bot6 minecraft:dropper_world (19.7,201.0,5310.3) survival
- positions after: Bot1 minecraft:overworld (3.7,157.0,-1.7) survival; Bot2 minecraft:overworld (-4.3,149.0,10.4) survival; Bot3 minecraft:overworld (-25.1,142.0,34.1) survival; Bot4 minecraft:overworld (0.5,157.0,0.5) survival; Bot5 minecraft:overworld (-3.7,157.0,-3.7) survival; Bot6 minecraft:overworld (-3.9,145.0,4.3) survival

**WARN** [bot-movement(benign)] x64: Bot1 moved too quickly! -2.6368578994398284,53.4432502499383,0.0

```
Bot1 moved too quickly! -2.6368578994398284,53.4432502499383,0.0

```

### pass1: tntrun (TNT Run (Round 1/3)) - PASS

- positions lobby: Bot1 minecraft:overworld (3.7,157.0,-1.7) survival; Bot2 minecraft:overworld (-6.2,149.0,12.3) survival; Bot3 minecraft:overworld (-28.9,141.0,32.3) survival; Bot4 minecraft:overworld (-4.5,157.0,-0.4) survival; Bot5 minecraft:overworld (-3.7,157.0,-3.7) survival; Bot6 minecraft:overworld (-14.6,141.0,4.8) survival
- positions t10: Bot1 minecraft:tntrun_world (102.8,1.0,82.3) survival; Bot2 minecraft:tntrun_world (104.3,0.3,117.3) spectator; Bot3 minecraft:tntrun_world (100.7,-1.0,118.3) spectator; Bot4 minecraft:tntrun_world (87.2,1.0,103.4) survival; Bot5 minecraft:tntrun_world (94.3,0.3,102.3) spectator; Bot6 minecraft:tntrun_world (100.7,0.2,100.7) spectator
- positions after: Bot1 minecraft:overworld (4.2,158.0,0.7) survival; Bot2 minecraft:overworld (5.3,151.0,8.7) survival; Bot3 minecraft:overworld (-1.7,155.0,6.8) survival; Bot4 minecraft:overworld (8.8,153.0,4.7) survival; Bot5 minecraft:overworld (5.3,151.0,8.7) survival; Bot6 minecraft:overworld (3.7,157.0,2.5) survival

### pass1: spleef (Spleef / Splegg (Round 1/3)) - PASS

- positions lobby: Bot1 minecraft:overworld (4.7,157.0,0.7) survival; Bot2 minecraft:overworld (5.3,151.0,8.7) survival; Bot3 minecraft:overworld (3.7,154.5,7.1) survival; Bot4 minecraft:overworld (8.8,153.0,4.7) survival; Bot5 minecraft:overworld (5.3,151.0,8.7) survival; Bot6 minecraft:overworld (6.0,158.3,4.3) survival
- positions t10: Bot1 minecraft:spleef_world (-14.3,89.0,22.1) survival; Bot2 minecraft:spleef_world (-5.3,90.0,24.0) survival; Bot3 minecraft:spleef_world (-22.8,89.0,15.1) survival; Bot4 minecraft:spleef_world (-13.5,89.0,22.5) survival; Bot5 minecraft:spleef_world (-7.3,89.0,21.6) survival; Bot6 minecraft:spleef_world (-12.8,81.0,15.6) survival
- positions t60: Bot1 minecraft:spleef_world (-7.2,89.0,23.5) survival; Bot2 minecraft:spleef_world (22.3,54.7,27.6) spectator; Bot3 minecraft:spleef_world (-18.1,89.0,18.7) survival; Bot4 minecraft:spleef_world (-6.0,81.0,3.9) survival; Bot5 minecraft:spleef_world (10.7,55.0,64.7) spectator; Bot6 minecraft:spleef_world (-12.6,73.0,11.5) survival
- positions after: Bot1 minecraft:overworld (2.3,150.0,13.7) survival; Bot2 minecraft:overworld (-2.2,142.5,26.1) survival; Bot3 minecraft:overworld (-2.0,156.0,2.3) survival; Bot4 minecraft:overworld (4.3,144.0,15.3) survival; Bot5 minecraft:overworld (-2.4,157.5,1.8) survival; Bot6 minecraft:overworld (-1.6,150.0,20.3) survival

### pass1: kitpvp (Battle Arena (KitPvP) (Round 1/2)) - PASS

- positions lobby: Bot1 minecraft:overworld (2.3,150.0,13.7) survival; Bot2 minecraft:overworld (5.6,140.5,31.7) survival; Bot3 minecraft:overworld (-2.0,156.0,2.2) survival; Bot4 minecraft:overworld (4.3,144.0,15.3) survival; Bot5 minecraft:overworld (-2.1,156.0,2.3) survival; Bot6 minecraft:overworld (-1.6,149.1,20.3) survival
- positions t10: Bot1 minecraft:pvp_world (4.1,32.0,47.3) survival; Bot2 minecraft:pvp_world (1.7,32.5,-36.8) survival; Bot3 minecraft:pvp_world (-5.7,32.1,43.4) survival; Bot4 minecraft:pvp_world (-4.5,33.0,-42.1) survival; Bot5 minecraft:pvp_world (-19.7,32.0,46.2) survival; Bot6 minecraft:pvp_world (-0.4,32.2,-45.3) survival
- positions t60: Bot1 minecraft:pvp_world (20.7,32.0,45.3) survival; Bot2 minecraft:pvp_world (-5.7,32.0,-52.7) survival; Bot3 minecraft:pvp_world (3.7,32.0,35.1) survival; Bot4 minecraft:pvp_world (4.9,33.2,9.3) survival; Bot5 minecraft:pvp_world (17.7,32.0,6.3) survival; Bot6 minecraft:pvp_world (-11.9,32.0,-61.7) survival
- positions after: Bot1 minecraft:overworld (2.3,154.3,7.0) survival; Bot2 minecraft:overworld (-3.3,146.9,19.8) survival; Bot3 minecraft:overworld (-17.1,143.2,16.6) survival; Bot4 minecraft:overworld (5.4,144.0,16.1) survival; Bot5 minecraft:overworld (-23.3,140.1,8.2) survival; Bot6 minecraft:overworld (4.4,148.0,14.3) survival

### pass1: survival (Speed Survival (15m)) - PASS

- positions lobby: Bot1 minecraft:overworld (2.3,154.3,7.0) survival; Bot2 minecraft:overworld (-3.2,143.8,20.9) survival; Bot3 minecraft:overworld (-18.7,142.0,13.9) survival; Bot4 minecraft:overworld (10.6,140.4,12.3) survival; Bot5 minecraft:overworld (-26.6,140.0,2.3) survival; Bot6 minecraft:overworld (4.4,148.0,14.3) survival
- positions t10: Bot1 minecraft:survival_world (35.0,82.0,7.6) survival; Bot2 minecraft:survival_world (5.0,65.2,100.7) survival; Bot3 minecraft:survival_world (-122.7,98.0,105.0) survival; Bot4 minecraft:survival_world (-160.3,72.0,-1.3) survival; Bot5 minecraft:survival_world (-133.4,73.0,-97.0) survival; Bot6 minecraft:survival_world (-3.3,64.0,-67.3) survival
- positions t60: Bot1 minecraft:survival_world (-12.9,71.0,4.6) survival; Bot2 minecraft:survival_world (-1.3,61.0,102.4) survival; Bot3 minecraft:survival_world (-116.8,103.0,89.3) survival; Bot4 minecraft:survival_world (-141.0,73.2,-19.6) survival; Bot5 minecraft:survival_world (-141.7,62.0,-134.3) survival; Bot6 minecraft:survival_world (-3.3,64.0,-67.3) survival
- positions after: Bot1 minecraft:overworld (-2.3,156.0,2.8) survival; Bot2 minecraft:overworld (-0.6,148.1,17.8) survival; Bot3 minecraft:overworld (-1.6,154.0,4.8) survival; Bot4 minecraft:overworld (3.1,151.0,12.3) survival; Bot5 minecraft:overworld (-11.7,141.0,11.7) survival; Bot6 minecraft:overworld (1.2,157.0,0.0) survival

### pass1: oneshot (One Shot (OITC)) - PASS

- positions lobby: Bot1 minecraft:overworld (-2.3,156.0,2.8) survival; Bot2 minecraft:overworld (3.6,142.4,17.3) survival; Bot3 minecraft:overworld (-1.6,154.0,4.8) survival; Bot4 minecraft:overworld (2.3,151.0,12.3) survival; Bot5 minecraft:overworld (-15.9,142.8,14.7) survival; Bot6 minecraft:overworld (1.2,158.3,0.0) survival
- positions t10: Bot1 minecraft:oneshot_world (53.7,42.0,100.3) survival; Bot2 minecraft:oneshot_world (49.0,40.0,95.3) survival; Bot3 minecraft:oneshot_world (52.6,38.0,101.3) adventure; Bot4 minecraft:oneshot_world (48.7,39.0,97.0) survival; Bot5 minecraft:oneshot_world (48.7,36.0,93.3) survival; Bot6 minecraft:oneshot_world (48.0,38.0,98.0) survival
- positions t60: Bot1 minecraft:oneshot_world (48.4,37.0,99.7) survival; Bot2 minecraft:oneshot_world (50.7,40.0,95.7) survival; Bot3 minecraft:oneshot_world (52.3,38.0,101.6) adventure; Bot4 minecraft:oneshot_world (48.3,38.0,98.2) adventure; Bot5 minecraft:oneshot_world (48.7,36.0,93.3) survival; Bot6 minecraft:oneshot_world (50.7,40.0,95.3) survival
- positions after: Bot1 minecraft:overworld (0.0,152.8,11.9) survival; Bot2 minecraft:overworld (-2.4,155.0,4.7) survival; Bot3 minecraft:overworld (-8.9,157.0,2.0) survival; Bot4 minecraft:overworld (-2.3,148.0,6.2) survival; Bot5 minecraft:overworld (6.6,151.0,8.9) survival; Bot6 minecraft:overworld (10.1,141.0,6.6) survival

**WARN** [bot-movement(benign)] x5: Bot3 moved too quickly! -3.5,-18.0,78.5

```
Bot3 moved too quickly! -3.5,-18.0,78.5

```

### pass1: tgttos (To Get To The Other Side) - PASS

- positions lobby: Bot1 minecraft:overworld (-0.3,149.0,15.3) survival; Bot2 minecraft:overworld (-2.4,155.0,4.7) survival; Bot3 minecraft:overworld (-8.1,157.0,1.1) survival; Bot4 minecraft:overworld (-2.3,148.0,6.2) survival; Bot5 minecraft:overworld (6.3,152.0,8.3) survival; Bot6 minecraft:overworld (10.1,141.0,6.6) survival
- positions t10: Bot1 minecraft:mcc_tgttos (8.2,101.0,12.7) survival; Bot2 minecraft:mcc_tgttos (1.5,101.0,-11.0) survival; Bot3 minecraft:mcc_tgttos (7.8,101.0,-1.5) survival; Bot4 minecraft:mcc_tgttos (3.0,101.0,9.5) survival; Bot5 minecraft:mcc_tgttos (3.1,101.0,5.0) survival; Bot6 minecraft:mcc_tgttos (1.7,101.0,5.6) survival
- positions t60: Bot1 minecraft:mcc_tgttos (4.3,101.0,-4.5) survival; Bot2 minecraft:mcc_tgttos (6.1,101.0,-11.7) survival; Bot3 minecraft:mcc_tgttos (5.3,101.0,-9.1) survival; Bot4 minecraft:mcc_tgttos (1.0,101.0,11.8) survival; Bot5 minecraft:mcc_tgttos (7.7,102.2,6.7) survival; Bot6 minecraft:mcc_tgttos (5.1,102.3,1.0) survival
- positions after: Bot1 minecraft:overworld (6.8,156.8,5.1) survival; Bot2 minecraft:overworld (0.7,154.0,6.3) survival; Bot3 minecraft:overworld (-3.5,156.0,4.2) survival; Bot4 minecraft:overworld (4.7,157.0,0.3) survival; Bot5 minecraft:overworld (-3.8,149.0,15.7) survival; Bot6 minecraft:overworld (0.4,155.3,7.8) survival

### pass1: skybattle (Sky Battle) - PASS

- positions lobby: Bot1 minecraft:overworld (5.6,156.0,4.3) survival; Bot2 minecraft:overworld (0.7,154.0,6.3) survival; Bot3 minecraft:overworld (-2.0,156.0,2.3) survival; Bot4 minecraft:overworld (4.7,157.0,0.3) survival; Bot5 minecraft:overworld (-3.8,149.8,15.7) survival; Bot6 minecraft:overworld (-1.1,155.0,8.2) survival
- positions t10: Bot1 minecraft:mcc_skybattle (2.3,101.1,32.1) survival; Bot2 minecraft:mcc_skybattle (28.0,101.0,15.7) survival; Bot3 minecraft:mcc_skybattle (31.5,101.0,-17.5) survival; Bot4 minecraft:mcc_skybattle (-4.3,101.0,-35.3) survival; Bot5 minecraft:mcc_skybattle (-26.0,101.0,19.6) survival; Bot6 minecraft:mcc_skybattle (-30.5,101.0,-17.5) survival
- positions t60: Bot1 minecraft:mcc_skybattle (3.7,101.1,39.9) survival; Bot2 minecraft:mcc_skybattle (-30.3,-1980.0,-4.4) spectator; Bot3 minecraft:mcc_skybattle (-1.4,104.0,8.7) spectator; Bot4 minecraft:mcc_skybattle (3.0,104.0,10.5) spectator; Bot5 minecraft:mcc_skybattle (-27.1,101.5,20.9) survival; Bot6 minecraft:mcc_skybattle (-27.8,101.0,-14.1) survival
- positions after: Bot1 minecraft:overworld (2.5,151.3,14.1) survival; Bot2 minecraft:overworld (-4.2,150.0,11.0) survival; Bot3 minecraft:overworld (-1.7,154.0,6.2) survival; Bot4 minecraft:overworld (-3.7,157.0,-3.7) survival; Bot5 minecraft:overworld (2.7,150.0,15.8) survival; Bot6 minecraft:overworld (3.3,153.0,10.7) survival

**WARN** [bot-movement(benign)] x25: Bot2 moved too quickly! -31.66923168337574,73.68868037874728,-1.3780710854896632

```
Bot2 moved too quickly! -31.66923168337574,73.68868037874728,-1.3780710854896632

```

### pass1: sandsoftime (Sands of Time) - PASS

- positions lobby: Bot1 minecraft:overworld (2.3,150.0,13.8) survival; Bot2 minecraft:overworld (-9.0,146.0,5.4) survival; Bot3 minecraft:overworld (-1.4,154.0,4.3) survival; Bot4 minecraft:overworld (-3.7,157.0,-3.7) survival; Bot5 minecraft:overworld (-1.2,151.0,14.1) survival; Bot6 minecraft:overworld (3.3,153.0,10.7) survival
- positions t10: Bot1 minecraft:mcc_sandsoftime (330.5,65.0,2.5) survival; Bot2 minecraft:mcc_sandsoftime (235.7,65.0,4.3) survival; Bot3 minecraft:mcc_sandsoftime (126.4,65.0,11.7) survival; Bot4 minecraft:mcc_sandsoftime (23.5,65.0,8.8) survival; Bot5 minecraft:mcc_sandsoftime (435.7,65.0,11.7) survival; Bot6 minecraft:mcc_sandsoftime (530.5,65.0,2.5) survival
- positions t60: Bot1 minecraft:mcc_sandsoftime (358.1,65.5,27.3) survival; Bot2 minecraft:mcc_sandsoftime (222.8,65.0,1.3) survival; Bot3 minecraft:mcc_sandsoftime (125.3,66.0,13.3) survival; Bot4 minecraft:mcc_sandsoftime (81.5,-468.1,26.8) spectator; Bot5 minecraft:mcc_sandsoftime (418.0,65.0,23.7) survival; Bot6 minecraft:mcc_sandsoftime (593.4,-1882.7,63.2) spectator
- positions after: Bot1 minecraft:overworld (-1.7,154.0,4.3) survival; Bot2 minecraft:overworld (3.3,142.0,17.9) survival; Bot3 minecraft:overworld (7.1,154.0,5.6) survival; Bot4 minecraft:overworld (17.7,140.0,0.9) survival; Bot5 minecraft:overworld (-3.7,156.0,3.3) survival; Bot6 minecraft:overworld (-1.7,156.0,2.3) survival

**WARN** [server] x1: Can't keep up! Is the server overloaded? Running 3100ms or 62 ticks behind

```
Can't keep up! Is the server overloaded? Running 3100ms or 62 ticks behind

```

### pass1: rocketspleef (Rocket Spleef Rush) - PASS

- positions lobby: Bot1 minecraft:overworld (-1.7,154.0,4.3) survival; Bot2 minecraft:overworld (2.3,142.4,19.0) survival; Bot3 minecraft:overworld (7.1,154.0,5.6) survival; Bot4 minecraft:overworld (17.7,140.0,0.9) survival; Bot5 minecraft:overworld (-3.7,156.0,3.3) survival; Bot6 minecraft:overworld (-1.7,156.0,2.3) survival
- positions t10: Bot1 minecraft:mcc_rocketspleef (-7.9,101.0,2.5) adventure; Bot2 minecraft:mcc_rocketspleef (-14.0,101.0,8.9) adventure; Bot3 minecraft:mcc_rocketspleef (-4.4,101.8,11.2) adventure; Bot4 minecraft:mcc_rocketspleef (-0.2,102.2,17.1) adventure; Bot5 minecraft:mcc_rocketspleef (5.3,101.0,8.4) adventure; Bot6 minecraft:mcc_rocketspleef (-3.5,101.0,13.0) adventure
- positions t60: Bot1 minecraft:mcc_rocketspleef (-0.6,101.0,8.8) adventure; Bot2 minecraft:mcc_rocketspleef (12.9,89.0,-13.9) adventure; Bot3 minecraft:mcc_rocketspleef (0.8,89.0,16.4) adventure; Bot4 minecraft:mcc_rocketspleef (11.2,102.0,-1.5) adventure; Bot5 minecraft:mcc_rocketspleef (-0.1,101.0,13.1) adventure; Bot6 minecraft:mcc_rocketspleef (-15.7,101.0,-1.9) adventure
- positions after: Bot1 minecraft:overworld (3.3,150.0,15.3) survival; Bot2 minecraft:overworld (-3.8,144.0,27.4) survival; Bot3 minecraft:overworld (-5.7,157.0,-3.7) survival; Bot4 minecraft:overworld (-2.7,156.8,2.3) survival; Bot5 minecraft:overworld (5.7,156.8,5.1) survival; Bot6 minecraft:overworld (-2.9,155.0,6.6) survival

### pass1: railroadrush (Railroad Rush) - PASS

- positions lobby: Bot1 minecraft:overworld (3.7,152.0,11.3) survival; Bot2 minecraft:overworld (4.6,140.0,30.7) survival; Bot3 minecraft:overworld (-5.7,157.0,-2.9) survival; Bot4 minecraft:overworld (-2.7,156.8,2.3) survival; Bot5 minecraft:overworld (8.2,154.0,5.0) survival; Bot6 minecraft:overworld (-2.9,155.0,6.6) survival
- positions t10: Bot1 minecraft:mcc_railroadrush (93.9,64.0,8.1) survival; Bot2 minecraft:mcc_railroadrush (53.7,64.0,18.5) survival; Bot3 minecraft:mcc_railroadrush (5.7,64.0,24.4) survival; Bot4 minecraft:mcc_railroadrush (29.6,65.0,18.1) survival; Bot5 minecraft:mcc_railroadrush (69.6,64.0,6.2) survival; Bot6 minecraft:mcc_railroadrush (115.3,64.0,15.5) survival
- positions t60: Bot1 minecraft:mcc_railroadrush (-121.9,60.0,99.7) survival; Bot2 minecraft:mcc_railroadrush (-112.3,60.0,97.7) survival; Bot3 minecraft:mcc_railroadrush (-116.3,60.0,87.3) survival; Bot4 minecraft:mcc_railroadrush (-133.7,60.0,95.3) survival; Bot5 minecraft:mcc_railroadrush (-111.3,60.0,89.3) survival; Bot6 minecraft:mcc_railroadrush (-103.9,60.0,72.3) survival
- positions after: Bot1 minecraft:overworld (-1.3,154.8,4.9) survival; Bot2 minecraft:overworld (-1.8,152.0,9.9) survival; Bot3 minecraft:overworld (0.1,153.0,12.6) survival; Bot4 minecraft:overworld (2.2,145.4,17.3) survival; Bot5 minecraft:overworld (2.7,154.5,7.0) survival; Bot6 minecraft:overworld (14.7,140.0,5.9) survival

**WARN** [server] x1: Can't keep up! Is the server overloaded? Running 2544ms or 50 ticks behind

```
Can't keep up! Is the server overloaded? Running 2544ms or 50 ticks behind

```

### pass1: parkourwarrior (Parkour Warrior) - PASS

- positions lobby: Bot1 minecraft:overworld (4.9,153.5,8.7) survival; Bot2 minecraft:overworld (-2.1,152.0,9.7) survival; Bot3 minecraft:overworld (0.1,152.0,12.7) survival; Bot4 minecraft:overworld (3.3,140.0,20.5) survival; Bot5 minecraft:overworld (5.3,153.0,7.3) survival; Bot6 minecraft:overworld (14.7,140.0,5.9) survival
- positions t10: Bot1 minecraft:mcc_parkourwarrior (-12.0,101.0,7.7) adventure; Bot2 minecraft:mcc_parkourwarrior (-9.6,101.0,8.7) adventure; Bot3 minecraft:mcc_parkourwarrior (-20.8,101.0,6.9) adventure; Bot4 minecraft:mcc_parkourwarrior (-10.1,101.4,8.9) adventure; Bot5 minecraft:mcc_parkourwarrior (6.9,101.0,8.7) adventure; Bot6 minecraft:mcc_parkourwarrior (3.1,101.0,5.5) adventure
- positions t60: Bot1 minecraft:mcc_parkourwarrior (-20.8,101.8,5.4) adventure; Bot2 minecraft:mcc_parkourwarrior (-5.9,101.0,0.7) adventure; Bot3 minecraft:mcc_parkourwarrior (-22.2,101.0,6.4) adventure; Bot4 minecraft:mcc_parkourwarrior (-3.4,101.0,0.9) adventure; Bot5 minecraft:mcc_parkourwarrior (-10.4,101.0,2.5) adventure; Bot6 minecraft:mcc_parkourwarrior (2.5,101.0,6.0) adventure
- positions after: Bot1 minecraft:overworld (-4.1,147.0,19.0) survival; Bot2 minecraft:overworld (4.7,157.0,0.7) survival; Bot3 minecraft:overworld (6.5,151.0,8.7) survival; Bot4 minecraft:overworld (5.3,151.0,8.3) survival; Bot5 minecraft:overworld (1.9,157.0,2.5) survival; Bot6 minecraft:overworld (-5.7,157.0,-3.7) survival

**WARN** [bot-movement(benign)] x2: Bot1 moved too quickly! 20.044942553421972,-1.2491870787446828,-2.425

```
Bot1 moved too quickly! 20.044942553421972,-1.2491870787446828,-2.425

```

### pass1: parkourtag (Parkour Tag) - PASS

- positions lobby: Bot1 minecraft:overworld (-3.7,144.0,19.3) survival; Bot2 minecraft:overworld (4.7,158.0,0.9) survival; Bot3 minecraft:overworld (6.5,151.0,8.7) survival; Bot4 minecraft:overworld (5.6,151.0,8.3) survival; Bot5 minecraft:overworld (3.7,157.0,-1.7) survival; Bot6 minecraft:overworld (-5.4,157.0,-3.7) survival
- positions t10: Bot1 minecraft:mcc_parkourtag (18.5,65.0,2.5) adventure; Bot2 minecraft:mcc_parkourtag (18.4,65.0,38.3) adventure; Bot3 minecraft:mcc_parkourtag (20.5,65.0,2.5) adventure; Bot4 minecraft:mcc_parkourtag (19.5,65.0,38.5) adventure; Bot5 minecraft:mcc_parkourtag (20.5,65.0,38.5) adventure; Bot6 minecraft:mcc_parkourtag (19.5,65.0,2.6) adventure
- positions t60: Bot1 minecraft:mcc_parkourtag (24.3,65.0,39.6) adventure; Bot2 minecraft:mcc_parkourtag (11.3,65.0,39.7) adventure; Bot3 minecraft:mcc_parkourtag (13.0,65.0,17.2) adventure; Bot4 minecraft:mcc_parkourtag (27.1,65.0,13.0) adventure; Bot5 minecraft:mcc_parkourtag (39.7,65.0,34.6) adventure; Bot6 minecraft:mcc_parkourtag (39.7,65.0,6.1) adventure
- positions after: Bot1 minecraft:overworld (3.7,150.0,13.3) survival; Bot2 minecraft:overworld (-3.7,156.5,3.3) survival; Bot3 minecraft:overworld (-3.6,149.0,15.9) survival; Bot4 minecraft:overworld (4.7,157.0,0.7) survival; Bot5 minecraft:overworld (1.6,150.5,15.2) survival; Bot6 minecraft:overworld (-1.3,157.0,-4.7) survival

### pass1: meltdown (Meltdown) - PASS

- positions lobby: Bot1 minecraft:overworld (3.7,150.0,13.3) survival; Bot2 minecraft:overworld (-3.7,156.1,4.4) survival; Bot3 minecraft:overworld (-5.6,149.0,16.4) survival; Bot4 minecraft:overworld (4.7,157.0,0.7) survival; Bot5 minecraft:overworld (1.6,150.0,15.2) survival; Bot6 minecraft:overworld (-1.3,157.0,-4.7) survival
- positions t10: Bot1 minecraft:mcc_meltdown (24.6,65.0,9.3) survival; Bot2 minecraft:mcc_meltdown (12.1,65.0,54.8) survival; Bot3 minecraft:mcc_meltdown (27.0,65.0,101.3) survival; Bot4 minecraft:mcc_meltdown (104.7,66.3,56.0) survival; Bot5 minecraft:mcc_meltdown (83.0,65.0,104.6) survival; Bot6 minecraft:mcc_meltdown (82.9,65.0,9.4) survival
- positions t60: Bot1 minecraft:mcc_meltdown (53.7,65.0,33.3) survival; Bot2 minecraft:mcc_meltdown (31.7,66.2,79.7) survival; Bot3 minecraft:mcc_meltdown (17.3,65.0,95.7) survival; Bot4 minecraft:mcc_meltdown (97.3,66.2,49.3) survival; Bot5 minecraft:mcc_meltdown (93.3,65.0,103.1) survival; Bot6 minecraft:mcc_meltdown (17.3,66.3,6.0) survival
- positions after: Bot1 minecraft:overworld (5.3,152.2,8.3) survival; Bot2 minecraft:overworld (2.3,151.0,13.1) survival; Bot3 minecraft:overworld (-8.8,144.0,2.3) survival; Bot4 minecraft:overworld (-2.2,157.1,-3.3) survival; Bot5 minecraft:overworld (-7.3,148.8,16.6) survival; Bot6 minecraft:overworld (-3.9,156.0,4.9) survival

### pass1: hitw (Hole in the Wall) - PASS

- positions lobby: Bot1 minecraft:overworld (5.3,151.4,8.3) survival; Bot2 minecraft:overworld (2.3,152.8,13.1) survival; Bot3 minecraft:overworld (-8.8,144.0,2.3) survival; Bot4 minecraft:overworld (-3.8,157.0,-2.6) survival; Bot5 minecraft:overworld (-14.7,144.4,25.6) survival; Bot6 minecraft:overworld (-4.6,155.8,4.3) survival
- positions t10: Bot1 minecraft:mcc_hitw (0.6,99.8,-9.2) adventure; Bot2 minecraft:mcc_hitw (5.8,100.0,5.7) adventure; Bot3 minecraft:mcc_hitw (9.7,97.2,-7.6) adventure; Bot4 minecraft:mcc_hitw (-4.6,100.0,3.0) adventure; Bot5 minecraft:mcc_hitw (-0.5,100.0,-7.3) adventure; Bot6 minecraft:mcc_hitw (3.5,99.8,-9.2) adventure
- positions after: Bot1 minecraft:overworld (2.5,154.0,6.3) survival; Bot2 minecraft:overworld (-4.4,144.0,20.4) survival; Bot3 minecraft:overworld (-12.6,142.0,46.2) survival; Bot4 minecraft:overworld (-3.2,158.2,-0.6) survival; Bot5 minecraft:overworld (-10.5,142.0,38.1) survival; Bot6 minecraft:overworld (5.3,151.8,8.7) survival

### pass1: gridrunners (Grid Runners) - PASS

- positions lobby: Bot1 minecraft:overworld (2.7,154.5,6.3) survival; Bot2 minecraft:overworld (-4.4,144.0,20.4) survival; Bot3 minecraft:overworld (-12.6,143.3,46.2) survival; Bot4 minecraft:overworld (-5.7,157.0,-3.7) survival; Bot5 minecraft:overworld (-14.3,142.0,46.4) survival; Bot6 minecraft:overworld (5.6,151.0,8.3) survival
- positions t10: Bot1 minecraft:mcc_gridrunners (323.8,65.0,5.5) adventure; Bot2 minecraft:mcc_gridrunners (159.7,65.0,4.4) adventure; Bot3 minecraft:mcc_gridrunners (-4.5,65.0,8.0) adventure; Bot4 minecraft:mcc_gridrunners (86.7,65.0,7.3) adventure; Bot5 minecraft:mcc_gridrunners (246.1,65.0,14.7) adventure; Bot6 minecraft:mcc_gridrunners (406.7,65.0,8.3) adventure
- positions t60: Bot1 minecraft:mcc_gridrunners (334.7,65.0,2.3) adventure; Bot2 minecraft:mcc_gridrunners (164.6,65.0,1.3) adventure; Bot3 minecraft:mcc_gridrunners (-4.1,65.0,3.3) adventure; Bot4 minecraft:mcc_gridrunners (94.7,65.0,11.3) adventure; Bot5 minecraft:mcc_gridrunners (246.0,65.0,9.8) adventure; Bot6 minecraft:mcc_gridrunners (410.9,65.8,14.5) adventure
- positions after: Bot1 minecraft:overworld (6.2,154.0,6.5) survival; Bot2 minecraft:overworld (-24.6,140.0,7.9) survival; Bot3 minecraft:overworld (2.3,151.0,12.3) survival; Bot4 minecraft:overworld (-4.7,155.8,4.3) survival; Bot5 minecraft:overworld (6.3,149.0,10.7) survival; Bot6 minecraft:overworld (-0.3,153.0,11.4) survival

**WARN** [server] x1: Can't keep up! Is the server overloaded? Running 3460ms or 69 ticks behind

```
Can't keep up! Is the server overloaded? Running 3460ms or 69 ticks behind

```

### pass1: footrace (Foot Race) - PASS

- positions lobby: Bot1 minecraft:overworld (6.2,154.0,6.5) survival; Bot2 minecraft:overworld (-24.6,140.0,7.9) survival; Bot3 minecraft:overworld (2.3,151.0,12.3) survival; Bot4 minecraft:overworld (-4.7,155.8,4.3) survival; Bot5 minecraft:overworld (6.3,149.0,10.7) survival; Bot6 minecraft:overworld (1.5,155.0,5.3) survival
- positions t10: Bot1 minecraft:mcc_footrace (19.5,101.0,7.8) adventure; Bot2 minecraft:mcc_footrace (23.9,101.0,7.9) adventure; Bot3 minecraft:mcc_footrace (25.2,101.0,7.9) adventure; Bot4 minecraft:mcc_footrace (15.2,101.0,2.0) adventure; Bot5 minecraft:mcc_footrace (24.3,101.0,7.9) adventure; Bot6 minecraft:mcc_footrace (25.4,101.0,1.3) adventure
- positions t60: Bot1 minecraft:mcc_footrace (35.7,101.0,0.7) adventure; Bot2 minecraft:mcc_footrace (27.1,101.0,7.2) adventure; Bot3 minecraft:mcc_footrace (22.7,103.0,-0.5) adventure; Bot4 minecraft:mcc_footrace (20.3,101.0,0.6) adventure; Bot5 minecraft:mcc_footrace (8.3,102.0,18.3) adventure; Bot6 minecraft:mcc_footrace (43.6,101.0,8.5) adventure
- positions after: Bot1 minecraft:overworld (-2.7,147.0,17.3) survival; Bot2 minecraft:overworld (-8.7,149.0,15.3) survival; Bot3 minecraft:overworld (-0.3,157.0,-1.2) survival; Bot4 minecraft:overworld (-3.7,156.1,3.3) survival; Bot5 minecraft:overworld (-0.2,152.0,13.2) survival; Bot6 minecraft:overworld (6.9,151.0,9.1) survival

### pass1: dodgebolt (Dodgebolt) - PASS

- positions lobby: Bot1 minecraft:overworld (-2.7,147.0,17.3) survival; Bot2 minecraft:overworld (-8.7,149.0,15.3) survival; Bot3 minecraft:overworld (-2.7,157.4,-4.7) survival; Bot4 minecraft:overworld (-3.7,156.0,3.3) survival; Bot5 minecraft:overworld (-2.2,151.5,12.2) survival; Bot6 minecraft:overworld (5.3,151.0,8.3) survival
- positions t10: Bot1 minecraft:mcc_dodgebolt (-13.7,70.3,-8.1) adventure; Bot2 minecraft:mcc_dodgebolt (-11.3,67.0,-7.3) adventure; Bot3 minecraft:mcc_dodgebolt (7.6,67.0,-15.3) adventure; Bot4 minecraft:mcc_dodgebolt (-1.2,65.0,-1.0) adventure; Bot5 minecraft:mcc_dodgebolt (12.3,67.1,-8.3) adventure; Bot6 minecraft:mcc_dodgebolt (0.5,65.0,11.5) adventure
- positions t60: Bot1 minecraft:mcc_dodgebolt (-13.7,69.0,-13.1) adventure; Bot2 minecraft:mcc_dodgebolt (-11.7,67.0,16.2) adventure; Bot3 minecraft:mcc_dodgebolt (-12.0,68.0,5.4) adventure; Bot4 minecraft:mcc_dodgebolt (-9.6,66.0,-1.0) adventure; Bot5 minecraft:mcc_dodgebolt (13.7,68.0,-16.7) adventure; Bot6 minecraft:mcc_dodgebolt (-2.4,65.0,3.7) adventure
- positions after: Bot1 minecraft:overworld (11.5,140.0,12.3) survival; Bot2 minecraft:overworld (0.5,157.0,3.1) survival; Bot3 minecraft:overworld (-2.3,156.0,2.3) survival; Bot4 minecraft:overworld (-2.7,155.9,2.6) survival; Bot5 minecraft:overworld (5.7,160.9,0.1) survival; Bot6 minecraft:overworld (-3.0,155.0,6.4) survival

### pass1: buildmart (Build Mart) - PASS

- positions lobby: Bot1 minecraft:overworld (11.5,140.0,12.3) survival; Bot2 minecraft:overworld (-1.7,154.8,6.7) survival; Bot3 minecraft:overworld (-2.3,156.0,2.3) survival; Bot4 minecraft:overworld (-2.7,155.9,2.6) survival; Bot5 minecraft:overworld (5.5,160.5,0.1) survival; Bot6 minecraft:overworld (3.7,152.0,12.2) survival
- positions t10: Bot1 minecraft:mcc_buildmart (-3.6,65.0,53.1) survival; Bot2 minecraft:mcc_buildmart (35.1,65.0,-23.8) survival; Bot3 minecraft:mcc_buildmart (8.8,65.0,-53.0) survival; Bot4 minecraft:mcc_buildmart (-35.9,65.0,28.1) survival; Bot5 minecraft:mcc_buildmart (42.5,65.0,24.5) survival; Bot6 minecraft:mcc_buildmart (-30.1,64.5,-18.8) survival
- positions t60: Bot1 minecraft:mcc_buildmart (8.0,65.0,51.7) survival; Bot2 minecraft:mcc_buildmart (31.4,64.5,-20.0) survival; Bot3 minecraft:mcc_buildmart (-6.9,65.0,-53.0) survival; Bot4 minecraft:mcc_buildmart (-43.8,65.0,28.8) survival; Bot5 minecraft:mcc_buildmart (34.9,65.0,26.3) survival; Bot6 minecraft:mcc_buildmart (-42.3,65.0,-24.8) survival
- positions after: Bot1 minecraft:overworld (6.4,153.0,7.1) survival; Bot2 minecraft:overworld (3.7,150.0,13.3) survival; Bot3 minecraft:overworld (-1.8,156.0,2.3) survival; Bot4 minecraft:overworld (4.7,157.0,0.3) survival; Bot5 minecraft:overworld (5.3,153.0,7.6) survival; Bot6 minecraft:overworld (1.6,147.2,16.7) survival

**WARN** [bot-movement(benign)] x1: Bot3 moved wrongly!

```
Bot3 moved wrongly!

```

### pass1: bingo (Bingo but Fast) - PASS

- positions lobby: Bot1 minecraft:overworld (6.4,153.8,7.1) survival; Bot2 minecraft:overworld (2.8,151.0,13.0) survival; Bot3 minecraft:overworld (-1.8,156.0,2.3) survival; Bot4 minecraft:overworld (4.2,157.0,0.7) survival; Bot5 minecraft:overworld (3.1,150.0,14.3) survival; Bot6 minecraft:overworld (3.9,145.0,16.5) survival
- positions t10: Bot1 minecraft:mcc_bingo (-64.5,63.0,80.3) survival; Bot2 minecraft:mcc_bingo (-39.3,67.0,78.4) survival; Bot3 minecraft:mcc_bingo (-34.7,64.0,64.3) survival; Bot4 minecraft:mcc_bingo (-58.4,63.0,58.5) survival; Bot5 minecraft:mcc_bingo (-56.5,64.0,84.5) survival; Bot6 minecraft:mcc_bingo (-37.6,65.0,66.4) survival
- positions t60: Bot1 minecraft:mcc_bingo (-96.2,58.1,101.4) survival; Bot2 minecraft:mcc_bingo (-24.7,62.0,51.4) survival; Bot3 minecraft:mcc_bingo (-91.1,57.7,60.7) survival; Bot4 minecraft:mcc_bingo (-77.9,60.5,72.9) survival; Bot5 minecraft:mcc_bingo (-56.7,64.0,83.3) survival; Bot6 minecraft:mcc_bingo (-40.6,65.1,91.3) survival
- positions after: Bot1 minecraft:overworld (4.0,154.0,7.9) survival; Bot2 minecraft:overworld (3.3,150.0,15.3) survival; Bot3 minecraft:overworld (6.0,141.0,16.3) survival; Bot4 minecraft:overworld (-1.7,155.5,6.2) survival; Bot5 minecraft:overworld (-1.7,146.0,17.3) survival; Bot6 minecraft:overworld (-0.7,154.0,7.9) survival

### pass1: lockout (Lockout Bingo) - PASS

- positions lobby: Bot1 minecraft:overworld (7.1,152.0,8.1) survival; Bot2 minecraft:overworld (10.5,139.9,13.6) survival; Bot3 minecraft:overworld (11.7,140.0,14.8) survival; Bot4 minecraft:overworld (1.8,157.0,-1.7) survival; Bot5 minecraft:overworld (-2.7,147.8,17.3) survival; Bot6 minecraft:overworld (-0.7,154.0,8.1) survival
- positions t10: Bot1 minecraft:mcc_bingo (248.9,68.0,16.2) survival; Bot2 minecraft:mcc_bingo (257.3,70.0,0.4) survival; Bot3 minecraft:mcc_bingo (248.9,68.0,-14.5) survival; Bot4 minecraft:mcc_bingo (221.6,65.0,0.7) survival; Bot5 minecraft:mcc_bingo (231.5,64.0,-14.9) survival; Bot6 minecraft:mcc_bingo (231.4,68.2,17.6) survival
- positions t60: Bot1 minecraft:mcc_bingo (212.6,59.5,51.3) survival; Bot2 minecraft:mcc_bingo (187.5,59.5,49.9) survival; Bot3 minecraft:mcc_bingo (220.0,57.1,-73.2) survival; Bot4 minecraft:mcc_bingo (210.7,60.4,-5.5) survival; Bot5 minecraft:mcc_bingo (247.6,68.0,-12.2) survival; Bot6 minecraft:mcc_bingo (259.8,68.2,71.8) survival
- positions after: Bot1 minecraft:overworld (-1.7,146.0,17.7) survival; Bot2 minecraft:overworld (-3.4,150.0,9.3) survival; Bot3 minecraft:overworld (-2.2,152.0,9.5) survival; Bot4 minecraft:overworld (8.8,150.0,5.5) survival; Bot5 minecraft:overworld (5.3,151.0,8.3) survival; Bot6 minecraft:overworld (-28.1,142.0,41.5) survival

**WARN** [bot-movement(benign)] x17: Bot3 moved too quickly! 8.537422869717322,0.0,5.2979521273535415

```
Bot3 moved too quickly! 8.537422869717322,0.0,5.2979521273535415

```

**WARN** [server] x1: Can't keep up! Is the server overloaded? Running 2998ms or 59 ticks behind

```
Can't keep up! Is the server overloaded? Running 2998ms or 59 ticks behind

```

### pass1: battlebox (Battle Box) - PASS

- positions lobby: Bot1 minecraft:overworld (-2.7,148.0,18.6) survival; Bot2 minecraft:overworld (-3.3,145.0,6.5) survival; Bot3 minecraft:overworld (-2.2,152.0,9.5) survival; Bot4 minecraft:overworld (8.3,150.0,6.2) survival; Bot5 minecraft:overworld (5.3,151.8,8.3) survival; Bot6 minecraft:overworld (-38.6,140.0,42.8) survival
- positions t10: Bot1 minecraft:mcc_battlebox (45.8,64.0,-11.3) adventure; Bot2 minecraft:mcc_battlebox (-1.5,65.3,-13.5) adventure; Bot3 minecraft:mcc_battlebox (46.5,64.0,14.5) adventure; Bot4 minecraft:mcc_battlebox (97.2,64.0,12.3) adventure; Bot5 minecraft:mcc_battlebox (-0.8,64.0,13.7) adventure; Bot6 minecraft:mcc_battlebox (95.0,64.0,-11.3) adventure
- positions t60: Bot1 minecraft:mcc_battlebox (43.1,64.0,6.3) survival; Bot2 minecraft:mcc_battlebox (-7.6,64.0,0.3) survival; Bot3 minecraft:mcc_battlebox (52.7,64.0,17.7) survival; Bot4 minecraft:mcc_battlebox (100.3,64.5,-8.7) survival; Bot5 minecraft:mcc_battlebox (6.8,64.0,-9.7) survival; Bot6 minecraft:mcc_battlebox (92.8,65.0,-16.7) survival
- positions after: Bot1 minecraft:overworld (-1.7,154.0,4.3) survival; Bot2 minecraft:overworld (4.3,157.0,0.7) survival; Bot3 minecraft:overworld (-1.2,154.5,5.8) survival; Bot4 minecraft:overworld (-1.8,149.2,17.2) survival; Bot5 minecraft:overworld (-2.3,156.2,7.0) survival; Bot6 minecraft:overworld (-3.1,156.0,3.3) survival

### pass1: acerace (Ace Race) - PASS

- positions lobby: Bot1 minecraft:overworld (-1.7,154.0,4.3) survival; Bot2 minecraft:overworld (4.7,157.0,0.7) survival; Bot3 minecraft:overworld (-1.2,154.5,5.8) survival; Bot4 minecraft:overworld (3.7,150.0,13.3) survival; Bot5 minecraft:overworld (-2.4,156.8,7.8) survival; Bot6 minecraft:overworld (-1.3,156.0,2.3) survival
- positions t10: Bot1 minecraft:mcc_acerace (11.5,100.0,0.5) adventure; Bot2 minecraft:mcc_acerace (11.7,100.0,-2.7) adventure; Bot3 minecraft:mcc_acerace (11.5,100.0,-2.5) adventure; Bot4 minecraft:mcc_acerace (23.6,100.0,-1.2) adventure; Bot5 minecraft:mcc_acerace (0.3,100.5,-2.7) adventure; Bot6 minecraft:mcc_acerace (37.6,107.3,1.3) adventure
- positions t60: Bot1 minecraft:mcc_acerace (6.9,100.0,-0.3) adventure; Bot2 minecraft:mcc_acerace (40.8,106.8,3.6) adventure; Bot3 minecraft:mcc_acerace (26.3,100.0,-1.9) adventure; Bot4 minecraft:mcc_acerace (37.8,107.6,1.6) adventure; Bot5 minecraft:mcc_acerace (42.9,106.0,2.6) adventure; Bot6 minecraft:mcc_acerace (55.7,112.0,1.9) adventure
- positions after: Bot1 minecraft:overworld (4.7,154.3,10.7) survival; Bot2 minecraft:overworld (2.3,155.3,7.4) survival; Bot3 minecraft:overworld (1.0,150.4,14.9) survival; Bot4 minecraft:overworld (-1.3,150.7,15.2) survival; Bot5 minecraft:overworld (26.5,141.3,4.6) survival; Bot6 minecraft:overworld (0.0,147.0,18.5) survival

### pass2: tgttos (To Get To The Other Side) - PASS

- positions lobby: Bot1 minecraft:overworld (4.7,153.0,10.7) survival; Bot2 minecraft:overworld (2.3,154.1,7.4) survival; Bot3 minecraft:overworld (0.7,150.0,14.6) survival; Bot4 minecraft:overworld (-3.4,150.0,10.0) survival; Bot5 minecraft:overworld (26.3,140.0,0.3) survival; Bot6 minecraft:overworld (-0.6,146.0,17.3) survival
- positions t10: Bot1 minecraft:mcc_tgttos (0.3,101.0,-5.8) survival; Bot2 minecraft:mcc_tgttos (1.5,101.0,-11.0) survival; Bot3 minecraft:mcc_tgttos (0.3,101.4,-11.7) survival; Bot4 minecraft:mcc_tgttos (6.5,101.0,9.8) survival; Bot5 minecraft:mcc_tgttos (3.8,102.2,11.6) survival; Bot6 minecraft:mcc_tgttos (8.9,101.0,-2.5) survival
- positions t60: Bot1 minecraft:mcc_tgttos (10.1,76.0,12.7) survival; Bot2 minecraft:mcc_tgttos (0.3,101.0,-7.4) survival; Bot3 minecraft:mcc_tgttos (29.4,76.0,4.7) survival; Bot4 minecraft:mcc_tgttos (0.3,101.0,-2.5) survival; Bot5 minecraft:mcc_tgttos (34.7,76.0,12.7) survival; Bot6 minecraft:mcc_tgttos (29.2,76.0,-0.3) survival
- positions after: Bot1 minecraft:overworld (5.8,154.0,13.5) survival; Bot2 minecraft:overworld (3.7,157.0,-1.7) survival; Bot3 minecraft:overworld (6.4,151.0,8.7) survival; Bot4 minecraft:overworld (3.7,150.0,13.3) survival; Bot5 minecraft:overworld (0.6,155.4,6.0) survival; Bot6 minecraft:overworld (-8.7,148.2,13.1) survival

### pass2: skybattle (Sky Battle) - PASS

- positions lobby: Bot1 minecraft:overworld (5.7,154.0,12.9) survival; Bot2 minecraft:overworld (3.7,157.0,-1.7) survival; Bot3 minecraft:overworld (5.3,151.0,8.3) survival; Bot4 minecraft:overworld (3.7,150.0,13.4) survival; Bot5 minecraft:overworld (1.6,155.0,6.3) survival; Bot6 minecraft:overworld (-8.7,148.0,13.7) survival
- positions t10: Bot1 minecraft:mcc_skybattle (-27.9,101.0,-15.2) survival; Bot2 minecraft:mcc_skybattle (-1.5,101.0,-31.1) survival; Bot3 minecraft:mcc_skybattle (-1.4,101.5,33.0) survival; Bot4 minecraft:mcc_skybattle (-34.1,101.0,17.6) survival; Bot5 minecraft:mcc_skybattle (23.9,68.3,-28.7) survival; Bot6 minecraft:mcc_skybattle (30.9,101.0,22.3) survival
- positions t60: Bot1 minecraft:mcc_skybattle (-35.1,101.0,-17.5) survival; Bot2 minecraft:mcc_skybattle (0.0,-123.4,-10.2) spectator; Bot3 minecraft:mcc_skybattle (-4.1,101.5,36.9) survival; Bot4 minecraft:mcc_skybattle (-35.0,101.0,16.2) survival; Bot5 minecraft:mcc_skybattle (-3.6,104.0,8.8) spectator; Bot6 minecraft:mcc_skybattle (33.4,101.8,20.8) survival
- positions after: Bot1 minecraft:overworld (2.3,150.0,13.3) survival; Bot2 minecraft:overworld (4.4,158.0,2.0) survival; Bot3 minecraft:overworld (0.5,157.0,0.5) survival; Bot4 minecraft:overworld (-3.5,156.0,4.5) survival; Bot5 minecraft:overworld (-1.3,156.0,2.3) survival; Bot6 minecraft:overworld (-7.3,147.0,12.7) survival

**WARN** [bot-movement(benign)] x35: Bot5 moved too quickly! -22.098484894127477,73.68868037874728,30.310859911600758

```
Bot5 moved too quickly! -22.098484894127477,73.68868037874728,30.310859911600758

```

### pass2: sandsoftime (Sands of Time) - PASS

- positions lobby: Bot1 minecraft:overworld (3.4,150.0,13.3) survival; Bot2 minecraft:overworld (6.5,154.4,5.9) survival; Bot3 minecraft:overworld (0.5,157.0,0.6) survival; Bot4 minecraft:overworld (-1.9,156.0,2.3) survival; Bot5 minecraft:overworld (-1.3,156.0,2.3) survival; Bot6 minecraft:overworld (-6.7,149.0,13.6) survival
- positions t10: Bot1 minecraft:mcc_sandsoftime (530.0,65.0,3.7) survival; Bot2 minecraft:mcc_sandsoftime (25.3,65.0,11.7) survival; Bot3 minecraft:mcc_sandsoftime (325.3,65.0,11.7) survival; Bot4 minecraft:mcc_sandsoftime (416.3,65.0,6.3) survival; Bot5 minecraft:mcc_sandsoftime (121.7,65.0,7.2) survival; Bot6 minecraft:mcc_sandsoftime (228.1,65.0,11.7) survival
- positions t60: Bot1 minecraft:mcc_sandsoftime (504.1,72.0,25.1) spectator; Bot2 minecraft:mcc_sandsoftime (35.7,65.0,23.7) survival; Bot3 minecraft:mcc_sandsoftime (322.0,65.0,1.3) survival; Bot4 minecraft:mcc_sandsoftime (418.5,65.0,2.3) survival; Bot5 minecraft:mcc_sandsoftime (125.3,65.5,37.6) survival; Bot6 minecraft:mcc_sandsoftime (227.7,65.8,-0.3) adventure
- positions after: Bot1 minecraft:overworld (-0.6,151.5,13.4) survival; Bot2 minecraft:overworld (-16.0,144.0,17.9) survival; Bot3 minecraft:overworld (-29.4,141.0,40.0) survival; Bot4 minecraft:overworld (2.3,147.0,16.7) survival; Bot5 minecraft:overworld (7.7,156.2,4.0) survival; Bot6 minecraft:overworld (0.7,154.1,6.3) survival

### pass2: rocketspleef (Rocket Spleef Rush) - PASS

- positions lobby: Bot1 minecraft:overworld (-2.7,147.0,17.3) survival; Bot2 minecraft:overworld (-17.7,140.9,12.3) survival; Bot3 minecraft:overworld (-30.3,140.0,45.7) survival; Bot4 minecraft:overworld (2.6,146.0,16.8) survival; Bot5 minecraft:overworld (7.3,156.2,3.3) survival; Bot6 minecraft:overworld (0.7,155.2,6.3) survival
- positions t10: Bot1 minecraft:mcc_rocketspleef (-14.1,101.0,-6.9) adventure; Bot2 minecraft:mcc_rocketspleef (-13.3,101.0,-8.8) adventure; Bot3 minecraft:mcc_rocketspleef (3.7,101.0,-7.8) adventure; Bot4 minecraft:mcc_rocketspleef (-0.1,101.0,11.7) adventure; Bot5 minecraft:mcc_rocketspleef (7.5,102.2,14.9) adventure; Bot6 minecraft:mcc_rocketspleef (-12.8,102.2,10.9) adventure
- positions t60: Bot1 minecraft:mcc_rocketspleef (-10.5,89.0,-0.5) adventure; Bot2 minecraft:mcc_rocketspleef (-15.1,89.0,7.0) adventure; Bot3 minecraft:mcc_rocketspleef (10.4,101.0,2.4) adventure; Bot4 minecraft:mcc_rocketspleef (-2.3,89.0,18.9) adventure; Bot5 minecraft:mcc_rocketspleef (14.1,101.0,9.8) adventure; Bot6 minecraft:mcc_rocketspleef (-9.9,-632.3,9.6) spectator
- positions after: Bot1 minecraft:overworld (0.2,157.0,1.3) survival; Bot2 minecraft:overworld (4.2,157.0,0.7) survival; Bot3 minecraft:overworld (-23.3,140.0,3.9) survival; Bot4 minecraft:overworld (-3.4,156.2,6.4) survival; Bot5 minecraft:overworld (-16.6,144.0,19.4) survival; Bot6 minecraft:overworld (-8.7,148.0,13.1) survival

### pass2: railroadrush (Railroad Rush) - PASS

- positions lobby: Bot1 minecraft:overworld (-1.6,154.8,4.5) survival; Bot2 minecraft:overworld (4.7,157.0,0.7) survival; Bot3 minecraft:overworld (-23.3,140.8,4.2) survival; Bot4 minecraft:overworld (-3.4,155.0,6.4) survival; Bot5 minecraft:overworld (-16.6,142.0,15.6) survival; Bot6 minecraft:overworld (-8.7,148.0,13.1) survival
- positions t10: Bot1 minecraft:mcc_railroadrush (97.1,64.0,0.3) survival; Bot2 minecraft:mcc_railroadrush (43.3,64.0,9.6) survival; Bot3 minecraft:mcc_railroadrush (117.5,64.0,3.9) survival; Bot4 minecraft:mcc_railroadrush (-4.3,64.0,29.7) survival; Bot5 minecraft:mcc_railroadrush (29.7,64.0,37.2) survival; Bot6 minecraft:mcc_railroadrush (77.7,64.0,27.3) survival
- positions t60: Bot1 minecraft:mcc_railroadrush (-122.7,60.0,93.7) survival; Bot2 minecraft:mcc_railroadrush (-128.0,60.0,83.3) survival; Bot3 minecraft:mcc_railroadrush (-111.1,60.8,92.3) survival; Bot4 minecraft:mcc_railroadrush (-120.4,60.0,86.3) survival; Bot5 minecraft:mcc_railroadrush (-110.3,60.0,93.7) survival; Bot6 minecraft:mcc_railroadrush (-108.8,60.0,107.7) survival
- positions after: Bot1 minecraft:overworld (-0.8,148.0,18.7) survival; Bot2 minecraft:overworld (-3.7,156.0,3.3) survival; Bot3 minecraft:overworld (-4.5,150.0,12.9) survival; Bot4 minecraft:overworld (0.5,149.0,16.2) survival; Bot5 minecraft:overworld (1.4,151.0,13.3) survival; Bot6 minecraft:overworld (-6.9,149.6,10.7) survival

### pass2: parkourwarrior (Parkour Warrior) - PASS

- positions lobby: Bot1 minecraft:overworld (-1.7,146.0,17.3) survival; Bot2 minecraft:overworld (-3.7,156.0,3.3) survival; Bot3 minecraft:overworld (-5.5,149.0,11.9) survival; Bot4 minecraft:overworld (0.7,149.0,15.3) survival; Bot5 minecraft:overworld (3.7,151.0,12.3) survival; Bot6 minecraft:overworld (-8.9,142.0,9.5) survival
- positions t10: Bot1 minecraft:mcc_parkourwarrior (-7.9,101.0,9.6) adventure; Bot2 minecraft:mcc_parkourwarrior (-12.1,101.0,0.4) adventure; Bot3 minecraft:mcc_parkourwarrior (17.6,102.2,9.3) adventure; Bot4 minecraft:mcc_parkourwarrior (-0.5,101.0,6.4) adventure; Bot5 minecraft:mcc_parkourwarrior (-20.9,101.4,4.0) adventure; Bot6 minecraft:mcc_parkourwarrior (19.2,101.0,7.7) adventure
- positions t60: Bot1 minecraft:mcc_parkourwarrior (-16.4,101.0,4.2) adventure; Bot2 minecraft:mcc_parkourwarrior (11.0,101.5,9.4) adventure; Bot3 minecraft:mcc_parkourwarrior (2.5,101.8,2.8) adventure; Bot4 minecraft:mcc_parkourwarrior (-4.3,101.0,10.1) adventure; Bot5 minecraft:mcc_parkourwarrior (-13.5,101.0,2.5) adventure; Bot6 minecraft:mcc_parkourwarrior (-5.5,101.0,10.1) adventure
- positions after: Bot1 minecraft:overworld (8.8,153.0,4.3) survival; Bot2 minecraft:overworld (6.3,153.0,7.4) survival; Bot3 minecraft:overworld (4.3,155.5,6.2) survival; Bot4 minecraft:overworld (0.5,150.0,15.3) survival; Bot5 minecraft:overworld (8.7,139.9,19.7) survival; Bot6 minecraft:overworld (4.4,157.0,0.3) survival

**WARN** [bot-movement(benign)] x9: Bot3 moved too quickly! -11.458673519132121,0.0,1.562245728253606

```
Bot3 moved too quickly! -11.458673519132121,0.0,1.562245728253606

```

### pass2: parkourtag (Parkour Tag) - PASS

- positions lobby: Bot1 minecraft:overworld (8.3,153.0,4.3) survival; Bot2 minecraft:overworld (6.3,152.0,7.4) survival; Bot3 minecraft:overworld (6.3,154.0,7.3) survival; Bot4 minecraft:overworld (0.5,150.8,15.3) survival; Bot5 minecraft:overworld (8.7,139.9,19.7) survival; Bot6 minecraft:overworld (4.4,157.0,0.3) survival
- positions t10: Bot1 minecraft:mcc_parkourtag (19.5,65.0,38.5) adventure; Bot2 minecraft:mcc_parkourtag (20.5,65.0,38.5) adventure; Bot3 minecraft:mcc_parkourtag (18.5,65.0,2.5) adventure; Bot4 minecraft:mcc_parkourtag (20.5,65.0,2.5) adventure; Bot5 minecraft:mcc_parkourtag (18.5,65.0,38.5) adventure; Bot6 minecraft:mcc_parkourtag (19.5,65.0,2.5) adventure
- positions t60: Bot1 minecraft:mcc_parkourtag (23.1,65.0,15.8) adventure; Bot2 minecraft:mcc_parkourtag (38.1,66.1,37.3) adventure; Bot3 minecraft:mcc_parkourtag (16.4,65.0,4.6) adventure; Bot4 minecraft:mcc_parkourtag (32.0,65.0,32.2) adventure; Bot5 minecraft:mcc_parkourtag (35.3,65.0,10.3) adventure; Bot6 minecraft:mcc_parkourtag (10.7,65.5,38.3) adventure
- positions after: Bot1 minecraft:overworld (0.5,150.0,15.0) survival; Bot2 minecraft:overworld (8.3,149.4,7.8) survival; Bot3 minecraft:overworld (2.5,154.0,8.5) survival; Bot4 minecraft:overworld (-1.0,154.0,6.7) survival; Bot5 minecraft:overworld (4.6,154.0,8.0) survival; Bot6 minecraft:overworld (-11.7,155.4,2.3) survival

### pass2: meltdown (Meltdown) - PASS

- positions lobby: Bot1 minecraft:overworld (0.5,150.0,15.0) survival; Bot2 minecraft:overworld (7.3,149.0,7.3) survival; Bot3 minecraft:overworld (2.7,154.7,12.3) survival; Bot4 minecraft:overworld (0.0,151.8,13.7) survival; Bot5 minecraft:overworld (4.6,154.0,8.0) survival; Bot6 minecraft:overworld (-11.7,155.4,2.3) survival
- positions t10: Bot1 minecraft:mcc_meltdown (104.3,65.0,11.7) survival; Bot2 minecraft:mcc_meltdown (75.0,65.0,98.6) survival; Bot3 minecraft:mcc_meltdown (13.4,65.0,102.4) survival; Bot4 minecraft:mcc_meltdown (10.3,65.0,47.6) survival; Bot5 minecraft:mcc_meltdown (99.6,65.8,68.3) survival; Bot6 minecraft:mcc_meltdown (39.6,65.0,8.9) survival
- positions t60: Bot1 minecraft:mcc_meltdown (97.3,65.0,26.8) survival; Bot2 minecraft:mcc_meltdown (65.3,65.0,95.7) survival; Bot3 minecraft:mcc_meltdown (15.7,65.0,72.0) survival; Bot4 minecraft:mcc_meltdown (15.7,65.0,49.3) survival; Bot5 minecraft:mcc_meltdown (97.3,65.0,65.3) survival; Bot6 minecraft:mcc_meltdown (31.7,65.0,31.7) survival
- positions after: Bot1 minecraft:overworld (-22.0,140.0,6.6) survival; Bot2 minecraft:overworld (0.5,154.5,9.3) survival; Bot3 minecraft:overworld (1.5,155.0,6.6) survival; Bot4 minecraft:overworld (-2.5,156.0,7.7) survival; Bot5 minecraft:overworld (0.3,149.9,16.0) survival; Bot6 minecraft:overworld (0.5,147.3,17.3) survival

### pass2: hitw (Hole in the Wall) - PASS

- positions lobby: Bot1 minecraft:overworld (-20.1,140.0,3.3) survival; Bot2 minecraft:overworld (0.5,153.5,9.3) survival; Bot3 minecraft:overworld (2.5,154.0,9.5) survival; Bot4 minecraft:overworld (-0.2,154.5,8.7) survival; Bot5 minecraft:overworld (-7.2,149.8,17.2) survival; Bot6 minecraft:overworld (0.5,146.0,17.3) survival
- positions t10: Bot1 minecraft:mcc_hitw (-7.1,100.0,0.7) adventure; Bot2 minecraft:mcc_hitw (7.6,100.0,-7.0) adventure; Bot3 minecraft:mcc_hitw (-7.8,100.0,-0.8) adventure; Bot4 minecraft:mcc_hitw (-6.3,100.0,-6.9) adventure; Bot5 minecraft:mcc_hitw (-8.0,101.2,-6.8) adventure; Bot6 minecraft:mcc_hitw (-9.0,100.6,2.0) adventure
- positions after: Bot1 minecraft:overworld (4.7,157.0,0.3) survival; Bot2 minecraft:overworld (2.3,150.0,13.3) survival; Bot3 minecraft:overworld (-14.3,144.0,18.7) survival; Bot4 minecraft:overworld (4.6,154.0,10.7) survival; Bot5 minecraft:overworld (-19.7,143.0,17.3) survival; Bot6 minecraft:overworld (5.3,151.0,8.3) survival

### pass2: gridrunners (Grid Runners) - PASS

- positions lobby: Bot1 minecraft:overworld (4.7,157.0,0.3) survival; Bot2 minecraft:overworld (2.3,150.0,13.3) survival; Bot3 minecraft:overworld (-14.3,144.0,18.7) survival; Bot4 minecraft:overworld (6.5,154.1,14.4) survival; Bot5 minecraft:overworld (-19.7,143.0,17.3) survival; Bot6 minecraft:overworld (5.3,151.0,8.3) survival
- positions t10: Bot1 minecraft:mcc_gridrunners (319.7,65.0,12.7) adventure; Bot2 minecraft:mcc_gridrunners (155.5,65.0,8.0) adventure; Bot3 minecraft:mcc_gridrunners (399.5,65.0,12.7) adventure; Bot4 minecraft:mcc_gridrunners (-7.7,65.0,4.2) adventure; Bot5 minecraft:mcc_gridrunners (79.7,65.0,4.5) adventure; Bot6 minecraft:mcc_gridrunners (241.3,65.0,2.7) adventure
- positions t60: Bot1 minecraft:mcc_gridrunners (322.1,65.0,1.3) adventure; Bot2 minecraft:mcc_gridrunners (159.7,65.0,11.9) adventure; Bot3 minecraft:mcc_gridrunners (403.3,65.0,11.6) adventure; Bot4 minecraft:mcc_gridrunners (5.3,65.0,10.3) adventure; Bot5 minecraft:mcc_gridrunners (91.6,65.0,9.3) adventure; Bot6 minecraft:mcc_gridrunners (253.3,65.0,10.4) adventure
- positions after: Bot1 minecraft:overworld (5.3,146.0,13.3) survival; Bot2 minecraft:overworld (-2.2,151.0,9.3) survival; Bot3 minecraft:overworld (0.5,150.2,16.2) survival; Bot4 minecraft:overworld (-3.7,156.0,4.5) survival; Bot5 minecraft:overworld (-2.3,147.0,5.9) survival; Bot6 minecraft:overworld (4.7,153.0,10.1) survival

**WARN** [bot-movement(benign)] x2: Bot3 moved wrongly!

```
Bot3 moved wrongly!

```

### pass2: footrace (Foot Race) - PASS

- positions lobby: Bot1 minecraft:overworld (6.1,146.5,13.3) survival; Bot2 minecraft:overworld (-2.3,151.0,9.3) survival; Bot3 minecraft:overworld (0.7,149.0,15.8) survival; Bot4 minecraft:overworld (-1.3,154.0,4.6) survival; Bot5 minecraft:overworld (-2.3,147.0,5.9) survival; Bot6 minecraft:overworld (4.7,153.0,10.7) survival
- positions t10: Bot1 minecraft:mcc_footrace (27.7,101.0,1.9) adventure; Bot2 minecraft:mcc_footrace (33.8,101.8,0.3) adventure; Bot3 minecraft:mcc_footrace (3.3,101.0,7.8) adventure; Bot4 minecraft:mcc_footrace (23.3,101.4,1.7) adventure; Bot5 minecraft:mcc_footrace (25.0,102.3,7.9) adventure; Bot6 minecraft:mcc_footrace (27.0,101.0,8.4) adventure
- positions t60: Bot1 minecraft:mcc_footrace (21.2,102.0,9.8) adventure; Bot2 minecraft:mcc_footrace (43.2,102.0,10.3) adventure; Bot3 minecraft:mcc_footrace (0.8,101.0,13.5) adventure; Bot4 minecraft:mcc_footrace (19.5,101.4,1.7) adventure; Bot5 minecraft:mcc_footrace (21.8,101.0,3.4) adventure; Bot6 minecraft:mcc_footrace (10.9,102.0,-0.0) adventure
- positions after: Bot1 minecraft:overworld (-3.5,150.0,9.9) survival; Bot2 minecraft:overworld (-3.7,156.0,3.3) survival; Bot3 minecraft:overworld (-14.7,144.0,15.8) survival; Bot4 minecraft:overworld (-16.4,146.0,22.0) survival; Bot5 minecraft:overworld (4.5,155.5,5.9) survival; Bot6 minecraft:overworld (-1.3,157.0,-4.7) survival

**WARN** [bot-movement(benign)] x4: Bot4 moved too quickly! 22.21292986420343,6.293512139530577,-18.703709186932965

```
Bot4 moved too quickly! 22.21292986420343,6.293512139530577,-18.703709186932965

```

### pass2: dodgebolt (Dodgebolt) - PASS

- positions lobby: Bot1 minecraft:overworld (-3.3,150.0,15.0) survival; Bot2 minecraft:overworld (-3.7,156.0,3.3) survival; Bot3 minecraft:overworld (-14.7,144.0,15.8) survival; Bot4 minecraft:overworld (-21.2,142.0,26.7) survival; Bot5 minecraft:overworld (-3.0,157.0,0.2) survival; Bot6 minecraft:overworld (-2.1,157.0,-4.7) survival
- positions t10: Bot1 minecraft:mcc_dodgebolt (-11.4,67.0,4.2) adventure; Bot2 minecraft:mcc_dodgebolt (-11.3,68.0,-5.7) adventure; Bot3 minecraft:mcc_dodgebolt (9.9,65.0,4.1) adventure; Bot4 minecraft:mcc_dodgebolt (0.5,65.5,-10.5) adventure; Bot5 minecraft:mcc_dodgebolt (-4.3,68.0,-16.3) adventure; Bot6 minecraft:mcc_dodgebolt (12.3,67.0,14.7) adventure
- positions t60: Bot1 minecraft:mcc_dodgebolt (-11.7,67.0,14.9) adventure; Bot2 minecraft:mcc_dodgebolt (-11.7,67.0,-8.7) adventure; Bot3 minecraft:mcc_dodgebolt (-9.7,65.0,2.0) adventure; Bot4 minecraft:mcc_dodgebolt (0.3,65.0,-1.0) adventure; Bot5 minecraft:mcc_dodgebolt (-12.7,68.0,6.9) adventure; Bot6 minecraft:mcc_dodgebolt (-12.7,68.4,-16.6) adventure
- positions after: Bot1 minecraft:overworld (-2.0,155.0,4.3) survival; Bot2 minecraft:overworld (-10.9,147.0,6.7) survival; Bot3 minecraft:overworld (-11.7,143.0,29.2) survival; Bot4 minecraft:overworld (2.2,150.0,13.3) survival; Bot5 minecraft:overworld (3.7,150.0,13.3) survival; Bot6 minecraft:overworld (-1.3,154.0,4.7) survival

### pass2: buildmart (Build Mart) - PASS

- positions lobby: Bot1 minecraft:overworld (-1.7,156.0,2.3) survival; Bot2 minecraft:overworld (-5.0,144.1,7.7) survival; Bot3 minecraft:overworld (-11.7,143.0,29.2) survival; Bot4 minecraft:overworld (2.3,150.0,13.3) survival; Bot5 minecraft:overworld (2.5,151.2,13.0) survival; Bot6 minecraft:overworld (-1.3,154.0,4.7) survival
- positions t10: Bot1 minecraft:mcc_buildmart (-2.6,59.7,40.6) survival; Bot2 minecraft:mcc_buildmart (31.3,64.6,-16.4) survival; Bot3 minecraft:mcc_buildmart (-43.9,65.0,-19.3) survival; Bot4 minecraft:mcc_buildmart (-34.6,55.0,18.0) survival; Bot5 minecraft:mcc_buildmart (0.4,64.5,-40.2) survival; Bot6 minecraft:mcc_buildmart (34.8,65.0,24.6) survival
- positions t60: Bot1 minecraft:mcc_buildmart (-0.9,64.5,40.0) survival; Bot2 minecraft:mcc_buildmart (30.4,64.5,-18.9) survival; Bot3 minecraft:mcc_buildmart (-48.5,65.0,-27.6) survival; Bot4 minecraft:mcc_buildmart (-33.0,64.5,18.4) survival; Bot5 minecraft:mcc_buildmart (1.1,64.5,-40.0) survival; Bot6 minecraft:mcc_buildmart (47.1,67.3,24.4) survival
- positions after: Bot1 minecraft:overworld (3.3,150.0,14.8) survival; Bot2 minecraft:overworld (-2.8,156.0,3.9) survival; Bot3 minecraft:overworld (-3.8,147.8,18.3) survival; Bot4 minecraft:overworld (2.3,152.0,12.3) survival; Bot5 minecraft:overworld (-19.6,143.0,27.1) survival; Bot6 minecraft:overworld (-1.3,156.0,2.3) survival

**WARN** [bot-movement(benign)] x2: Bot3 moved wrongly!

```
Bot3 moved wrongly!

```

### pass2: bingo (Bingo but Fast) - PASS

- positions lobby: Bot1 minecraft:overworld (3.1,150.4,14.8) survival; Bot2 minecraft:overworld (-2.8,156.0,3.9) survival; Bot3 minecraft:overworld (0.5,150.4,14.3) survival; Bot4 minecraft:overworld (2.3,152.0,12.3) survival; Bot5 minecraft:overworld (-19.0,143.2,34.2) survival; Bot6 minecraft:overworld (-1.3,156.0,2.3) survival
- positions t10: Bot1 minecraft:mcc_bingo (-14.0,77.0,-2.2) survival; Bot2 minecraft:mcc_bingo (7.6,82.8,8.2) survival; Bot3 minecraft:mcc_bingo (21.3,74.0,-27.7) survival; Bot4 minecraft:mcc_bingo (4.7,78.0,-13.3) survival; Bot5 minecraft:mcc_bingo (14.3,78.0,4.1) survival; Bot6 minecraft:mcc_bingo (-6.9,83.0,18.7) survival
- positions t60: Bot1 minecraft:mcc_bingo (-7.3,77.0,3.7) survival; Bot2 minecraft:mcc_bingo (6.3,79.0,4.7) survival; Bot3 minecraft:mcc_bingo (-14.3,69.0,-33.7) survival; Bot4 minecraft:mcc_bingo (15.5,77.1,-17.7) survival; Bot5 minecraft:mcc_bingo (8.2,81.0,7.7) survival; Bot6 minecraft:mcc_bingo (-40.7,76.0,38.7) survival
- positions after: Bot1 minecraft:overworld (-3.4,151.0,12.1) survival; Bot2 minecraft:overworld (0.4,142.0,19.5) survival; Bot3 minecraft:overworld (-5.6,149.0,11.1) survival; Bot4 minecraft:overworld (9.2,156.2,12.7) survival; Bot5 minecraft:overworld (1.5,157.0,2.3) survival; Bot6 minecraft:overworld (0.7,150.0,14.9) survival

### pass2: lockout (Lockout Bingo) - PASS

- positions lobby: Bot1 minecraft:overworld (-11.2,144.0,12.2) survival; Bot2 minecraft:overworld (1.4,142.0,20.3) survival; Bot3 minecraft:overworld (-4.4,149.0,10.7) survival; Bot4 minecraft:overworld (9.2,155.0,12.7) survival; Bot5 minecraft:overworld (4.7,157.0,0.3) survival; Bot6 minecraft:overworld (0.5,154.0,8.1) survival
- positions t10: Bot1 minecraft:mcc_bingo (-4.5,81.0,5.9) survival; Bot2 minecraft:mcc_bingo (18.7,61.0,0.3) survival; Bot3 minecraft:mcc_bingo (-4.8,78.0,-15.3) survival; Bot4 minecraft:mcc_bingo (-17.5,81.0,0.5) survival; Bot5 minecraft:mcc_bingo (9.3,68.0,-14.3) survival; Bot6 minecraft:mcc_bingo (3.3,81.0,19.7) survival
- positions t60: Bot1 minecraft:mcc_bingo (-13.7,72.0,-24.3) survival; Bot2 minecraft:mcc_bingo (15.5,55.0,3.7) survival; Bot3 minecraft:mcc_bingo (2.0,77.0,-20.2) survival; Bot4 minecraft:mcc_bingo (-50.8,61.5,-52.4) survival; Bot5 minecraft:mcc_bingo (20.2,55.0,-16.7) survival; Bot6 minecraft:mcc_bingo (22.8,76.0,39.9) survival
- positions after: Bot1 minecraft:overworld (6.8,154.0,5.8) survival; Bot2 minecraft:overworld (13.7,141.0,35.0) survival; Bot3 minecraft:overworld (-3.7,156.0,3.3) survival; Bot4 minecraft:overworld (-1.4,157.0,-0.4) survival; Bot5 minecraft:overworld (-4.1,143.0,24.7) survival; Bot6 minecraft:overworld (6.5,152.0,7.7) survival

### pass2: battlebox (Battle Box) - PASS

- positions lobby: Bot1 minecraft:overworld (7.0,154.0,5.9) survival; Bot2 minecraft:overworld (17.7,141.0,32.1) survival; Bot3 minecraft:overworld (-3.7,156.4,3.3) survival; Bot4 minecraft:overworld (-5.7,157.0,-3.7) survival; Bot5 minecraft:overworld (-4.7,143.0,24.7) survival; Bot6 minecraft:overworld (6.5,152.0,7.7) survival
- positions t10: Bot1 minecraft:mcc_battlebox (44.3,64.0,-11.4) adventure; Bot2 minecraft:mcc_battlebox (-1.2,64.8,-11.3) adventure; Bot3 minecraft:mcc_battlebox (46.5,64.0,12.3) adventure; Bot4 minecraft:mcc_battlebox (96.3,64.0,12.3) adventure; Bot5 minecraft:mcc_battlebox (-1.5,64.0,12.3) adventure; Bot6 minecraft:mcc_battlebox (94.5,64.0,-13.5) adventure
- positions t60: Bot1 minecraft:mcc_battlebox (52.7,64.0,-11.5) survival; Bot2 minecraft:mcc_battlebox (-3.7,64.0,12.3) survival; Bot3 minecraft:mcc_battlebox (52.7,64.0,12.3) survival; Bot4 minecraft:mcc_battlebox (105.1,64.0,10.3) survival; Bot5 minecraft:mcc_battlebox (7.0,64.7,8.2) survival; Bot6 minecraft:mcc_battlebox (99.5,64.0,-11.4) survival
- positions after: Bot1 minecraft:overworld (-5.7,157.0,-3.7) survival; Bot2 minecraft:overworld (4.7,157.8,0.3) survival; Bot3 minecraft:overworld (3.1,150.1,14.8) survival; Bot4 minecraft:overworld (0.7,147.0,16.3) survival; Bot5 minecraft:overworld (8.8,149.0,7.3) survival; Bot6 minecraft:overworld (1.7,157.0,-3.7) survival

**WARN** [bot-movement(benign)] x1: Bot6 moved wrongly!

```
Bot6 moved wrongly!

```

### pass2: acerace (Ace Race) - PASS

- positions lobby: Bot1 minecraft:overworld (-5.7,157.0,-3.7) survival; Bot2 minecraft:overworld (4.7,157.0,0.3) survival; Bot3 minecraft:overworld (3.1,150.0,14.8) survival; Bot4 minecraft:overworld (0.7,147.0,16.3) survival; Bot5 minecraft:overworld (9.1,150.0,4.8) survival; Bot6 minecraft:overworld (1.7,157.0,-3.7) survival
- positions t10: Bot1 minecraft:mcc_acerace (25.0,100.0,-2.1) adventure; Bot2 minecraft:mcc_acerace (15.5,100.0,0.5) adventure; Bot3 minecraft:mcc_acerace (3.4,100.0,2.1) adventure; Bot4 minecraft:mcc_acerace (0.3,100.0,3.7) adventure; Bot5 minecraft:mcc_acerace (11.5,100.8,-0.5) adventure; Bot6 minecraft:mcc_acerace (10.4,101.3,3.1) adventure
- positions t60: Bot1 minecraft:mcc_acerace (61.2,112.0,-1.6) adventure; Bot2 minecraft:mcc_acerace (15.5,100.0,0.5) adventure; Bot3 minecraft:mcc_acerace (32.4,101.0,0.5) adventure; Bot4 minecraft:mcc_acerace (27.0,100.0,-0.4) adventure; Bot5 minecraft:mcc_acerace (22.9,100.1,2.8) adventure; Bot6 minecraft:mcc_acerace (13.1,100.0,-0.3) adventure
- positions after: Bot1 minecraft:overworld (1.9,152.7,13.1) survival; Bot2 minecraft:overworld (5.2,156.0,4.3) survival; Bot3 minecraft:overworld (-8.7,149.0,15.3) survival; Bot4 minecraft:overworld (-5.3,157.0,-3.7) survival; Bot5 minecraft:overworld (-1.4,146.0,17.3) survival; Bot6 minecraft:overworld (-5.0,149.6,15.1) survival

**WARN** [bot-movement(benign)] x3: Bot3 moved too quickly! 15.2,0.0,-3.2

```
Bot3 moved too quickly! 15.2,0.0,-3.2

```


## Bot kicks / errors

None.

## Follow-up: lockout rerun (results-lockout-rerun.md)
- Pass 2 cap (12 min) equalled Lockout's own limit (720 s + 5 s countdown), so the main run skipped it ~5 s early. Rerun with --pass2-mins=14: ended naturally "Time's up" after 731 s; one 2.5 s "Can't keep up" at start (mcc_bingo world regenerated in start()).
- The rerun's "FAIL" is a harness false positive: newer core jar returns MCC-game players to the new hub world `mcc_lobby` (joins still land in `overworld`). The harness now accepts both as lobby.
- New at boot with that core: `[Multiverse-Core] Safe spawn NOT found!!!` for mcc_lobby (world.dat spawn 0,71,108 judged unsafe) - Multiverse ERROR, harmless so far.
