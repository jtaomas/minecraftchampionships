# `tgttos` datapack — exact unpack (A7)

Source world: `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-badlands-by-mickeykat\TGTTOS Badlands - By MickeyKat`
World name: `TGTTOS Badlands Practise` · DataVersion 3120 (1.19.2) · server under test: Paper 1.21.1 (`version_history.json` = `1.21.1-133`)
Datapack: `datapacks/tgttos`, enabled in `level.dat` as `file/tgttos` [H]
Raw copies of every file: `data/raw/` · hashes: `data/source_hashes.csv` · line-by-line commands: `data/datapack_commands.csv`

Evidence tags: **H** = directly observed in file/command/render; **M** = inference from two sources; **L** = guess.

---

## 1. Pack metadata and wiring

`data/raw/pack.mcmeta` [H]:

```json
{
    "pack": {
      "pack_format": 10,
      "description": "Made by Mickey_Kat"
    }
}
```

`data/raw/tag_load.json` [H]:

```json
{
    "values": [
        "tgttos:load"
    ]
}
```

`data/raw/tag_tick.json` [H]:

```json
{
    "values": [
        "tgttos:loop"
    ]
}
```

File tree [H]:

```
tgttos/
  pack.mcmeta
  data/minecraft/tags/functions/load.json
  data/minecraft/tags/functions/tick.json
  data/tgttos/functions/load.mcfunction
  data/tgttos/functions/loop.mcfunction
```

That is the whole pack: **2 functions, 2 tags, 1 mcmeta**, 130 lines in `loop` (113 non-empty), 8 lines in `load`. There are no predicates, loot tables, advancements, structures, recipes, damage types or custom dimensions. [H]

The **source** world's `level.dat` has `DataPacks.Enabled = ["vanilla", "file/tgttos"]` and `Disabled = []`, so the pack was live when the world was saved. [H] The **imported** `mcc_tgttos_badlands` world is different: `DataPacks.Enabled = ["vanilla", "file/bukkit", "paper"]` and `Disabled = ["bundle", "trade_rebalance"]`; **`file/tgttos` is in neither list**, so the pack is on disk but not loaded. The imported pack is byte-identical to the source. Full verification: `data/import_and_121_verification.md` and `data/import_state.json`. [H]

---

## 2. `load.mcfunction` — exact text (8 lines)

```mcfunction
scoreboard objectives add hit_chicken minecraft.custom:minecraft.damage_dealt_resisted
scoreboard objectives add playing dummy
scoreboard objectives add countdown dummy
scoreboard objectives add start dummy
scoreboard objectives add spawntime dummy
scoreboard objectives add playtimeS dummy
scoreboard objectives add playtimeT dummy
scoreboard objectives add finished dummy
```

What it does [H]:

| Objective | Criterion | Role | Reads / writes |
|---|---|---|---|
| `hit_chicken` | `minecraft.custom:minecraft.damage_dealt_resisted` | finish detector: counts damage that the target **resisted** | written by the vanilla statistic system, read by `loop` lines 33–40, 123 |
| `playing` | dummy | 1 while the run is live | set 1 at countdown 130; set 0 on finish; gates timer + marker lines |
| `countdown` | dummy | 0–130 countdown clock | added 1/tick per started player; reset at 130 |
| `start` | dummy | sign-click latch | `+1` per sign click; reset at 130; gates the countdown |
| `spawntime` | dummy | ticks since last reset-marker summon | added 1/tick while playing; reset at 20 and on finish |
| `playtimeS` | dummy | whole seconds of run time | added 1 every 20 ticks; shown in the finish subtitle; reset on finish |
| `playtimeT` | dummy | ticks remainder | added 1/tick; resets every 20; shown in the finish subtitle |
| `finished` | dummy | ticks since the chicken hit | added 1/tick while `hit_chicken>=1`; 60 triggers the reset |

Notes [H/M]:

- **No display slot is set.** There is no sidebar, tab list or below-name display; the only HUD is the title/subtitle text. [H]
- **No gamerule is set by the pack.** Every gamerule is baked into `level.dat` (see `data/gamerules.csv`). [H]
- **No team, tag, spawnpoint, worldborder, difficulty or time command.** [H]
- `scoreboard.dat` contains a ninth objective, `playtime` (dummy), which this pack never creates or uses. It is a leftover from an earlier version. [H]
- On a fresh 1.21.1 world the `add` commands would fail because the folder is not read (see §8); in *this* save the objectives already exist in `scoreboard.dat`, so the sign's `scoreboard players add @s start 1` would still find `start`. [H/M]

---

## 3. `loop.mcfunction` — structure (130 lines, run every tick)

Line numbers are the file's 1-based lines. Blank lines: 4, 8, 24, 29, 32, 50, 51, 54, 101, 121, 122, 125–130.

| Lines | Group | Trigger | What happens |
|---|---|---|---|
| 1 | item hygiene | every tick | `kill @e[type=item]` — every dropped item is deleted |
| 2 | chicken immunity | every tick | `effect give @e[type=chicken] resistance 1 5 true` — Resistance VI, 1 s, no particles |
| 3 | start-pen hygiene | every tick | `fill 9 -40 21 11 -42 40 air replace blue_concrete` (x 9–11, y −42…−40, z 21–40) |
| 5–6 | kit | every tick | offhand `blue_concrete` with 44-entry `CanPlaceOn`, count 2 then 1 |
| 7 | kit | every tick | `hotbar.0` iron pickaxe, Efficiency II, `CanDestroy:["minecraft:blue_concrete"]` |
| 9–23 | countdown | `start>=1` | advance `countdown`, title 5-4-3-2-1-GO, open the gate, set `playing` |
| 25–28 | timers | `playing>=1` | `spawntime`, `playtimeT`, `playtimeS` |
| 30–31 | reset markers | `spawntime>=20` | summon invisible `armor_stand` 10 above each playing player, tag `remove_block`; reset `spawntime` |
| 33–40 | finish | `hit_chicken>=1` | stop timer, show time, spectator, `finished+1`, particles/sound on the first tick |
| 41–49 | finish reset | `finished>=60` | restore 3 barrier walls, clear finisher, adventure, `tp @a 10 -42 31`, reset the finisher's timers |
| 52–53 | block cleanup | `finished>=60` | for every `remove_block` stand, `fill ~-15 ~-25 ~-15 ~15 ~5 ~15 air replace blue_concrete`; kill all stands |
| 56–120 | cactus reset | `finished>=60` | 64 `fill`/`setblock … cactus` commands (179 positions) |
| 123–124 | score reset | `finished>=60` | `hit_chicken=0` and `finished=0` for **all** players |

The exact text of every non-blank line is in `data/datapack_commands.csv` (113 rows, with group, trigger, effect and 1.21.1 risk). The raw file is `data/raw/loop.mcfunction`. [H]

### 3.1 The kit lines — exact (lines 5–7)

Line 5 (line 6 is identical except the trailing count is `1`; line 5 ends `…]} 2`) [H]:

```mcfunction
item replace entity @a weapon.offhand with blue_concrete{CanPlaceOn:["minecraft:blue_concrete","minecraft:gray_wool","minecraft:jungle_planks","minecraft:jungle_fence","minecraft:red_wool","minecraft:light_gray_concrete_powder","minecraft:gray_concrete","minecraft:nether_brick_wall","minecraft:red_concrete","minecraft:sea_lantern","minecraft:chain","minecraft:red_terracotta","minecraft:diorite","minecraft:white_terracotta","minecraft:white_concrete","minecraft:cactus","minecraft:soul_sand","minecraft:red_sand","minecraft:cyan_concrete_powder","minecraft:smooth_stone","minecraft:stripped_spruce_log","minecraft:acacia_leaves","minecraft:gravel","minecraft:diorite_wall","minecraft:nether_brick_slab","minecraft:iron_trapdoor","minecraft:gray_terracotta","minecraft:andesite","minecraft:red_sandstone_stairs","minecraft:light_gray_concrete","minecraft:light_gray_terracotta","minecraft:spruce_planks","minecraft:brown_concrete_powder","minecraft:granite","minecraft:terracotta","minecraft:stone","minecraft:smooth_red_sandstone","minecraft:stone_bricks","minecraft:blackstone","minecraft:iron_bars","minecraft:cyan_terracotta","minecraft:oak_leaves","minecraft:black_terracotta","minecraft:black_concrete"]} 2
```

Line 7 [H]:

```mcfunction
item replace entity @a hotbar.0 with iron_pickaxe{CanDestroy:["minecraft:blue_concrete"],Enchantments:[{id:"minecraft:efficiency",lvl:2s}]}
```

Semantics [H/M]:

- The offhand is force-set every tick. A player always has exactly **1 blue concrete** (the count-2 line is immediately overwritten by the count-1 line), and cannot use the offhand for anything else.
- The pickaxe is force-set every tick, so its durability never depletes; it is not `Unbreakable`, it is just replaced.
- `CanPlaceOn` is a 44-block allow-list. It includes `blue_concrete` (bridge on bridge), `cactus` (you can anchor on a cactus), most of the terrain palette (but **not dirt**), and most build materials.
- It **excludes 19 of the 62 block IDs in the build**, including `dirt`, `quartz_block`, `polished_granite`, `cracked_stone_bricks`, `smooth_stone_slab`, `cyan_concrete`, `light_blue_concrete`, `black/white_wool`, `daylight_detector`, `lantern`, `nether_brick_fence`, `iron_block`, `black_concrete_powder`, `barrier`, `oak_wall_sign` and `dead_bush`. Full table: `data/can_place_on.csv`. [H]
- The `dirt` omission is the one with real gameplay impact: exposed dirt on the start slope and finish trench cannot be used as a bridge anchor. [H blocks + M impact]
- `blue_concrete` is not in the saved world palette; it exists only as the player's bridging item. [H]
- The list is a **client-side adventure-mode rule**; it only applies in Adventure. In Survival the item can be placed on anything (and the map itself can be broken). [M]

### 3.2 Countdown — exact (lines 9–23)

```mcfunction
execute as @a[scores={start=1..}] at @s run scoreboard players add @a countdown 1
execute as @a[scores={countdown=20..120}] at @s run title @a times 0 3 0
execute as @a[scores={countdown=20..120}] at @s run title @a title {"text":"Starting in","color":"aqua"}
execute as @a[scores={countdown=20..40}] at @s run title @a subtitle {"text":"5","bold":true}
execute as @a[scores={countdown=40..60}] at @s run title @a subtitle {"text":"4","bold":true}
execute as @a[scores={countdown=60..80}] at @s run title @a subtitle {"text":"3","bold":true}
execute as @a[scores={countdown=80..100}] at @s run title @a subtitle {"text":"2","bold":true}
execute as @a[scores={countdown=100..120}] at @s run title @a subtitle {"text":"1","bold":true}
execute as @a[scores={countdown=120..130}] at @s run title @a times 0 20 10
execute as @a[scores={countdown=120..130}] at @s run title @a title {"text":"GO!!!","color":"aqua"}
execute as @a[scores={countdown=120..130}] at @s run title @a subtitle {"text":" "}
execute as @a[scores={countdown=120..130}] at @s run fill 9 -38 41 12 -42 20 air replace barrier
execute as @a[scores={countdown=130..}] at @s run scoreboard players set @a playing 1
execute as @a[scores={countdown=130..}] at @s run scoreboard players set @a start 0
execute as @a[scores={countdown=130..}] at @s run scoreboard players set @a countdown 0
```

Timeline for **one** started player [H]:

| Ticks | Display | Gate | State |
|---|---|---|---|
| 0–19 | none | closed | click → `start=1` |
| 20–39 | "Starting in" + "5" | closed | 1 s |
| 40–59 | "4" | closed | |
| 60–79 | "3" | closed | |
| 80–99 | "2" | closed | |
| 100–119 | "1" | closed | |
| 120–129 | "GO!!!" (blank subtitle) | **opens at 120** | players can move, timer not yet running |
| 130 | GO title remains | open | `playing=1`, `start=0`, `countdown=0`; timer starts |

So: **6.5 s from click to timer start**, with a 0.5 s head start after the gate opens. [H]

Multiplayer bug [H command, M effect]: line 9 is `execute as @a[start=1..] … run scoreboard players add @a countdown 1`. The inner `@a` adds 1 to **every** player for **each** player with `start>=1`. If 10 players click the sign, `countdown` advances 10/tick and the 130-tick countdown lasts 13 ticks (0.65 s). If only one player clicks, the countdown is normal, but `playing` is set for every player at 130, so everyone starts. [H/M]

### 3.3 Timer, markers and finish — exact (lines 25–49)

```mcfunction
execute as @a[scores={playing=1..}] at @s run scoreboard players add @s spawntime 1
execute as @a[scores={playing=1..}] at @s run scoreboard players add @s playtimeT 1
execute as @a[scores={playtimeT=20..}] at @s run scoreboard players add @s playtimeS 1
execute as @a[scores={playtimeT=20..}] at @s run scoreboard players set @s playtimeT 0

execute as @a[scores={spawntime=20..}] at @s run summon armor_stand ~ ~10 ~ {NoGravity:1b,Silent:1b,Invulnerable:1b,Small:1b,Invisible:1b,NoBasePlate:1b,Tags:["remove_block"]}
execute as @a[scores={spawntime=20..}] at @s run scoreboard players set @s spawntime 0

execute as @a[scores={hit_chicken=1..}] at @s run scoreboard players set @s playing 0
execute as @a[scores={hit_chicken=1..}] at @s run title @a times 0 100 20
execute as @a[scores={hit_chicken=1..}] at @s run title @a subtitle ["",{"text":"Time: ","italic":true,"color":"dark_gray"},{"score":{"name":"@s","objective":"playtimeS"},"italic":true,"color":"dark_gray"},{"text":" Seconds and ","italic":true,"color":"dark_gray"},{"score":{"name":"@s","objective":"playtimeT"},"italic":true,"color":"dark_gray"},{"text":" Ticks","italic":true,"color":"dark_gray"}]
execute as @a[scores={hit_chicken=1..}] at @s run title @a title {"text":"Finished","color":"green"}
execute as @a[scores={hit_chicken=1..}] at @s run gamemode spectator @s
execute as @a[scores={hit_chicken=1..}] at @s run scoreboard players add @s finished 1
execute as @a[scores={finished=1}] at @s run particle minecraft:totem_of_undying ~ ~ ~ 0 0.3 0 1 50 normal
execute as @a[scores={finished=1}] at @s run playsound minecraft:entity.arrow.hit_player ambient @a ~ ~ ~ 1 1 0

execute as @a[scores={finished=60..}] at @s run fill 9 -38 20 12 -42 20 barrier
execute as @a[scores={finished=60..}] at @s run fill 12 -38 20 12 -42 41 barrier
execute as @a[scores={finished=60..}] at @s run fill 12 -42 41 9 -38 41 barrier
execute as @a[scores={finished=60..}] at @s run clear @s
execute as @a[scores={finished=60..}] at @s run gamemode adventure @s
execute as @a[scores={finished=60..}] at @s run tp @a 10 -42 31
execute as @a[scores={finished=60..}] at @s run scoreboard players set @s spawntime 0
execute as @a[scores={finished=60..}] at @s run scoreboard players set @s playtimeT 0
execute as @a[scores={finished=60..}] at @s run scoreboard players set @s playtimeS 0
```

Reading [H/M]:

- `spawntime`/`playtimeT`/`playtimeS` are **per player**; `playtimeS` is seconds, `playtimeT` is the tick remainder (0–19). The finish subtitle reads "*N* Seconds and *T* Ticks".
- Every 20 ticks a marker armor stand is summoned **10 blocks above** the player. That is the breadcrumb trail used later by the block cleanup. The stand is invisible, silent, invulnerable, small, no gravity, no base plate, tag `remove_block`.
- The finish trigger is **any damage dealt that was resisted** (`hit_chicken`). The chickens are kept at Resistance VI by line 2, so a punch deals 0 final damage but the statistic still counts the resisted amount. This is the classic "punch the chicken" datapack finish. [H command + M stat semantics]
- On finish: `playing=0`, a green "Finished" title with the player's time, `gamemode spectator @s`, `finished+1` every tick. The particle/sound run only on `finished=1` (first tick). The title is re-sent every tick because `hit_chicken` stays ≥1, so it holds for 3 s. [H]
- At `finished=60` (3 s): three barrier fills, `clear @s`, `gamemode adventure @s`, **`tp @a`** to the start, reset the finisher's timers. [H]
- The barrier fills recreate only the 3 walls (140 unique positions); the static x=8 lip (44 barriers) is never removed, so the pen is sealed again. Full barrier arithmetic in §6. [H]

### 3.4 Block cleanup and cactus reset — exact (lines 52–53, 56–120)

```mcfunction
execute as @a[scores={finished=60..}] at @s run execute as @e[tag=remove_block] at @s run fill ~-15 ~-25 ~-15 ~15 ~5 ~15 air replace blue_concrete
execute as @a[scores={finished=60..}] at @s run kill @e[tag=remove_block]
```

- The first line runs **for each finishing player** and, inside, **for every `remove_block` stand in the world**: a 31×31×21 box (`±15` x/z, `−25…+5` y) of `air replace blue_concrete`. Then all stands are killed. [H]
- Because the stands were dropped every second along every player's path, the union of boxes approximates a swept corridor around the route. It is not a full map reset; blocks placed more than 15 blocks away or more than 25 below a marker survive. [H/M]
- The cactus reset is 64 commands (lines 56–120, line 101 blank): **56 `fill` + 8 `setblock`**; the full list is `data/cactus_reset.csv`. It covers **179** cactus positions. [H]
- The world has **181** cacti. The reset is missing the 4-block column at `x=154, z=33, y=−3…0`, and contains 2 stale positions at `x=22, z=25, y=−42…−41` where the world currently has no cactus. So 177/181 are protected; 4 are not. [H]
- The reset runs only when a player reaches `finished=60`. If nobody finishes, cacti and player blocks persist. There is **no reset on load, on death, on quit or on a timer**. [H]

### 3.5 Final score reset — exact (lines 123–124)

```mcfunction
execute as @a[scores={finished=60..}] at @s run scoreboard players set @a hit_chicken 0
execute as @a[scores={finished=60..}] at @s run scoreboard players set @a finished 0
```

These two lines are inside the `finished=60` block but use **`@a`**, so one finisher clears everyone's `hit_chicken` and `finished`. That is the multiplayer bug described in §7. [H]

---

## 4. Start, respawn, finish — the full state machine

### 4.1 Start

1. Player is in the pen at world spawn `10,−42,31`, on a quartz floor at y=−43. [H]
2. The only trigger is the oak wall sign at `9,−41,31` (facing east), whose **line 1** JSON is:
   `{"clickEvent":{"action":"run_command","value":"scoreboard players add @s start 1"},"text":""}`.
   The visible "Start" is line 2 (`bold`, `green`), with no click event. [H]
3. Clicking line 1 runs `scoreboard players add @s start 1` **as the server, at the sign, with the player as `@s`**. Sign commands use command-block permission in 1.20+, so a non-op can trigger it. [H wiki + M 1.21.1]
4. The next tick, line 9 sees `start>=1` and starts adding to `countdown`. [H]
5. At countdown 120 the gate fill removes every barrier in `x 9–12, y −42…−38, z 20–41`. [H]
6. At 130 `playing=1`, `start=0`, `countdown=0`; the timer starts. [H]

The pen: quartz floor `x 9–11, z 21–40, y −43`; black-wool frame `x 7–12, y −43…−40, z 20–41`; barrier walls at `x=12`, `z=20`, `z=41` (5 tall, y −42…−38) and a static barrier lip at `x=8, y −39…−38, z 20–41`. The sign is mounted on the x=8 wall. [H]

### 4.2 Respawn

There is **no respawn handler**. [H]

- `fallDamage=false`; the only common death is the void. [H]
- `doImmediateRespawn=false`, `keepInventory=false`, `spawnRadius=10`. [H]
- The world spawn is `10,−42,31` (inside the pen). A player with a personal spawn (`SpawnForced=1`) respawns there exactly; a new player without one respawns within 10 blocks of it, i.e. possibly **outside** the 5×22 pen. [H/M]
- `playing`, `playtimeS`, `playtimeT` are **not** reset on death. The run timer keeps counting. [H]
- The kit is re-given by the per-tick `item replace` lines (if they work); there is no explicit "fresh kit on respawn" logic. [H]
- On a void death, a player can be back near the start with the clock still running and, if outside the pen, can rejoin the course without clicking the sign. That is a soft reset, not a race restart. [M]

### 4.3 Finish

- Trigger: `hit_chicken>=1`, i.e. the player dealt damage that the target resisted. The chickens are kept at Resistance VI, so a punch or pickaxe hit counts. [H/M]
- The chickens are **normal saved chickens** (no `NoAI`, no `PersistenceRequired`, no `Invulnerable`); the only protection is the per-tick Resistance effect. If the pack is not running, one hit kills a chicken and there is **no respawn command**. [H]
- There are 27 chickens on the ledge at `x≈241, y=−1, z 11–52`, plus one chicken below the world at `222.9,−114.4,−48.0` that will be removed on load. [H]
- On finish: spectator for 3 s, then adventure + `tp @a 10 −42 31`, `clear @s`, barriers restored. The finisher is put back **inside the sealed pen** and must click the sign again. [H]
- `tp @a`, `clear @s`, `gamemode adventure @s` and the score resets are **global/per-player mismatched**: one finisher moves everyone, clears only themselves, and clears everyone's `hit_chicken`/`finished`. See §7. [H]

### 4.4 Checkpoints

**None.** There is no checkpoint objective, entity, gate, banner, beacon or sign; `data/checkpoints.csv` records this. The only gate is the start barrier. [H]

---

## 5. Timers

| Timer | Length | Scope | Shown |
|---|---|---|---|
| countdown | 130 ticks (6.5 s) for one started player | global, driven by `start` clicks | title/subtitle |
| run time | unlimited | per player | only at finish: `playtimeS` seconds + `playtimeT` ticks |
| finish hold | 60 ticks (3 s) | per finisher (but reset globally) | "Finished" title |
| marker interval | 20 ticks (1 s) | per playing player | invisible armor stand |
| cactus/barrier reset | once per finish | global | none |

There is **no round timer, no time limit, no auto-restart, no results screen and no placement order**. A run ends only when the player punches a chicken; a player who never finishes can stay on the course indefinitely. [H]

---

## 6. Barriers, blocks and palette facts used by the pack

- Barrier blocks in the world: **184**. Breakdown: the 3 gate walls (x=12, z=20, z=41) union to **140**; the static x=8 lip is **44**. `fill … air replace barrier` at countdown 120 removes only the 140 (the fill box is x 9–12); the 44 lip stays. The finish reset recreates the 140. [H]
- The countdown fill box `9 −38 41 12 −42 20` normalises to `x 9–12, y −42…−38, z 20–41`. It removes **all** barriers in that volume, including the gate walls; it does not touch x=8. [H]
- `blue_concrete` does not occur in the saved build (0 blocks); it is only the player's kit. [H]
- Cacti: 181 in the world, 179 in the reset, 4 unprotected at `x=154, z=33`, 2 stale reset positions at `x=22, z=25`. [H]
- The `fill … cactus` commands place cacti without checking support; the fixed coordinates are all on valid ground in the saved world, but a player-placed block at one of those cells would be overwritten by the reset (the fill does not use `replace`). [M]

---

## 7. Multiplayer and robustness bugs (single-player practice design)

These are **logic** findings, independent of the 1.21.1 port. [H command, M effect]

1. **Countdown speed scales with clicks.** `scoreboard players add @a countdown 1` inside `as @a[start=1..]` → N started players advance the clock N× per tick. 10 players = 0.65 s countdown. [H/M]
2. **Titles broadcast to everyone.** Every `title @a` inside `as @a[start=1..]` re-sends the title once per started player per tick; with 10 players that is 10 copies per tick. [H]
3. **One finisher resets everyone.** At `finished=60`, `tp @a 10 −42 31` teleports all players; `scoreboard players set @a hit_chicken 0` and `@a finished 0` clear everyone. A second player who finished within the same 3 s has their `finished` cleared before their own `finished=60` block, so they never get the return teleport and stay a spectator. [H]
4. **Block cleanup is global.** The first finisher's `finished=60` block runs `fill … air replace blue_concrete` around **every** `remove_block` stand, including stands from racers still on the course. Their bridges can disappear under them. [H]
5. **Cactus reset is global** and runs once per finisher (duplicated work, harmless). [H]
6. **Clear is only the finisher, but teleport is everyone.** Non-finishers are moved to the start with their inventory intact and their `playing` state possibly still 1 (unless the `@a` score reset caught them). [H]
7. **No reset on quit.** A player who places blocks and leaves leaves them in the world; the next finish resets only the boxes around the remaining players' stands (the quitter's stands are still in the world, so they are included if they were summoned; if the quitter left before a stand was summoned, the blocks are not covered). [H/M]
8. **No reset on load.** A server restart preserves every placed block and every cactus the players broke. [H]
9. **Late joiners** have no `countdown` score; they never match `countdown=130`, so they never get `playing=1`; their timer stays at 0. They can still punch a chicken and "finish" with 0 s. [M]
10. **Re-clicking the sign during a run** sets `start=1` again; the next tick restarts the countdown and at 130 resets `playing=1` for everyone, including players mid-run, and clears their `start`/`countdown` — a race restart triggered by a finished player in the pen. [M]

---

## 8. 1.21.1 compatibility — exact likely failures

Server: Paper **1.21.1** (pack format **48**) [H from `version_history.json` / log]. World: 1.19.2 (DataVersion 3120) [H].

| # | Area | 1.19.2 artifact | 1.21.1 behaviour | Exact failure | Tag |
|---|---|---|---|---|---|
| 0 | Imported registration | source `Enabled=[vanilla, file/tgttos]` | imported `Enabled=[vanilla, file/bukkit, paper]`, `Disabled=[bundle, trade_rebalance]`; `file/tgttos` in neither | The pack is on disk but **not enabled**; no function or tag is read at all. This is the first gate. Enable `file/tgttos` (offline `level.dat` edit or `/datapack enable`) before the folder/format fixes can matter. | **H** |
| 1 | Folder layout | `data/tgttos/functions/`, `data/minecraft/tags/functions/` | 1.21 renamed to `function/` and `tags/function/` | Even after enabling, the pack **registers no functions and no load/tick tags**. `#minecraft:load` and `#minecraft:tick` are empty for this pack; `/function tgttos:loop` is unknown. No countdown, no gate opening, no kit, no finish, no reset. | **H** |
| 2 | Pack format | `pack_format: 10` | 1.21.1 expects 48 | Flagged old/incompatible; a format-10 pack can usually still be enabled, but the **folder layout** (row 1) is the functional blocker, not the number alone. | **H/M** |
| 3 | Item NBT | `blue_concrete{CanPlaceOn:[…]}`, `iron_pickaxe{CanDestroy:[…],Enchantments:[…]}` | 1.20.5 replaced item NBT with components (`can_place_on`, `can_break`, `enchantments`) | Lines 5–7 **fail to parse every tick**; no offhand blue concrete and no pickaxe. Even after renaming the folders, the map has no bridging block. New syntax: `blue_concrete[minecraft:can_place_on={blocks:[…]}]`, `iron_pickaxe[minecraft:can_break={blocks:"minecraft:blue_concrete"},minecraft:enchantments={levels:{"minecraft:efficiency":2}}]`. | **H** |
| 4 | Sign format | `Text1…Text4`, `Color`, `GlowingText` | 1.20+ upgrades to `front_text.messages` / `back_text` | The imported sign **did** upgrade to `front_text` and the clickEvent survived in `messages[0]`, but it is attached to **empty line 1**; the visible "Start" is line 2. In 1.20+ clickable sign text is hit-tested per line, so the blank top line may have no clickable hotspot. Verify in-game before relying on it. | **H NBT / M-L click** |
| 5 | Sign permission | `run_command` | Sign commands run at **command-block permission**, not the player's permission | This is *not* a failure: a non-op can start the map. Only requires the player not to be editing the sign (Adventure). | **H wiki / M** |
| 6 | Gamemode | source `level.dat GameType=1` (Creative); imported `GameType=0` (Survival); saved player `playerGameType=2` (Adventure) | No code sets Adventure at start; Multiverse `gamemode: survival`; server default Survival, `force-gamemode=false` | New players enter in Survival (imported), so `CanPlaceOn`/`CanDestroy` are ignored and the build can be broken; in Creative they can fly out of the pen and break anything. The only `gamemode adventure` is on the finish reset (line 45). | **H/M** |
| 7 | Respawn | no handler; `fallDamage=false`, `spawnRadius=10`, `doImmediateRespawn=false`, `keepInventory=false` | Vanilla respawn at world spawn ±10 | Void death returns the player near the start with the timer still running; a new player may respawn outside the pen. No run reset, no checkpoint, no kit-refill command. | **H/M** |
| 8 | Chickens | 27 normal chickens; per-tick Resistance from the pack | If the loop is dead, the chickens have no Resistance and can be killed in one hit; no respawn | The finish can be destroyed; one chicken is already below the world at y=−114. The saved chickens all have the Resistance effect, consistent with the loop running at save time. | **H/M** |
| 9 | Scoreboard | Objectives saved in `scoreboard.dat` | Objectives persist even if `load` never runs | The sign's `scoreboard players add @s start 1` still finds `start`, but nothing reacts. `hit_chicken` (a statistic criterion) still increments, but no finish. | **H/M** |
| 10 | Commands | `fill`, `effect`, `summon`, `title`, `particle`, `playsound`, `gamemode`, `tp`, `clear`, `kill` | All still valid in 1.21.1 | Renaming folders + item components is sufficient for the rest; no other command syntax needs changing before 1.21.5. | **H/M** |
| 11 | Text components | JSON `{"text":…}` in `title` | 1.21.1 still accepts JSON text components (the SNBT/`click_event` rewrite lands in 1.21.5) | No failure on 1.21.1. | **H/M** |
| 12 | Objective criterion | `minecraft.custom:minecraft.damage_dealt_resisted` | Still a valid custom statistic in 1.21.1 | No failure; the objective is already saved. | **M** |
| 13 | World upgrade | DataVersion 3120 | 1.21.1 DataFixer upgrades blocks, sign, player inventory, entities | All 62 block IDs are still valid; the sign/daylight detectors upgrade; the player's legacy item NBT is converted to components (so the *saved* player may keep a working kit even though the commands fail). | **M** |
| 14 | `playtime` objective | saved dummy objective, never used | persists | Dead objective; no effect. | **H** |
| 15 | Source enable entry (for reference) | source `level.dat DataPacks.Enabled = [vanilla, file/tgttos]` | The imported world's `Enabled` is `[vanilla, file/bukkit, paper]`; `file/tgttos` is absent | The source was enabled; the import did not preserve the entry (row 0). Do not read this row as "still enabled" in the imported world. | **H** |

**Bottom line:** on 1.21.1 this world loads, but the imported pack is **not even enabled**; even if enabled, the game loop does not run. Players spawn inside a sealed barrier pen (Survival/Adventure) or can fly out (Creative); the sign may start a scoreboard counter that nothing reads. Fixing only the folder names still leaves the kit lines failing. A port needs (0) enable `file/tgttos`, (a) `function/` + `tags/function/` + format 48, (b) item components, (c) Adventure mode + sign-click verification + a mode/start/reset wrapper (the current plugin already has the last part). [H/M]

---

## 9. What is reusable (with cautions)

1. **Chicken-resistance finish**: `effect give @e[type=chicken] resistance 1 5 true` + a `damage_dealt_resisted` objective is a zero-code, any-chicken finish detector. Useful for imported maps that have no plugin hooks; not needed by `McmTgttos` (it uses `PrePlayerAttackEntityEvent` and tagged chickens). Caution: it depends on the stat semantics and on the chickens surviving; it has no pen validation, no place order and no decoy rejection. [H/M]
2. **Sign-click start**: server-run `run_command` at command-block permission means non-ops can start a map without a plugin. Caution: one global latch, no per-player registration; the click event is on an empty line, and a creative player can open the editor instead. [H/M]
3. **One-block offhand bridge**: `item replace … weapon.offhand` with a 1-count concrete and a `CanPlaceOn` allow-list is a compact anti-grief kit. Caution: the list must include the whole walkable palette (this one omits dirt), and it only applies in Adventure. [H/M]
4. **Breadcrumb + box cleanup**: summon an invisible marker every second, then `fill air replace blue_concrete` around each marker. It is a cheap no-paste reset for a persistent map. Caution: global, multiplayer-hostile, not persistent-safe, and only covers ±15/−25/+5. The current plugin's paster/diff reset is strictly better. [H/M]
5. **Cactus refill list**: an explicit list of hazard blocks to restore after a run. The current `TgMapData` has no per-block reset list because it re-pastes the whole map; an imported-map adapter could generate such a list from the schematic and avoid the 4 missing cacti. [H/M]
6. **Map design**: start pen → marked slope → themed mine/rail yard → black rail obstacle → canyon → mesa/cave landmark → finish canyon → fenced chicken ledge under an MCC billboard. The sequence and silhouettes are the strongest part of the map and are worth copying as *composition*, not as code. [M]

Do **not** reuse: the countdown arithmetic, the global `@a` finish reset, the global block cleanup, the lack of respawn/checkpoint, or the sign-only start as the sole registration mechanism.
