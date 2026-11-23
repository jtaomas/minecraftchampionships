# MCM staging bot test - run 2026-11-23T17-59-25

Raw logs: `C:\Users\USER\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\bot-harness\logs\2026-11-23T17-59-25`
Bots: 6, pass1 watch 90s then /mcm skip; pass2 natural end (timeout 13 min).

- Jars copied: McmBattleBox.jar, McmBingo.jar, McmDecisionDome.jar, McmDodgebolt.jar, McmFindTheButton.jar, McmFootRace.jar, McmGridRunners.jar, McmHoleInTheWall.jar, McmParkourTag.jar, McmParkourWarrior.jar, McmRocketSpleef.jar, McmSandsOfTime.jar, McmSkyBlockle.jar, McmTgttos.jar, MinecraftMondayCore.jar (core)

## Pass 1 (all games: play, watch, skip)

| id | name | result | started | ended | duration | bots in arena @10s / @60s | back in lobby | issues | bot kicks/errors |
|---|---|---|---|---|---|---|---|---|---|
| findthebutton | Find the Button | **FAIL** | yes | - (skip) | -s | 6/6 | true | WARN bot-movement(benign) x202 | 0 |
| tgttos | To Get To The Other Side | **FAIL** | yes | - (skip) | -s | 6/6 | true | WARN McmTgttos x1 | 0 |
| skyblockle | Sky Blockle | **PASS** | yes | Last side standing (natural) | 84s | 6/6 | true | WARN server x1; WARN bot-movement(benign) x262 | 0 |
| parkourwarrior | Parkour Warrior | **FAIL** | yes | - (skip) | -s | 6/6 | true | - | 0 |
| battlebox | Battle Box | **FAIL** | yes | - (skip) | -s | 6/6 | true | WARN McmBattleBox x1; WARN bot-movement(benign) x264 | 0 |

## Pass 2 (MCC games to natural end)

| id | name | result | end reason | ended by | duration | bots in arena @10s/@60s | back in lobby | issues |
|---|---|---|---|---|---|---|---|---|
| findthebutton | Find the Button | **PASS** | Find the Button finished | natural | 511s | 6/6 | true | WARN bot-movement(benign) x1 |
| tgttos | To Get To The Other Side | **PASS** | TGTTOS finished | natural | 734s | 6/6 | true | WARN McmTgttos x1; WARN McmTgttos x1; WARN McmTgttos x1; WARN McmTgttos x1; WARN McmTgttos x1; WARN McmTgttos x1 |
| skyblockle | Sky Blockle | **PASS** | Last side standing | natural | 112s | 6/6 | true | WARN bot-movement(benign) x204 |
| parkourwarrior | Parkour Warrior | **PASS** | Time's up! | natural | 646s | 6/6 | true | WARN bot-movement(benign) x2 |
| battlebox | Battle Box | **PASS** | Battle Box complete | natural | 415s | 6/6 | true | WARN bot-movement(benign) x3 |

## Server lag ("Can't keep up")

- 04:59:57 boot: 3807 ms (76 ticks) **>2s**
- 05:05:04 pass1:skyblockle: 2613 ms (52 ticks) **>2s**

## Per-game details

### pass1: findthebutton (Find the Button) - FAIL

- No OVER line within 20s of /mcm skip; sending /mcm stop
- positions lobby: Bot1 minecraft:overworld (-2.7,155.0,5.0) survival; Bot2 minecraft:overworld (-3.3,150.0,11.9) survival; Bot3 minecraft:overworld (-1.3,156.0,2.3) survival; Bot4 minecraft:overworld (6.4,154.0,5.3) survival; Bot5 minecraft:overworld (-0.0,157.0,-2.7) survival; Bot6 minecraft:overworld (0.4,143.0,18.8) survival
- positions t10: Bot1 minecraft:mcc_findthebutton (140.5,65.0,19.5) adventure; Bot2 minecraft:mcc_findthebutton (136.5,65.0,17.7) adventure; Bot3 minecraft:mcc_findthebutton (141.3,65.0,19.1) adventure; Bot4 minecraft:mcc_findthebutton (142.8,65.0,7.3) adventure; Bot5 minecraft:mcc_findthebutton (146.7,65.0,12.3) adventure; Bot6 minecraft:mcc_findthebutton (143.1,65.0,11.7) adventure
- positions t60: Bot1 minecraft:mcc_findthebutton (138.1,65.0,20.1) adventure; Bot2 minecraft:mcc_findthebutton (136.3,65.0,20.7) adventure; Bot3 minecraft:mcc_findthebutton (139.0,65.0,10.3) adventure; Bot4 minecraft:mcc_findthebutton (138.1,66.0,5.0) adventure; Bot5 minecraft:mcc_findthebutton (138.2,65.0,14.7) adventure; Bot6 minecraft:mcc_findthebutton (148.7,66.3,16.6) adventure
- positions after: Bot1 minecraft:overworld (-5.7,144.0,23.7) survival; Bot2 minecraft:overworld (10.3,141.0,4.8) survival; Bot3 minecraft:overworld (7.3,148.0,9.9) survival; Bot4 minecraft:overworld (16.5,141.4,4.4) survival; Bot5 minecraft:overworld (4.7,157.4,0.3) survival; Bot6 minecraft:overworld (-2.7,157.0,-4.7) survival

**WARN** [bot-movement(benign)] x202: Bot1 moved too quickly! 116.91913268843004,0.0,0.49293426995248524

```
Bot1 moved too quickly! 116.91913268843004,0.0,0.49293426995248524

```

### pass1: tgttos (To Get To The Other Side) - FAIL

- No OVER line within 20s of /mcm skip; sending /mcm stop
- positions lobby: Bot1 minecraft:overworld (-5.7,145.0,24.0) survival; Bot2 minecraft:overworld (12.7,140.0,8.7) survival; Bot3 minecraft:overworld (7.3,148.0,9.9) survival; Bot4 minecraft:overworld (25.9,140.4,2.6) survival; Bot5 minecraft:overworld (-0.7,157.4,0.7) survival; Bot6 minecraft:overworld (-1.9,157.8,-4.7) survival
- positions t10: Bot1 minecraft:mcc_tgttos (3600.3,65.0,6.7) adventure; Bot2 minecraft:mcc_tgttos (3604.3,65.0,6.7) adventure; Bot3 minecraft:mcc_tgttos (3611.7,65.0,6.7) adventure; Bot4 minecraft:mcc_tgttos (3582.3,65.0,0.3) adventure; Bot5 minecraft:mcc_tgttos (3582.3,65.0,5.4) adventure; Bot6 minecraft:mcc_tgttos (3614.0,65.0,6.7) adventure
- positions t60: Bot1 minecraft:mcc_tgttos (3621.1,65.0,13.3) adventure; Bot2 minecraft:mcc_tgttos (3614.4,65.0,24.3) adventure; Bot3 minecraft:mcc_tgttos (3582.3,65.8,6.3) adventure; Bot4 minecraft:mcc_tgttos (3608.9,65.0,25.5) adventure; Bot5 minecraft:mcc_tgttos (3579.8,66.0,21.7) adventure; Bot6 minecraft:mcc_tgttos (3592.3,65.0,-1.3) adventure
- positions after: Bot1 minecraft:mcc_lobby (32.3,71.0,68.8) survival; Bot2 minecraft:mcc_lobby (35.4,71.0,108.9) survival; Bot3 minecraft:mcc_lobby (13.8,71.0,71.4) survival; Bot4 minecraft:mcc_lobby (-25.7,71.5,53.3) survival; Bot5 minecraft:mcc_lobby (26.6,72.7,54.2) survival; Bot6 minecraft:mcc_lobby (3.2,71.0,58.6) survival

**WARN** [McmTgttos] x1: [McmTgttos] TGTTOS: bad block state 'minecraft:polished_deepslate_brick_stairs[facing=south,half=bottom,shape=straight]', using minecraft:stone

```
[McmTgttos] TGTTOS: bad block state 'minecraft:polished_deepslate_brick_stairs[facing=south,half=bottom,shape=straight]', using minecraft:stone

```

### pass1: skyblockle (Sky Blockle) - PASS

- positions lobby: Bot1 minecraft:mcc_lobby (34.7,72.8,63.8) survival; Bot2 minecraft:mcc_lobby (37.1,70.0,106.6) survival; Bot3 minecraft:mcc_lobby (13.8,71.0,71.4) survival; Bot4 minecraft:mcc_lobby (-27.1,71.5,55.7) survival; Bot5 minecraft:mcc_lobby (25.7,71.5,52.3) survival; Bot6 minecraft:mcc_lobby (3.2,71.0,58.6) survival
- positions t10: Bot1 minecraft:mcc_skyblockle (0.5,101.0,138.5) survival; Bot2 minecraft:mcc_skyblockle (-118.5,101.0,70.5) survival; Bot3 minecraft:mcc_skyblockle (-118.5,101.0,-68.5) survival; Bot4 minecraft:mcc_skyblockle (0.5,101.0,-137.5) survival; Bot5 minecraft:mcc_skyblockle (119.5,101.0,-69.5) survival; Bot6 minecraft:mcc_skyblockle (119.5,101.0,70.5) survival
- positions t60: Bot1 minecraft:mcc_skyblockle (7.7,106.0,-15.7) spectator; Bot2 minecraft:mcc_skyblockle (-117.0,101.0,71.8) survival; Bot3 minecraft:mcc_skyblockle (-24.4,47.0,16.3) spectator; Bot4 minecraft:mcc_skyblockle (2.8,101.8,-137.7) survival; Bot5 minecraft:mcc_skyblockle (-9.7,101.0,-6.7) spectator; Bot6 minecraft:mcc_skyblockle (-2.9,119.0,22.7) spectator
- positions after: Bot1 minecraft:mcc_lobby (-1.4,71.4,105.4) survival; Bot2 minecraft:mcc_lobby (-27.4,70.8,113.6) survival; Bot3 minecraft:mcc_lobby (23.1,71.0,106.7) survival; Bot4 minecraft:mcc_lobby (-28.7,71.5,104.3) survival; Bot5 minecraft:mcc_lobby (-16.2,72.2,64.6) survival; Bot6 minecraft:mcc_lobby (0.5,71.0,108.5) survival

**WARN** [server] x1: Can't keep up! Is the server overloaded? Running 2613ms or 52 ticks behind

```
Can't keep up! Is the server overloaded? Running 2613ms or 52 ticks behind

```

**WARN** [bot-movement(benign)] x262: Bot1 moved too quickly! -2.571456489157979,72.58926478336295,-141.73700343570326

```
Bot1 moved too quickly! -2.571456489157979,72.58926478336295,-141.73700343570326

```

### pass1: parkourwarrior (Parkour Warrior) - FAIL

- No OVER line within 20s of /mcm skip; sending /mcm stop
- positions lobby: Bot1 minecraft:mcc_lobby (-1.4,72.2,105.4) survival; Bot2 minecraft:mcc_lobby (-24.1,70.0,121.2) survival; Bot3 minecraft:mcc_lobby (29.7,71.0,105.7) survival; Bot4 minecraft:mcc_lobby (-28.7,71.5,104.7) survival; Bot5 minecraft:mcc_lobby (-20.0,71.0,60.9) survival; Bot6 minecraft:mcc_lobby (3.4,71.0,105.9) survival
- positions t10: Bot1 minecraft:mcc_parkourwarrior (-5.5,101.0,-1.5) adventure; Bot2 minecraft:mcc_parkourwarrior (-4.1,101.0,-2.5) adventure; Bot3 minecraft:mcc_parkourwarrior (-5.5,101.4,2.5) adventure; Bot4 minecraft:mcc_parkourwarrior (-7.7,101.0,-5.3) adventure; Bot5 minecraft:mcc_parkourwarrior (-0.0,101.0,2.3) adventure; Bot6 minecraft:mcc_parkourwarrior (-5.5,102.3,2.0) adventure
- positions t60: Bot1 minecraft:mcc_parkourwarrior (-4.7,101.0,8.9) adventure; Bot2 minecraft:mcc_parkourwarrior (2.3,101.0,1.1) adventure; Bot3 minecraft:mcc_parkourwarrior (-8.9,101.0,5.1) adventure; Bot4 minecraft:mcc_parkourwarrior (-4.9,101.4,-8.8) adventure; Bot5 minecraft:mcc_parkourwarrior (5.6,101.0,-7.8) adventure; Bot6 minecraft:mcc_parkourwarrior (6.3,101.0,8.9) adventure
- positions after: Bot1 minecraft:mcc_lobby (-24.7,72.3,52.3) survival; Bot2 minecraft:mcc_lobby (0.4,74.0,-0.9) survival; Bot3 minecraft:mcc_lobby (-53.7,71.0,111.7) survival; Bot4 minecraft:mcc_lobby (-12.4,72.2,87.1) survival; Bot5 minecraft:mcc_lobby (8.2,71.0,67.8) survival; Bot6 minecraft:mcc_lobby (-34.7,70.4,114.7) survival

### pass1: battlebox (Battle Box) - FAIL

- No OVER line within 20s of /mcm skip; sending /mcm stop
- positions lobby: Bot1 minecraft:mcc_lobby (-24.7,71.5,52.3) survival; Bot2 minecraft:mcc_lobby (4.8,77.0,-9.1) survival; Bot3 minecraft:mcc_lobby (-55.0,71.0,109.7) survival; Bot4 minecraft:mcc_lobby (-14.9,71.0,72.6) survival; Bot5 minecraft:mcc_lobby (8.9,71.0,78.4) survival; Bot6 minecraft:mcc_lobby (-40.4,74.0,115.5) survival
- positions t10: Bot1 minecraft:mcc_battlebox (0.5,65.0,19.3) adventure; Bot2 minecraft:mcc_battlebox (-2.4,70.0,-11.3) spectator; Bot3 minecraft:mcc_battlebox (-2.2,70.0,-11.1) spectator; Bot4 minecraft:mcc_battlebox (-8.8,65.0,-6.1) spectator; Bot5 minecraft:mcc_battlebox (0.5,65.0,-20.5) adventure; Bot6 minecraft:mcc_battlebox (5.0,65.0,-4.3) spectator
- positions t60: Bot1 minecraft:mcc_battlebox (17.7,65.0,7.3) survival; Bot2 minecraft:mcc_battlebox (17.7,65.0,-1.4) spectator; Bot3 minecraft:mcc_battlebox (17.7,65.0,-16.7) spectator; Bot4 minecraft:mcc_battlebox (-16.7,65.0,-11.6) spectator; Bot5 minecraft:mcc_battlebox (17.7,65.5,-15.7) survival; Bot6 minecraft:mcc_battlebox (12.7,65.0,11.7) spectator
- positions after: Bot1 minecraft:mcc_lobby (-11.8,71.0,82.7) survival; Bot2 minecraft:mcc_lobby (-31.3,72.7,60.3) survival; Bot3 minecraft:mcc_lobby (4.1,71.0,112.8) survival; Bot4 minecraft:mcc_lobby (-4.7,71.0,70.1) survival; Bot5 minecraft:mcc_lobby (-29.9,71.5,102.1) survival; Bot6 minecraft:mcc_lobby (-3.7,71.0,83.5) survival

**WARN** [McmBattleBox] x1: [McmBattleBox] Battle Box: bad block state 'minecraft:polished_andesite_wall[north=none,east=none,south=none,west=none,up=true]', using minecraft:stone

```
[McmBattleBox] Battle Box: bad block state 'minecraft:polished_andesite_wall[north=none,east=none,south=none,west=none,up=true]', using minecraft:stone

```

**WARN** [bot-movement(benign)] x264: Bot1 moved too quickly! -217.2,46.0,-2.2

```
Bot1 moved too quickly! -217.2,46.0,-2.2

```

### pass2: findthebutton (Find the Button) - PASS

- positions lobby: Bot1 minecraft:mcc_lobby (-19.4,71.0,71.2) survival; Bot2 minecraft:mcc_lobby (-31.7,72.3,60.3) survival; Bot3 minecraft:mcc_lobby (9.7,71.5,117.7) survival; Bot4 minecraft:mcc_lobby (3.4,71.0,58.3) survival; Bot5 minecraft:mcc_lobby (-17.0,71.0,106.3) survival; Bot6 minecraft:mcc_lobby (-4.3,71.5,82.5) survival
- positions t10: Bot1 minecraft:mcc_findthebutton (84.7,65.0,14.9) adventure; Bot2 minecraft:mcc_findthebutton (78.7,65.0,10.6) adventure; Bot3 minecraft:mcc_findthebutton (68.9,65.4,8.8) adventure; Bot4 minecraft:mcc_findthebutton (76.5,65.0,17.9) adventure; Bot5 minecraft:mcc_findthebutton (77.3,66.0,2.5) adventure; Bot6 minecraft:mcc_findthebutton (84.7,65.0,13.2) adventure
- positions t60: Bot1 minecraft:mcc_findthebutton (77.2,65.1,13.7) adventure; Bot2 minecraft:mcc_findthebutton (71.4,65.0,2.3) adventure; Bot3 minecraft:mcc_findthebutton (74.0,66.0,1.3) adventure; Bot4 minecraft:mcc_findthebutton (79.3,65.0,4.2) adventure; Bot5 minecraft:mcc_findthebutton (84.7,65.0,17.7) adventure; Bot6 minecraft:mcc_findthebutton (73.1,65.0,2.3) adventure
- positions after: Bot1 minecraft:overworld (-14.7,144.0,26.2) survival; Bot2 minecraft:overworld (3.7,152.0,11.3) survival; Bot3 minecraft:overworld (-1.2,157.0,1.9) survival; Bot4 minecraft:overworld (4.1,157.0,0.4) survival; Bot5 minecraft:overworld (1.7,157.0,-3.7) survival; Bot6 minecraft:overworld (4.7,157.0,0.3) survival

**WARN** [bot-movement(benign)] x1: Bot6 moved wrongly!

```
Bot6 moved wrongly!

```

### pass2: tgttos (To Get To The Other Side) - PASS

- positions lobby: Bot1 minecraft:overworld (-14.7,144.4,27.3) survival; Bot2 minecraft:overworld (4.1,153.0,9.7) survival; Bot3 minecraft:overworld (-1.2,157.8,1.9) survival; Bot4 minecraft:overworld (4.7,157.0,0.3) survival; Bot5 minecraft:overworld (1.3,157.0,-3.7) survival; Bot6 minecraft:overworld (4.7,157.0,0.3) survival
- positions t10: Bot1 minecraft:mcc_tgttos (607.0,65.0,0.3) survival; Bot2 minecraft:mcc_tgttos (603.1,65.0,6.7) survival; Bot3 minecraft:mcc_tgttos (617.7,65.0,6.7) survival; Bot4 minecraft:mcc_tgttos (582.5,65.0,5.5) survival; Bot5 minecraft:mcc_tgttos (616.2,65.0,6.7) survival; Bot6 minecraft:mcc_tgttos (610.6,65.0,6.6) survival
- positions t60: Bot1 minecraft:mcc_tgttos (616.6,66.2,0.3) survival; Bot2 minecraft:mcc_tgttos (584.7,62.0,23.7) survival; Bot3 minecraft:mcc_tgttos (612.9,65.0,15.6) survival; Bot4 minecraft:mcc_tgttos (607.8,65.0,8.6) survival; Bot5 minecraft:mcc_tgttos (590.6,65.0,8.2) survival; Bot6 minecraft:mcc_tgttos (609.5,65.0,6.8) survival
- positions after: Bot1 minecraft:mcc_lobby (15.0,71.0,87.2) survival; Bot2 minecraft:mcc_lobby (34.7,71.0,78.4) survival; Bot3 minecraft:mcc_lobby (-30.7,71.0,72.4) survival; Bot4 minecraft:mcc_lobby (12.7,72.3,116.7) survival; Bot5 minecraft:mcc_lobby (-4.3,71.0,62.4) survival; Bot6 minecraft:mcc_lobby (-32.3,71.5,99.5) survival

**WARN** [McmTgttos] x1: [McmTgttos] TGTTOS: bad block state 'minecraft:smooth_stone_stairs[facing=south,half=bottom,shape=straight]', using minecraft:stone

```
[McmTgttos] TGTTOS: bad block state 'minecraft:smooth_stone_stairs[facing=south,half=bottom,shape=straight]', using minecraft:stone

```

**WARN** [McmTgttos] x1: [McmTgttos] TGTTOS: bad block state 'minecraft:polished_andesite_wall[north=none,east=low,south=none,west=none,up=true,waterlogged=false]', using minecraft:stone

```
[McmTgttos] TGTTOS: bad block state 'minecraft:polished_andesite_wall[north=none,east=low,south=none,west=none,up=true,waterlogged=false]', using minecraft:stone

```

**WARN** [McmTgttos] x1: [McmTgttos] TGTTOS: bad block state 'minecraft:polished_andesite_wall[north=none,east=low,south=none,west=low,up=false,waterlogged=false]', using minecraft:stone

```
[McmTgttos] TGTTOS: bad block state 'minecraft:polished_andesite_wall[north=none,east=low,south=none,west=low,up=false,waterlogged=false]', using minecraft:stone

```

**WARN** [McmTgttos] x1: [McmTgttos] TGTTOS: bad block state 'minecraft:polished_andesite_wall[north=none,east=none,south=none,west=none,up=true,waterlogged=false]', using minecraft:stone

```
[McmTgttos] TGTTOS: bad block state 'minecraft:polished_andesite_wall[north=none,east=none,south=none,west=none,up=true,waterlogged=false]', using minecraft:stone

```

**WARN** [McmTgttos] x1: [McmTgttos] TGTTOS: bad block state 'minecraft:polished_andesite_wall[north=none,east=none,south=none,west=low,up=true,waterlogged=false]', using minecraft:stone

```
[McmTgttos] TGTTOS: bad block state 'minecraft:polished_andesite_wall[north=none,east=none,south=none,west=low,up=true,waterlogged=false]', using minecraft:stone

```

**WARN** [McmTgttos] x1: [McmTgttos] TGTTOS: bad block state 'minecraft:string', using minecraft:stone

```
[McmTgttos] TGTTOS: bad block state 'minecraft:string', using minecraft:stone

```

### pass2: skyblockle (Sky Blockle) - PASS

- positions lobby: Bot1 minecraft:mcc_lobby (15.0,71.0,87.2) survival; Bot2 minecraft:mcc_lobby (34.7,71.8,78.4) survival; Bot3 minecraft:mcc_lobby (-30.7,71.0,83.4) survival; Bot4 minecraft:mcc_lobby (12.7,71.5,116.7) survival; Bot5 minecraft:mcc_lobby (-9.3,71.0,52.9) survival; Bot6 minecraft:mcc_lobby (-32.3,71.5,99.5) survival
- positions t10: Bot1 minecraft:mcc_skyblockle (0.5,101.0,138.5) survival; Bot2 minecraft:mcc_skyblockle (-118.5,101.0,70.5) survival; Bot3 minecraft:mcc_skyblockle (-118.5,101.0,-68.5) survival; Bot4 minecraft:mcc_skyblockle (0.4,101.0,-137.5) survival; Bot5 minecraft:mcc_skyblockle (119.5,101.0,-69.5) survival; Bot6 minecraft:mcc_skyblockle (119.5,101.0,70.5) survival
- positions t60: Bot1 minecraft:mcc_skyblockle (3.0,101.0,138.2) survival; Bot2 minecraft:mcc_skyblockle (-122.1,101.0,69.3) survival; Bot3 minecraft:mcc_skyblockle (-11.7,103.0,8.2) spectator; Bot4 minecraft:mcc_skyblockle (3.7,101.0,-138.0) survival; Bot5 minecraft:mcc_skyblockle (122.5,101.0,-68.3) survival; Bot6 minecraft:mcc_skyblockle (-2.8,103.0,13.7) spectator
- positions after: Bot1 minecraft:mcc_lobby (-4.9,71.0,68.4) survival; Bot2 minecraft:mcc_lobby (14.1,72.3,85.2) survival; Bot3 minecraft:mcc_lobby (12.6,71.0,90.9) survival; Bot4 minecraft:mcc_lobby (8.5,71.0,91.2) survival; Bot5 minecraft:mcc_lobby (-10.5,70.0,127.6) survival; Bot6 minecraft:mcc_lobby (-3.2,71.0,66.0) survival

**WARN** [bot-movement(benign)] x204: Bot3 moved too quickly! 120.06389688421811,74.68868037874728,64.59982129001759

```
Bot3 moved too quickly! 120.06389688421811,74.68868037874728,64.59982129001759

```

### pass2: parkourwarrior (Parkour Warrior) - PASS

- positions lobby: Bot1 minecraft:mcc_lobby (-3.1,71.0,59.0) survival; Bot2 minecraft:mcc_lobby (12.9,71.8,71.4) survival; Bot3 minecraft:mcc_lobby (17.2,71.0,81.8) survival; Bot4 minecraft:mcc_lobby (3.0,71.0,82.3) survival; Bot5 minecraft:mcc_lobby (-12.9,68.2,137.8) survival; Bot6 minecraft:mcc_lobby (-9.0,71.1,58.2) survival
- positions t10: Bot1 minecraft:mcc_parkourwarrior (3.2,101.0,9.7) adventure; Bot2 minecraft:mcc_parkourwarrior (-5.2,101.0,6.7) adventure; Bot3 minecraft:mcc_parkourwarrior (-1.3,101.0,2.0) adventure; Bot4 minecraft:mcc_parkourwarrior (0.1,101.0,9.8) adventure; Bot5 minecraft:mcc_parkourwarrior (-3.6,101.0,5.5) adventure; Bot6 minecraft:mcc_parkourwarrior (-8.0,101.0,5.7) adventure
- positions t60: Bot1 minecraft:mcc_parkourwarrior (6.2,102.0,-6.1) adventure; Bot2 minecraft:mcc_parkourwarrior (-2.3,102.0,-9.0) adventure; Bot3 minecraft:mcc_parkourwarrior (0.1,102.0,5.9) adventure; Bot4 minecraft:mcc_parkourwarrior (2.5,101.0,5.1) adventure; Bot5 minecraft:mcc_parkourwarrior (-2.7,101.0,9.9) adventure; Bot6 minecraft:mcc_parkourwarrior (9.9,101.0,3.7) adventure
- positions after: Bot1 minecraft:mcc_lobby (10.5,71.0,97.7) survival; Bot2 minecraft:mcc_lobby (-18.1,71.5,113.6) survival; Bot3 minecraft:mcc_lobby (22.6,71.0,64.8) survival; Bot4 minecraft:mcc_lobby (-4.8,72.3,65.9) survival; Bot5 minecraft:mcc_lobby (15.8,71.0,70.0) survival; Bot6 minecraft:mcc_lobby (31.3,72.2,78.6) survival

**WARN** [bot-movement(benign)] x2: Bot6 moved wrongly!

```
Bot6 moved wrongly!

```

### pass2: battlebox (Battle Box) - PASS

- positions lobby: Bot1 minecraft:mcc_lobby (8.9,71.0,89.2) survival; Bot2 minecraft:mcc_lobby (-18.5,71.5,113.7) survival; Bot3 minecraft:mcc_lobby (28.9,71.0,57.9) survival; Bot4 minecraft:mcc_lobby (4.3,72.3,62.2) survival; Bot5 minecraft:mcc_lobby (15.8,71.5,70.0) survival; Bot6 minecraft:mcc_lobby (36.9,71.0,85.3) survival
- positions t10: Bot1 minecraft:mcc_battlebox (100.6,65.0,19.3) adventure; Bot2 minecraft:mcc_battlebox (-2.5,70.0,-11.5) spectator; Bot3 minecraft:mcc_battlebox (4.3,65.0,-18.3) adventure; Bot4 minecraft:mcc_battlebox (100.5,65.0,-18.3) adventure; Bot5 minecraft:mcc_battlebox (0.9,65.0,-18.3) adventure; Bot6 minecraft:mcc_battlebox (-1.6,65.0,19.3) adventure
- positions t60: Bot1 minecraft:mcc_battlebox (107.5,65.0,13.3) survival; Bot2 minecraft:mcc_battlebox (-16.0,65.0,17.7) spectator; Bot3 minecraft:mcc_battlebox (-16.7,65.0,-6.3) survival; Bot4 minecraft:mcc_battlebox (117.7,65.0,-16.7) survival; Bot5 minecraft:mcc_battlebox (17.7,65.0,-8.7) survival; Bot6 minecraft:mcc_battlebox (-6.4,65.0,-10.7) survival
- positions after: Bot1 minecraft:mcc_lobby (33.6,71.0,78.2) survival; Bot2 minecraft:mcc_lobby (23.7,71.5,110.7) survival; Bot3 minecraft:mcc_lobby (-4.3,71.0,71.6) survival; Bot4 minecraft:mcc_lobby (-7.5,71.0,67.1) survival; Bot5 minecraft:mcc_lobby (-15.7,70.0,128.1) survival; Bot6 minecraft:mcc_lobby (-29.7,72.8,102.7) survival

**WARN** [bot-movement(benign)] x3: Bot6 moved wrongly!

```
Bot6 moved wrongly!

```


## Boot / other issues


**WARN** [server] x1: Can't keep up! Is the server overloaded? Running 3807ms or 76 ticks behind

```
Can't keep up! Is the server overloaded? Running 3807ms or 76 ticks behind

```

## Bot kicks / errors

None.
