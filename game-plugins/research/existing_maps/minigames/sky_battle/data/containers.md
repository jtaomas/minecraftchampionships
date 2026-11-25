# Container block entities — MG-SB-01 (19 total)

**19 container block entities = 13 loot containers (93 items) + 5 campfires + 1 lectern; all non-loot are empty.**

Re-read from `game-plugins/research/existing_maps/_qa/data/minigames_container_by_arena.csv` on 2026-11-25. The QA file has exactly 19 `MG-SB-01` rows. The 13 loot containers are also expanded in `data/chests.csv`; the 5 campfires and 1 lectern have no items (`Items: []`).

## Complete list

| ID | Block | Block entity | x | y | z | Loot? | Empty? | Stacks | Role |
|---|---|---|---:|---:|---:|---|---|---:|---|
| SB-C01 | white shulker box | `minecraft:shulker_box` | 1965 | 24 | 0 | yes | no | 9 | Red team spawn shulker |
| SB-C02 | white shulker box | `minecraft:shulker_box` | 2035 | 24 | 0 | yes | no | 9 | Green team spawn shulker |
| SB-C03 | white shulker box | `minecraft:shulker_box` | 2000 | 24 | 35 | yes | no | 9 | Blue team spawn shulker |
| SB-C04 | white shulker box | `minecraft:shulker_box` | 2000 | 24 | -35 | yes | no | 9 | Yellow team spawn shulker |
| SB-C05 | chest | `minecraft:chest` | 1974 | 26 | -26 | yes | no | 9 | Orange balloon crate chest |
| SB-C06 | chest | `minecraft:chest` | 1974 | 26 | 26 | yes | no | 9 | Cyan balloon crate chest |
| SB-C07 | chest | `minecraft:chest` | 2026 | 26 | -26 | yes | no | 9 | Lime balloon crate chest |
| SB-C08 | chest | `minecraft:chest` | 2026 | 26 | 26 | yes | no | 9 | Purple balloon crate chest |
| SB-C09 | barrel | `minecraft:barrel` | 1974 | 14 | -26 | yes | no | 3 | Orange balloon bottom barrel |
| SB-C10 | barrel | `minecraft:barrel` | 1974 | 14 | 26 | yes | no | 3 | Cyan balloon bottom barrel |
| SB-C11 | barrel | `minecraft:barrel` | 2026 | 14 | -26 | yes | no | 3 | Lime balloon bottom barrel |
| SB-C12 | barrel | `minecraft:barrel` | 2026 | 14 | 26 | yes | no | 3 | Purple balloon bottom barrel |
| SB-C13 | barrel | `minecraft:barrel` | 2000 | 29 | 0 | yes | no | 9 | Mid platform barrel |
| SB-E01 | campfire | `minecraft:campfire` | 1974 | 29 | -26 | no | yes | 0 | Orange balloon campfire |
| SB-E02 | campfire | `minecraft:campfire` | 1974 | 29 | 26 | no | yes | 0 | Cyan balloon campfire |
| SB-E03 | campfire | `minecraft:campfire` | 2000 | 63 | 0 | no | yes | 0 | Sky-lobby stem campfire |
| SB-E04 | campfire | `minecraft:campfire` | 2026 | 29 | -26 | no | yes | 0 | Lime balloon campfire |
| SB-E05 | campfire | `minecraft:campfire` | 2026 | 29 | 26 | no | yes | 0 | Purple balloon campfire |
| SB-E06 | lectern | `minecraft:lectern` | 2000 | 77 | -1 | no | yes | 0 | Sky-lobby lectern |

## Reconciliation

- **Loot containers:** 13 — `data/chests.csv` (4 white shulker boxes, 5 barrels, 4 chests), **93 items** in `data/loot.csv` / `data/container_nbt.csv`.
- **Non-loot:** 6 — 4 balloon campfires at y29, 1 sky-lobby stem campfire at y63, 1 sky-lobby lectern at 2000 77 -1. All empty.
- **Total:** 19 container block entities. **H** (QA CSV + source NBT).
