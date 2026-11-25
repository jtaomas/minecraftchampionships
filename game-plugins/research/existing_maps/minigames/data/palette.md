# Palette notes — `mcc_minigames`

Counts are exact block counts from `data/blocks_by_island.csv` (source world, 1.17.1). Percentages are share of that arena's non-air blocks. Evidence: **H** (decoded block data).

## MG-SB-01 Sky Battle — 47,874 blocks
| Block | Count | Share |
|---|---:|---:|
| barrier | 34,751 | 72.6% |
| white concrete | 4,545 | 9.5% |
| stone bricks | 1,231 | 2.6% |
| red concrete | 984 | 2.1% |
| yellow / green concrete | 725 / 725 | 1.5% each |
| blue concrete | 570 | 1.2% |
| blue / red / yellow / green terracotta | 360 / 359 / 359 / 359 | 0.75% each |
| gray wool | 287 | 0.6% |
| smooth quartz | 256 | 0.5% |
| oak fence | 240 | 0.5% |
| orange / cyan / lime / purple wool | 223 each | 0.47% each |
| cobblestone wall | 164 | 0.34% |
| oak leaves | 138 | 0.29% |

Read: a **white-concrete and stone-brick arena shell**, with team identity carried by saturated concrete + terracotta + wool in four quadrants/gift islands; warm wood/leaf accents; a mostly invisible barrier envelope. Lighting is not a palette feature here; the sky lobby uses gray wool + gold/raw gold. **H.**

## MG-PT-01 Parkour Tag — 8,375 blocks
| Block | Count | Share |
|---|---:|---:|
| gray concrete | 1,849 | 22.1% |
| cyan terracotta | 1,073 | 12.8% |
| stone bricks | 708 | 8.5% |
| smooth quartz | 609 | 7.3% |
| white concrete | 509 | 6.1% |
| dead horn coral block | 441 | 5.3% |
| barrier | 401 | 4.8% |
| brick slab | 379 | 4.5% |
| black concrete | 328 | 3.9% |
| deepslate bricks | 298 | 3.6% |
| red concrete | 251 | 3.0% |
| waxed copper / exposed / weathered / oxidized | 151 / 137 / 106 / 57 | 1.8 / 1.6 / 1.3 / 0.7% |

Read: **cool gray + cyan stadium** with quartz trim; the pink-red **dead horn coral** and **brick** accents mark the course; the **waxed copper oxidation family** is a deliberate green-teal gradient on the course structures. High material variety (66 block types) for its size. Lighting: glowstone/sea lanterns are present but not dominant; barrier is used for the invisible course gates. **H.**

## MG-HUB-01 Main lobby — 10,161 blocks
gold block 1,690 (16.6%), gray concrete 1,455 (14.3%), smooth quartz 1,275 (12.5%), raw gold 801 (7.9%), white concrete 710 (7.0%), then six team colours (red, pink, yellow, magenta, blue, lime, orange) at ~200–290 each.

Read: a **gold/quartz crown** on a gray plinth, with one colour per game/team bay. Warm, bright, high-contrast; reads as the social centre. **H.**

## MG-SC-01 Super Clash — 16,492 blocks
light 6,416 (38.9%), tinted glass 1,443 (8.7%), light blue concrete 1,053 (6.4%), deepslate bricks 870 (5.3%), stone bricks 860 (5.2%), grass block 796 (4.8%), stone 743 (4.5%), blue concrete 718 (4.4%), dirt 459 (2.8%), polished blackstone bricks 292 (1.8%), white concrete 262 (1.6%), blackstone 261 (1.6%).

Read: a **lit blue-glass arena**: light blocks are invisible illumination, tinted glass is the roof, light-blue concrete + painted clouds form the sky mural, and a grass/dirt/stone terrain sits inside. Two dark towers and trees sit east of the box. **H.**

## MG-SR-RED-01 / ORG-01 — 23,115 / 22,869 blocks
Red: barrier 20,548 (88.9%), black concrete 870, red concrete 374, red stained glass 325, red terracotta 196, white concrete 177, light-gray concrete 116, chain 98, black stained glass 88, raw gold 69, light-gray wool 48.
Orange: barrier 20,597 (90.1%), black concrete 871, orange concrete 409, orange stained glass 280, orange terracotta 215, white concrete 177, black stained glass 88, raw gold 69, cobweb 24.

Read: **black structural frame + one team colour + raw-gold finish basket**, floating in a barrier envelope. Red and Orange are the same layout with a palette swap; Orange adds cobwebs. **H.**

## MG-SR-HUB-01 Sky Race hub — 902 blocks
stone bricks 201 (22.3%), gray concrete 168 (18.6%), white concrete 161 (17.8%), bricks 148 (16.4%), stone-brick slab 98 (10.9%), brick stairs 30, stone-brick stairs 17, light 12, iron bars 9, six colour accents 6 each.

Read: a small **brick-and-stone kiosk** with six saturated pillars; intentionally neutral so the level colours pop. **H.**

## MG-CMD-01 Command room — 1,533 blocks
white concrete 752 (49.1%), chain command block 335 (21.9%), repeater 154 (10.0%), redstone wire 92 (6.0%), command block 80 (5.2%), repeating command block 59 (3.8%), birch wall sign 57 (3.7%), redstone block 2, sticky piston 2.

Read: pure **service architecture** — a white test platform carrying the redstone/command machine. No decorative palette. **H.**

## MG-REB-01 / MG-REB-02 — 309 / 1,400 blocks
Kiosk: white concrete 151, black stained glass 121, black concrete 24, quartz 4, quartz stairs 3, sea lantern 1.
Course: white concrete 1,030 (73.6%), lava 218 (15.6%), stone 66, waxed copper 52, yellow carpet 24, quartz 4+3, sea lantern 1.

Read: **white concrete over lava**, with waxed copper used as a jump-boost pad (command confirms) and yellow carpet as the finish marker. **M** game identity; **H** palette.

## Cross-arena observations
- **Barrier is the dominant "material"** (55,000+ blocks across SB + SR + PT). Any block-count or palette analysis that excludes barrier will misrepresent these arenas.
- **Team colour is consistent**: red / yellow / green / blue appear as concrete + terracotta + wool, with white concrete as the neutral.
- **Container block entities exist**: **34** total — 11 barrels, 6 campfires, 6 chests, 5 lecterns, 6 shulker boxes; **21 empty**, **13 Sky Battle loot** (93 items). Full list: `data/containers.csv`. Do not assume all are decorative; see A6a/A6b.
- **No custom resource-pack blocks**: every id is vanilla 1.17.1. **H.**
