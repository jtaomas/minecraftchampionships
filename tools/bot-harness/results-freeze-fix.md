# MCM staging bot test - run 2026-11-24T03-38-29

Raw logs: `C:\Users\LeonS\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\bot-harness\logs\2026-11-24T03-38-29`
Bots: 6, pass1 watch 90s then /mcm next; pass2 natural end (timeout 13 min).

- Jars copied: McmBattleBox.jar, McmBingo.jar, McmDecisionDome.jar, McmDodgebolt.jar, McmFindTheButton.jar, McmFootRace.jar, McmGridRunners.jar, McmHoleInTheWall.jar, McmParkourTag.jar, McmParkourWarrior.jar, McmRocketSpleef.jar, McmSandsOfTime.jar, McmSkyBlockle.jar, McmTgttos.jar, MinecraftMondayCore.jar (core)

## Pass 1 (all games: play, watch, skip)

| id | name | result | started | ended | duration | bots in arena @10s / @60s | back in lobby | issues | bot kicks/errors |
|---|---|---|---|---|---|---|---|---|---|
| skyblockle | Sky Blockle | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | WARN server x1; WARN bot-movement(benign) x320 | 0 |
| sandsoftime | Sands of Time | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | WARN bot-movement(benign) x2 | 0 |
| gridrunners | Grid Runners | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | WARN bot-movement(benign) x4 | 0 |
| bingo | Bingo but Fast | **PASS** | yes | Skipped by Admin (skip) | 90s | 6/6 | true | WARN bot-movement(benign) x1 | 0 |

## Pass 2 (MCC games to natural end)

| id | name | result | end reason | ended by | duration | bots in arena @10s/@60s | back in lobby | issues |
|---|---|---|---|---|---|---|---|---|
| skyblockle | Sky Blockle | **PASS** | Last side standing | natural | 153s | 6/6 | true | WARN bot-movement(benign) x75 |
| sandsoftime | Sands of Time | **PASS** | Every team is out of the dungeon. | natural | 150s | 6/6 | true | WARN bot-movement(benign) x5 |
| gridrunners | Grid Runners | **PASS** | Time's up | natural | 609s | 6/6 | true | WARN bot-movement(benign) x1 |
| bingo | Bingo but Fast | **PASS** | Time's up | natural | 608s | 6/6 | true | - |

## Server lag ("Can't keep up")

- 14:39:18 boot: 4217 ms (84 ticks) **>2s**
- 14:39:44 pass1:skyblockle: 2564 ms (51 ticks) **>2s**

## Per-game details

### pass1: skyblockle (Sky Blockle) - PASS

- positions lobby: Bot1 minecraft:overworld (0.3,146.8,17.3) survival; Bot2 minecraft:overworld (-11.4,142.0,40.5) survival; Bot3 minecraft:overworld (7.9,152.0,6.3) survival; Bot4 minecraft:overworld (-3.7,156.0,4.4) survival; Bot5 minecraft:overworld (8.0,156.0,15.4) survival; Bot6 minecraft:overworld (3.7,157.0,-1.7) survival
- positions t10: Bot1 minecraft:mcc_skyblockle (0.5,101.0,138.5) survival; Bot2 minecraft:mcc_skyblockle (-118.5,101.0,70.5) survival; Bot3 minecraft:mcc_skyblockle (-118.4,101.0,-68.5) survival; Bot4 minecraft:mcc_skyblockle (0.5,101.0,-137.4) survival; Bot5 minecraft:mcc_skyblockle (119.4,101.0,-69.5) survival; Bot6 minecraft:mcc_skyblockle (119.5,101.0,70.5) survival
- positions t60: Bot1 minecraft:mcc_skyblockle (18.7,107.0,-3.3) spectator; Bot2 minecraft:mcc_skyblockle (-121.0,101.0,71.3) survival; Bot3 minecraft:mcc_skyblockle (10.7,105.0,13.7) spectator; Bot4 minecraft:mcc_skyblockle (1.8,101.0,-136.2) survival; Bot5 minecraft:mcc_skyblockle (8.1,102.0,10.2) spectator; Bot6 minecraft:mcc_skyblockle (-3.3,107.0,-19.7) spectator
- positions after: Bot1 minecraft:mcc_lobby (22.7,71.5,111.7) survival; Bot2 minecraft:mcc_lobby (-24.7,71.0,84.4) survival; Bot3 minecraft:mcc_lobby (30.3,71.0,87.1) survival; Bot4 minecraft:mcc_lobby (16.5,71.0,96.6) survival; Bot5 minecraft:mcc_lobby (15.5,72.2,80.3) survival; Bot6 minecraft:mcc_lobby (-15.1,71.0,60.8) survival

**WARN** [server] x1: Can't keep up! Is the server overloaded? Running 2564ms or 51 ticks behind

```
Can't keep up! Is the server overloaded? Running 2564ms or 51 ticks behind

```

**WARN** [bot-movement(benign)] x320: Bot6 moved too quickly! -120.59505792882521,72.58926478336295,-68.3201961500626

```
Bot6 moved too quickly! -120.59505792882521,72.58926478336295,-68.3201961500626

```

### pass1: sandsoftime (Sands of Time) - PASS

- positions lobby: Bot1 minecraft:mcc_lobby (22.7,71.5,111.7) survival; Bot2 minecraft:mcc_lobby (-20.1,71.0,75.9) survival; Bot3 minecraft:mcc_lobby (36.4,71.0,80.4) survival; Bot4 minecraft:mcc_lobby (19.7,71.0,97.1) survival; Bot5 minecraft:mcc_lobby (10.0,71.0,76.2) survival; Bot6 minecraft:mcc_lobby (-21.7,71.0,53.2) survival
- positions t10: Bot1 minecraft:mcc_sandsoftime (25.3,65.0,11.7) survival; Bot2 minecraft:mcc_sandsoftime (126.8,65.0,1.3) survival; Bot3 minecraft:mcc_sandsoftime (225.3,65.0,1.3) survival; Bot4 minecraft:mcc_sandsoftime (325.3,65.0,8.2) survival; Bot5 minecraft:mcc_sandsoftime (437.3,65.0,4.9) survival; Bot6 minecraft:mcc_sandsoftime (520.6,65.0,4.1) survival
- positions t60: Bot1 minecraft:mcc_sandsoftime (4.4,65.0,6.1) survival; Bot2 minecraft:mcc_sandsoftime (101.3,65.0,2.7) survival; Bot3 minecraft:mcc_sandsoftime (250.1,72.0,47.8) spectator; Bot4 minecraft:mcc_sandsoftime (316.1,65.0,6.2) survival; Bot5 minecraft:mcc_sandsoftime (427.4,65.0,-2.6) adventure; Bot6 minecraft:mcc_sandsoftime (513.3,65.5,11.7) survival
- positions after: Bot1 minecraft:mcc_lobby (23.9,71.0,85.7) survival; Bot2 minecraft:mcc_lobby (-5.6,72.2,89.7) survival; Bot3 minecraft:mcc_lobby (26.4,71.0,100.9) survival; Bot4 minecraft:mcc_lobby (-6.7,71.0,61.9) survival; Bot5 minecraft:mcc_lobby (-6.5,71.0,89.3) survival; Bot6 minecraft:mcc_lobby (6.9,71.0,105.4) survival

**WARN** [bot-movement(benign)] x2: Bot5 moved too quickly! -25.085026708594455,3.0,-5.920300401156178

```
Bot5 moved too quickly! -25.085026708594455,3.0,-5.920300401156178

```

### pass1: gridrunners (Grid Runners) - PASS

- positions lobby: Bot1 minecraft:mcc_lobby (31.5,71.0,78.0) survival; Bot2 minecraft:mcc_lobby (-19.9,71.0,83.3) survival; Bot3 minecraft:mcc_lobby (26.4,71.0,100.9) survival; Bot4 minecraft:mcc_lobby (-10.8,71.0,58.1) survival; Bot5 minecraft:mcc_lobby (-12.8,71.0,96.1) survival; Bot6 minecraft:mcc_lobby (10.1,71.4,107.2) survival
- positions t10: Bot1 minecraft:mcc_gridrunners (78.7,65.0,5.6) adventure; Bot2 minecraft:mcc_gridrunners (159.7,65.0,3.3) adventure; Bot3 minecraft:mcc_gridrunners (-0.2,65.0,7.5) adventure; Bot4 minecraft:mcc_gridrunners (239.1,65.0,11.9) adventure; Bot5 minecraft:mcc_gridrunners (330.5,65.0,14.3) adventure; Bot6 minecraft:mcc_gridrunners (401.5,65.0,8.7) adventure
- positions t60: Bot1 minecraft:mcc_gridrunners (94.7,66.0,14.7) adventure; Bot2 minecraft:mcc_gridrunners (166.8,65.0,3.5) adventure; Bot3 minecraft:mcc_gridrunners (14.7,65.0,4.6) adventure; Bot4 minecraft:mcc_gridrunners (254.7,66.0,3.5) adventure; Bot5 minecraft:mcc_gridrunners (324.3,66.2,14.7) adventure; Bot6 minecraft:mcc_gridrunners (403.6,65.8,14.7) adventure
- positions after: Bot1 minecraft:mcc_lobby (24.7,71.0,101.3) survival; Bot2 minecraft:mcc_lobby (32.0,71.0,86.2) survival; Bot3 minecraft:mcc_lobby (4.3,71.0,63.3) survival; Bot4 minecraft:mcc_lobby (7.2,71.8,78.6) survival; Bot5 minecraft:mcc_lobby (-14.6,71.0,63.2) survival; Bot6 minecraft:mcc_lobby (-8.8,71.0,59.0) survival

**WARN** [bot-movement(benign)] x4: Bot4 moved wrongly!

```
Bot4 moved wrongly!

```

### pass1: bingo (Bingo but Fast) - PASS

- positions lobby: Bot1 minecraft:mcc_lobby (24.7,71.0,101.3) survival; Bot2 minecraft:mcc_lobby (33.0,71.0,84.3) survival; Bot3 minecraft:mcc_lobby (-4.1,71.0,59.1) survival; Bot4 minecraft:mcc_lobby (7.1,71.0,78.5) survival; Bot5 minecraft:mcc_lobby (-16.1,71.0,54.2) survival; Bot6 minecraft:mcc_lobby (-5.8,71.8,49.5) survival
- positions t10: Bot1 minecraft:mcc_bingo (-340.7,68.0,-184.6) survival; Bot2 minecraft:mcc_bingo (-344.5,65.0,-171.2) survival; Bot3 minecraft:mcc_bingo (-361.0,65.0,-174.8) survival; Bot4 minecraft:mcc_bingo (-365.7,69.0,-185.3) survival; Bot5 minecraft:mcc_bingo (-360.5,70.0,-207.5) survival; Bot6 minecraft:mcc_bingo (-346.0,70.0,-198.6) survival
- positions t60: Bot1 minecraft:mcc_bingo (-352.4,71.0,-259.2) survival; Bot2 minecraft:mcc_bingo (-310.9,56.4,-158.4) survival; Bot3 minecraft:mcc_bingo (-326.7,63.8,-177.3) survival; Bot4 minecraft:mcc_bingo (-326.7,62.0,-170.7) survival; Bot5 minecraft:mcc_bingo (-349.6,70.0,-231.9) survival; Bot6 minecraft:mcc_bingo (-318.5,71.0,-213.6) survival
- positions after: Bot1 minecraft:mcc_lobby (-3.6,71.0,104.9) survival; Bot2 minecraft:mcc_lobby (15.0,71.0,70.8) survival; Bot3 minecraft:mcc_lobby (29.5,71.0,102.9) survival; Bot4 minecraft:mcc_lobby (-10.1,71.0,89.2) survival; Bot5 minecraft:mcc_lobby (9.7,71.5,117.4) survival; Bot6 minecraft:mcc_lobby (16.7,71.0,77.8) survival

**WARN** [bot-movement(benign)] x1: Bot5 moved wrongly!

```
Bot5 moved wrongly!

```

### pass2: skyblockle (Sky Blockle) - PASS

- positions lobby: Bot1 minecraft:mcc_lobby (-14.6,72.0,95.5) survival; Bot2 minecraft:mcc_lobby (15.0,71.0,70.8) survival; Bot3 minecraft:mcc_lobby (30.7,72.3,103.7) survival; Bot4 minecraft:mcc_lobby (-17.8,71.0,95.0) survival; Bot5 minecraft:mcc_lobby (9.7,71.5,117.7) survival; Bot6 minecraft:mcc_lobby (24.6,71.0,65.6) survival
- positions t10: Bot1 minecraft:mcc_skyblockle (0.5,101.0,138.5) survival; Bot2 minecraft:mcc_skyblockle (-130.5,101.0,43.5) survival; Bot3 minecraft:mcc_skyblockle (-131.5,101.0,44.5) survival; Bot4 minecraft:mcc_skyblockle (-81.5,101.0,-110.5) survival; Bot5 minecraft:mcc_skyblockle (82.5,101.0,-110.5) survival; Bot6 minecraft:mcc_skyblockle (131.5,101.0,43.5) survival
- positions t60: Bot1 minecraft:mcc_skyblockle (-2.7,101.0,140.8) survival; Bot2 minecraft:mcc_skyblockle (-132.6,101.0,41.2) survival; Bot3 minecraft:mcc_skyblockle (3.7,101.0,12.7) spectator; Bot4 minecraft:mcc_skyblockle (-80.3,101.0,-114.0) survival; Bot5 minecraft:mcc_skyblockle (-3.5,101.0,-5.5) spectator; Bot6 minecraft:mcc_skyblockle (8.7,101.0,9.7) spectator
- positions after: Bot1 minecraft:mcc_lobby (20.5,71.0,90.7) survival; Bot2 minecraft:mcc_lobby (-15.7,71.0,96.8) survival; Bot3 minecraft:mcc_lobby (31.5,71.0,63.7) survival; Bot4 minecraft:mcc_lobby (20.9,71.0,79.3) survival; Bot5 minecraft:mcc_lobby (-14.4,71.0,78.8) survival; Bot6 minecraft:mcc_lobby (-8.8,71.0,93.3) survival

**WARN** [bot-movement(benign)] x75: Bot5 moved too quickly! -80.71584522643404,71.85558683112157,108.1004383995092

```
Bot5 moved too quickly! -80.71584522643404,71.85558683112157,108.1004383995092

```

### pass2: sandsoftime (Sands of Time) - PASS

- positions lobby: Bot1 minecraft:mcc_lobby (18.2,71.0,102.4) survival; Bot2 minecraft:mcc_lobby (-15.7,71.0,96.8) survival; Bot3 minecraft:mcc_lobby (32.2,72.2,63.9) survival; Bot4 minecraft:mcc_lobby (29.5,71.0,77.5) survival; Bot5 minecraft:mcc_lobby (-9.5,71.0,69.2) survival; Bot6 minecraft:mcc_lobby (-6.1,71.0,81.7) survival
- positions t10: Bot1 minecraft:mcc_sandsoftime (10.4,65.0,18.7) survival; Bot2 minecraft:mcc_sandsoftime (135.7,65.0,11.7) survival; Bot3 minecraft:mcc_sandsoftime (124.1,65.0,7.6) survival; Bot4 minecraft:mcc_sandsoftime (244.4,65.0,9.8) survival; Bot5 minecraft:mcc_sandsoftime (325.3,65.0,1.3) survival; Bot6 minecraft:mcc_sandsoftime (413.3,65.0,11.7) survival
- positions t60: Bot1 minecraft:mcc_sandsoftime (27.7,65.0,-0.3) adventure; Bot2 minecraft:mcc_sandsoftime (135.7,65.0,47.7) survival; Bot3 minecraft:mcc_sandsoftime (125.3,65.0,-0.3) adventure; Bot4 minecraft:mcc_sandsoftime (249.3,65.4,16.3) survival; Bot5 minecraft:mcc_sandsoftime (325.3,65.0,35.7) survival; Bot6 minecraft:mcc_sandsoftime (411.7,65.0,11.2) survival
- positions after: Bot1 minecraft:mcc_lobby (18.3,71.0,108.7) survival; Bot2 minecraft:mcc_lobby (27.0,71.0,100.4) survival; Bot3 minecraft:mcc_lobby (-30.7,71.0,87.1) survival; Bot4 minecraft:mcc_lobby (18.6,71.4,80.5) survival; Bot5 minecraft:mcc_lobby (5.3,71.0,87.6) survival; Bot6 minecraft:mcc_lobby (3.3,71.0,66.8) survival

**WARN** [bot-movement(benign)] x5: Bot2 moved too quickly! 0.016157851869706974,1.0,-54.33673097536065

```
Bot2 moved too quickly! 0.016157851869706974,1.0,-54.33673097536065

```

### pass2: gridrunners (Grid Runners) - PASS

- positions lobby: Bot1 minecraft:mcc_lobby (15.7,71.5,114.8) survival; Bot2 minecraft:mcc_lobby (30.7,71.5,103.7) survival; Bot3 minecraft:mcc_lobby (-30.7,71.0,80.8) survival; Bot4 minecraft:mcc_lobby (18.6,72.0,80.5) survival; Bot5 minecraft:mcc_lobby (-4.0,71.0,81.1) survival; Bot6 minecraft:mcc_lobby (3.3,72.2,66.1) survival
- positions t10: Bot1 minecraft:mcc_gridrunners (79.2,65.0,12.7) adventure; Bot2 minecraft:mcc_gridrunners (159.6,66.3,12.7) adventure; Bot3 minecraft:mcc_gridrunners (172.9,65.5,12.9) adventure; Bot4 minecraft:mcc_gridrunners (9.3,65.0,11.9) adventure; Bot5 minecraft:mcc_gridrunners (236.8,65.0,12.7) adventure; Bot6 minecraft:mcc_gridrunners (319.3,66.2,7.8) adventure
- positions t60: Bot1 minecraft:mcc_gridrunners (78.7,65.0,3.3) adventure; Bot2 minecraft:mcc_gridrunners (159.7,65.0,5.3) adventure; Bot3 minecraft:mcc_gridrunners (174.7,65.0,2.3) adventure; Bot4 minecraft:mcc_gridrunners (13.6,65.0,14.7) adventure; Bot5 minecraft:mcc_gridrunners (254.7,65.0,6.2) adventure; Bot6 minecraft:mcc_gridrunners (325.7,66.2,14.7) adventure
- positions after: Bot1 minecraft:mcc_lobby (30.7,71.0,70.6) survival; Bot2 minecraft:mcc_lobby (-12.5,71.8,75.1) survival; Bot3 minecraft:mcc_lobby (3.0,71.0,101.9) survival; Bot4 minecraft:mcc_lobby (-14.6,71.0,61.7) survival; Bot5 minecraft:mcc_lobby (27.3,71.0,89.0) survival; Bot6 minecraft:mcc_lobby (5.4,71.0,72.3) survival

**WARN** [bot-movement(benign)] x1: Bot1 moved wrongly!

```
Bot1 moved wrongly!

```

### pass2: bingo (Bingo but Fast) - PASS

- positions lobby: Bot1 minecraft:mcc_lobby (30.7,71.0,70.6) survival; Bot2 minecraft:mcc_lobby (-2.4,71.0,71.0) survival; Bot3 minecraft:mcc_lobby (13.5,71.0,92.6) survival; Bot4 minecraft:mcc_lobby (-14.7,71.0,60.6) survival; Bot5 minecraft:mcc_lobby (34.0,71.5,97.4) survival; Bot6 minecraft:mcc_lobby (5.9,72.3,72.0) survival
- positions t10: Bot1 minecraft:mcc_bingo (3757.3,67.0,-192.2) survival; Bot2 minecraft:mcc_bingo (3749.5,63.0,-175.5) survival; Bot3 minecraft:mcc_bingo (3747.4,62.2,-161.7) survival; Bot4 minecraft:mcc_bingo (3737.7,62.0,-181.6) survival; Bot5 minecraft:mcc_bingo (3737.1,71.0,-195.4) survival; Bot6 minecraft:mcc_bingo (3747.8,62.0,-195.5) survival
- positions t60: Bot1 minecraft:mcc_bingo (3744.3,62.0,-198.3) survival; Bot2 minecraft:mcc_bingo (3747.4,62.3,-162.6) survival; Bot3 minecraft:mcc_bingo (3708.1,62.0,-156.3) survival; Bot4 minecraft:mcc_bingo (3797.7,62.0,-173.7) survival; Bot5 minecraft:mcc_bingo (3735.3,72.0,-207.7) survival; Bot6 minecraft:mcc_bingo (3748.7,62.0,-197.7) survival
- positions after: Bot1 minecraft:mcc_lobby (-12.6,71.0,100.6) survival; Bot2 minecraft:mcc_lobby (7.9,71.0,80.6) survival; Bot3 minecraft:mcc_lobby (-2.2,72.0,90.9) survival; Bot4 minecraft:mcc_lobby (1.8,71.0,73.2) survival; Bot5 minecraft:mcc_lobby (37.7,71.5,71.3) survival; Bot6 minecraft:mcc_lobby (37.4,71.0,77.7) survival


## Boot / other issues


**WARN** [server] x1: Can't keep up! Is the server overloaded? Running 4217ms or 84 ticks behind

```
Can't keep up! Is the server overloaded? Running 4217ms or 84 ticks behind

```

## Bot kicks / errors

None.
