# Contradiction resolution — Super Clash and Sky Race

Scope: two known disagreements in the minigames index. §1 resolves the Super Clash villain objective
(`kill_sc_vip` vs `kill_sc_vp`); §2 resolves the Sky Race Red vs Orange "same layout" claim. Research only;
A6c's files were not edited.

## Ruling

**The objective is `kill_sc_vp`.** The string `kill_sc_vip` does not exist in any command, scoreboard file or
source report in `game-plugins/research/existing_maps/` (a literal search of the source dossiers returned zero
matches). It is a transcription error for `kill_sc_vp` and occurs in this comparison only as the quoted error.

**The criterion is `teamkill.green`.** That is the real logic bug: villains join `Super_Clash_V`, not
`Green`, so the objective is unlikely to increment for villain kills.

## Enclosing command chain (decoded)

From the source world's command blocks (`minigames/data/command_blocks.csv`, rows at x=4987/4989, y=20/24):

```
# reset at lobby level 0
(-205,19,965)  scoreboard players set @a[level=0] kill_sc_vp 0

# villain kills -> counter -> win check
(4987,24,-2)   execute as @a[team=Super_Clash_V,scores={kill_sc_vp=1}] run scoreboard players add @a kill_sc_v 1
(4987,24,-3)   execute as @a[team=Super_Clash_V,scores={kill_sc_vp=1}] run scoreboard players add Villains_Kills Super_Clash 1
(4987,24,-4)   execute as @a[team=Super_Clash_V,scores={kill_sc_vp=1}] run scoreboard players set @s kill_sc_vp 0
(4987,20,-6)   execute as @a[team=Super_Clash_V,scores={kill_sc_v=10}] at @s run xp set @s 22 levels
(4987,20,-2)   execute as @a[team=Super_Clash_V,scores={kill_sc_v=10}] at @s run tellraw @s {...The Villains win!...}
```

The win check reads **`kill_sc_v`** (a dummy counter), which is fed by **`kill_sc_vp`**. So the naming answer
and the logic answer are different layers:

- objective name: `kill_sc_vp` — correct spelling;
- criterion: `teamkill.green` — wrong team;
- feeder: `scoreboard players add @a kill_sc_v 1` — also adds to every player, not just the killer, so after
  10 villain kills every villain has `kill_sc_v=10` and the whole team is treated as a winner.

## Sources (H unless marked)

1. `../minigames/other/data/scoreboard_objectives.csv` line 19:
   `kill_sc_vp,teamkill.green,...,MG-SC-01,"Criteria teamkill.green; villains join Super_Clash_V, not Green. Likely broken."`
2. `../minigames/other/README.md` §2.2, "The suspect objective": `kill_sc_vp` is `teamkill.green`;
   `kill_sc_hp` is `playerKillCount` for heroes; villains join `Super_Clash_V`, not `Green`. **H** criteria;
   **M** that villain kills do not count.
3. `../minigames/README.md` §5 (Super Clash): names the objective `kill_sc_vp` and the criterion
   `teamkill.green`.
4. `../minigames/STATUS.md` line 65 and `../minigames/data/arenas.csv` row `MG-SC-01`: same finding.
5. `../minigames/other/data/commands.csv` row `MG-SC-01`: the 165-command summary records the same bug.
6. Command-block evidence: `../minigames/data/command_blocks.csv` rows `(4987,24,-2..-4)` and
   `../minigames/data/tile_entities.csv` rows 984/1007/1010. **H** exact strings.
7. Teams: `../minigames/other/README.md` §2.2 — villains join `Super_Clash_V`; the `Green` team exists but is
   unused. **H**.

## What A13 does with it

Use `kill_sc_vp` in any comparison table, with a footnote that its criterion is broken. Do not "fix" the
name to `kill_sc_vip`; do not reuse the chain as-is. If the class-PvP loop is ever lifted, the correct model
is an explicit per-player kill event with team attribution, not a `teamkill.<other-team>` criterion.

---

## 2. Sky Race Red vs Orange — not the same layout

**Ruling: A6c is correct; A6's "same layout" is superseded.** Red and Orange share the start tower, the finish
basket and a repeated gate rhythm, but the middle is structurally different.

| Map | Vertical range | Signature structure | Blocks / visible | Evidence |
|---|---:|---|---|---|
| Red (`MG-SR-RED-01`) | **y13–82** | 98 chains and grey cloud/branch props in the upper half | 23,115 total / 2,567 renderable | `../minigames/other/README.md` §2.4; H |
| Orange (`MG-SR-ORG-01`) | **y13–54** | 24 cobwebs, no chains and no grey cloud props; low flat platform run | 22,869 total / 2,272 renderable | `../minigames/other/README.md` §2.5; H |

A6's claim is at `../minigames/README.md` §5b, cluster table row `MG-SR-ORG-01` ("Orange course (same
layout)"). A6c directly corrects it in `../minigames/other/README.md` §4 row 2, using
`blocks_positions.csv` and `frames/srred_iso.png` vs `frames/srorg_iso.png`. **H** for the geometry, **M** for
the "easy variant" design intent.

**Consequence for synthesis:** do not treat Orange as a palette reskin of Red. If either Sky Race course is
used as a reference, carry the different vertical layout and hazard set, not just the colour swap.
