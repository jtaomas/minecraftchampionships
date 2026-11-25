# Scoreboard and team state — MG-SB-01

Evidence: `data/scoreboard.dat` decoded from the 1.17.1 source world (NBT dump `data/read_scoreboard.py`), plus the 474 CB-14/15 commands. **H** where the NBT/command is quoted; **M** where the intent is inferred.

## 1. Objectives

| Objective | Criterion | Used by Sky Battle? | Evidence |
|---|---|---|---|
| `timer_skybattle` | dummy | yes — seconds since start | `scoreboard players add @a[level=10..13] timer_skybattle 1` (CB-14 x934) |
| `death_skybattle` | deathCount | yes — auto-increments on death | `scores={death_skybattle=1}` death handling (CB-14 x949) |
| `ironblock` | `minecraft.mined:minecraft.iron_block` | yes — mined ribbon knot | `if score @s ironblock matches 1 run give @s iron_block 1` (CB-15 x1001) |
| `POINTS` | dummy | yes — team win points only | `scoreboard players add Red/Blue/Green/Yellow POINTS 50` |
| `kill_event` | playerKillCount | reset but never read by Sky Battle | `scoreboard players set @a[level=9] kill_event 0` |
| `death_event` | deathCount | reset but not read | `... death_event 0` |
| `dummy_event` | dummy | reset but not read | `... dummy_event 0` |
| `health` | health | displayed on sidebar slot 2, not Sky Battle | `DisplaySlots.slot_2=health` |
| `Super_Clash`, `kill_sc_*`, `timer_parkourtag`, `death_parkourtag`, `kill_parkourtag`, `lap_counter`, `position_tr`, `lobbyteleporter`, `leave` | various | other games | same `scoreboard.dat` |

**No objective is created by any command in the world.** All 26 objectives already exist in `data/scoreboard.dat`. A reimplementation that copies only the command blocks and not the scoreboard will fail every `scoreboard players` line silently. **H**.

## 2. Teams

| Team | Display | TeamColor (stored) | Members in snapshot | Sky Battle role |
|---|---|---|---|---|
| `Sky_Battle_Red` | `{"text":"Sky_Battle_Red"}` | **yellow** | empty | Red team, XP level 10 |
| `Sky_Battle_Blue` | `{"text":"Sky_Battle_Blue"}` | **yellow** | empty | Blue team, XP level 11 |
| `Sky_Battle_Green` | `{"text":"Sky_Battle_Green"}` | **yellow** | empty | Green team, XP level 12 |
| `Sky_Battle_Yellw` | `{"text":"Sky_Battle_Yellw"}` | **yellow** | empty | Yellow team, XP level 13 (name misspelt) |
| `pve` | `{"text":"pve"}` | — | 3 players | post-game/winner holding team |

All four Sky Battle teams have `TeamColor=yellow`. The `tellraw` win messages use the correct colours (`red`, `blue`, `dark_green`, `yellow`), but the tab/name colour of every team is yellow. **H**. This is a bug to fix, not copy.

All four teams also have `CollisionRule=never`, `AllowFriendlyFire=0`, `DeathMessageVisibility=hideForOtherTeams`, `SeeFriendlyInvisibles=1`, `NameTagVisibility=always`. **H**.

## 3. XP-level state machine

The whole game is encoded in each player's XP level:

| Level | Meaning | Set by |
|---:|---|---|
| 8 | in sky lobby, team selected, ready | hub pad; `xp set @a[level=14] 8 levels` (CB-14 x929) |
| 9 | countdown / kit handout | `execute as @a[level=8,team=…] run xp set @s 9 levels` (CB-14 x986..989) |
| 10 | Red, alive | `xp set @a[team=Sky_Battle_Red] 10 levels` (CB-14 x942) |
| 11 | Blue, alive | `xp set @a[team=Sky_Battle_Blue] 11 levels` |
| 12 | Green, alive | `xp set @a[team=Sky_Battle_Green] 12 levels` |
| 13 | Yellow, alive | `xp set @a[team=Sky_Battle_Yellw] 13 levels` |
| 14 | dead / winner / spectator / post-game | death handling and win check |

Consequences: **any** XP change (bottles, commands, plugins, `/xp`) breaks team membership, the win check, the timer, and the kit. The win check literally tests `@a[level=10,team=Sky_Battle_Red,gamemode=adventure]` and `unless entity @a[level=10,...team=!Sky_Battle_Red]`. **H** commands; **H** fragility.

## 4. Scores in the snapshot

`POINTS`: Red 50, Green 50, Yellow 0, Blue 0. This is a post-game snapshot: Red and Green both have 50, so the last game may have been won twice (or the score was not reset between tests). **H** values, **M** interpretation.

`death_skybattle`: Caspar_laur 0, daniellaur 0, RebeccaLau 17, Koetjecool 0. The 17 is a running death count from earlier tests; the game resets it to 0 on death handling. **H** values, **M** interpretation.

## 5. 1.21.1 notes

- `CriteriaName` values are stored as strings. `deathCount`, `dummy`, `health`, `playerKillCount` are unchanged in 1.21.1. `minecraft.mined:minecraft.iron_block` is unchanged. `minecraft.custom:minecraft.leave_game` is not Sky Battle-critical.
- `TeamColor=yellow` is a valid 1.21.1 team colour and will be preserved by the world upgrade. Fix it manually if the team colours matter.
- The `Sky_Battle_Yellw` spelling is preserved. Any reimplementation must either keep the misspelling in every selector or rename both the team and all 11+ commands that reference it.

