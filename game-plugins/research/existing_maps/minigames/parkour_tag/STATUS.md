# STATUS — A6b Parkour Tag (`MG-PT-01`)

Updated: 2026-11-25 (A14 QA fixes)
Owner: A6b. Folder: `game-plugins/research/existing_maps/minigames/parkour_tag/` only.
Source: `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3` (1.17.1, DataVersion 2730).
Live world: `mcc_minigames` (not touched; no RCON, no restart, no edits).

## Done

- Read A6’s plan, README, STATUS, `data/arenas.csv`, `command_clusters.csv`, `command_blocks.csv`; confirmed the 123-command / 25-sign counts.
- Extracted the MG-PT-01 subset: 8,375 blocks, 123 commands, 25 signs; full sign NBT with colour/glow.
- Decoded the scoreboard: `death_parkourtag` = deathCount, `kill_parkourtag`/`kill_parkourtag2` = playerKillCount, `timer_parkourtag` = dummy; teams `pve` (friendly fire off) and `Parkour_Tag`.
- Rebuilt the envelope, arena box, two start rooms, gallery deck, underground waiting lobby, command machine and banner bounds.
- Measured the course: 666 standable cells in the arena box, 242 at ground `y 11`; five tiers (y11–25); exact platform bounds in `README.md` §3 and `data/route.md`.
- Mapped the game loop: XP state 4/5/6/7; role pads; 1–4 runner buttons; countdown glass doors; 60 s bossbar; Strength 100 / Glowing; deathCount/playerKillCount tag; three win conditions; reset.
- Exported `data/commands.csv` (123 rows), `data/command_blocks.csv` (canonical-name alias of `commands.csv`; same 123 rows), `data/signs.csv` (25), `data/checkpoints.csv` (13 state transitions), `data/blocks.csv` (68 types), `data/entities.csv` (none), `data/containers.csv` (1 storage barrel + 2 decorative lecterns = 3 container block entities; all empty).
- Produced and opened the evidence set: full top, 2 full iso, course top/iso, runner/hunter spawn close-ups, 2 cutaways, low/high surface maps, 4 band plans, route schematic; copied 16 images to `frames/`.
- Compared read-only with `game-plugins/ParkourTag` (`ParkourTagGame.java`, `ArenaBlueprint.java`): format, tag model, runner vision, presentation, portability, reusable ideas.
- Wrote 1.21.1 risk table: six firework commands are HIGH risk (old item NBT), one broken tellraw, team-split risk, sign NBT LOW–MED, other commands LOW.
- **A14 QA pass:** added the two gallery lecterns at `1049 28 993/996` to `containers.csv`/README/STATUS; corrected the container definition to “1 storage barrel + 2 decorative lecterns = 3 container block entities; all empty”; added `data/command_blocks.csv` as the canonical-name alias of `commands.csv`; re-opened all 16 cited `frames/` images (0 missing citations).

**Data-quality note (2026-11-25, A14 QA):** the earlier “A6 0 containers” correction is superseded. A6 now reports **13 Sky Battle + 1 Parkour Tag** storage container, and A14’s source scan lists **34** container block entities world-wide (21 empty, 13 Sky Battle loot). For MG-PT-01, A14 finds **1 storage barrel + 2 decorative lecterns = 3 container block entities; all empty** (`Items: []`). Barrel `1067 12 992`; lecterns `1049 28 993` / `1049 28 996`; the lecterns each hold a written guide book, not loot. Source: `_qa/data/minigames_container_by_arena.csv`. **H**

## Blocked

- None for the research. No live-server observation was used, so the runtime state of the `pve`/`Parkour_Tag` split and the exact bossbar audience are not proven; they are marked **M** and listed as open questions.

## Open questions

1. **Does the runtime team split work?** `PT-C060` places a redstone block at `1057 6 1001` at round start; it should power the repeater at `1056 6 1001`, the `team empty Parkour_Tag` block at `1055 6 1001`, and the `team join Parkour_Tag @a[level=6]` chain at `1054 6 1001`. `PT-C072/073` remove the block at reset. The static export cannot show the runtime block; if it is missing or the repeater faces the wrong way, all players stay on `pve` (friendly fire off) and tags fail. **H** commands, **M** runtime. Needs a read-only live test.
2. **Does the bossbar get its players?** The player selector is `x 1057..1065, y 15..20, z 979..1011`; the top is `y 23..25` and the E/W edges are outside. The list is static and set by an impulse in the start chain; if it runs before the start-room teleports, it adds nobody. **H** selector, **M** trigger order/effect.
3. **Is the SE pit room a hiding spot or a trap?** It has a high iron door (`y 18..19`), a floor 7 blocks below, and a pressure plate at `1070 14 1003` on the wall. **H** geometry, **M** intent.
4. **Are the start-tower roofs reachable?** They are standable at `y 21` but isolated by a 4-block vertical gap; no ladder/stairs connect them. **H** geometry, **M** intent.
5. **Is the map one round or best-of?** The commands implement one 60 s round + reset; there is no round counter/score. **H** commands.
6. **Live sign count:** A6/A12 confirm 123 command blocks for this cluster; the live 25-sign count was not independently confirmed. **H** source, **M** live.

## Key findings (for A13/A14)

- **Strong presentation, fragile logic.** The stadium/banner/barrier-roof/underground-lobby package is the best presentation idea in this world; the command machine has several concrete defects (broken tellraw, wrong timer-stop selector, bossbar box, old firework NBT, empty chain) and one big runtime dependency (the command-placed team-split redstone at `1057 6 1001`).
- **The arena is a 20 × 20 climbing frame with four raised tiers + ground**, not a checkpoint course. 666 standable cells, 242 at ground level; the top is three usable perches + one grandstand (the NW brick roof is decorative), not a loop.
- **The lobby lecterns carry the rules.** Both lecterns hold a written book “Parkour Tag” by daniellaur: the game ends when every runner is tagged within a minute, and the hunter can start by pressing “the right button”. This supports the command-loop reading. **H** NBT; **M** which button “right” means.
- **Glowing runners invert MCC’s runner-vision design.** The hunter sees runners through walls; the four “hiding” rooms are tactically irrelevant.
- **The map detects a tag as a kill** (Strength 100 + deathCount/playerKillCount). The current plugin’s event-based, damage-cancelled tag is cleaner and death-free.
- **Reusable:** the 20 × 20 tier template, the barrier-as-glass-floor gallery, the role pads + 1–4 runner buttons, the glass-door countdown, the cyan/copper/dead-coral palette, the 60 s bossbar styling.

## Files

```
README.md, REFERENCE_IMAGES.md, STATUS.md
frames/            16 images (full, course, spawns, cutaways, height maps, bands, route)
renders/           MCMDUMPs + raw render outputs
data/commands.csv, command_blocks.csv, signs.csv, checkpoints.csv, blocks.csv, entities.csv, containers.csv
data/route.md, palette.md
data/pt_blocks.csv, pt_commands.csv, pt_signs.csv, signs_full.csv
data/*.py          extraction / analysis / export scripts (read-only source)
```

## Confidence

- **H:** bounds, block counts, sign text/colour/glow, command strings, scoreboard criteria, gamerules, standable-cell counts, 1.21 syntax changes.
- **M:** route/tactical judgements, pause/team runtime, hiding intent, readability.
- **L:** none used for a load-bearing claim.
