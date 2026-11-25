# Pack -> map matrix

All counts are **placed block states** decoded from the staged region files, not palette entries (**H**).
`own` = the world that ships and would use the pack; `other` = same block placed elsewhere, where
the vanilla texture is used because those worlds do not ship that pack (**H** for placement, **M** for the
"vanilla texture there" inference).

| Pack | Pack world | Pack format | Files | Bytes | Placed blocks using an overridden texture | Distinct overridden blocks placed |
|---|---|---|---|---|---|---|
| Clouds 2 | `mcc_clouds2` (1.20.1) | 15 | 83 | 22,642,170 | 11,152 | 24 of 28 |
| Yeti Set Go | `mcc_yetisetgo` (1.19.2) | 9 | 23 | 368,223 | 584 | 4 of 6 |
| Space Race | `mcc_spacerace` (1.18.1) | 6 | 39 | 165,841 | 28,037 | 20 of 25 |

## Overridden-block usage, Clouds 2

| Block | Clouds 2 | Yeti | Space Race | City Marathon | TGTTOS Badlands | TGTTOS Industry | TGTTOS Pits |
|---|---|---|---|---|---|---|---|
| magenta_glazed_terracotta | 46 | 23 | 723 | 16 | 0 | 0 | 0 |
| pink_glazed_terracotta | 181 | 92 | 0 | 64 | 0 | 0 | 0 |
| red_glazed_terracotta | 181 | 92 | 0 | 64 | 0 | 0 | 0 |
| yellow_glazed_terracotta | 595 | 377 | 0 | 651 | 0 | 0 | 0 |
| purple_glazed_terracotta | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| sea_lantern | 6 | 6 | 6,831 | 824 | 94 | 9 | 0 |
| diorite | 222 | 0 | 0 | 0 | 2,035 | 0 | 47 |
| oak_planks | 1,189 | 0 | 155 | 0 | 0 | 0 | 0 |
| red_nether_bricks | 131 | 0 | 171 | 0 | 0 | 0 | 0 |
| red_stained_glass | 42 | 0 | 284 | 0 | 0 | 0 | 0 |
| soul_sand | 448 | 16 | 0 | 0 | 778 | 0 | 0 |
| glowstone | 0 | 0 | 0 | 0 | 0 | 0 | 8 |
| 16 terracotta colours | 8,111 (Clouds 2 pack) | 192 (vanilla texture) | 0 | 12,604 (vanilla texture) | 0 | 0 | 0 |

The Clouds 2 pack is active only in `mcc_clouds2`; the same blocks in City Marathon/TGTTOS use vanilla textures. Yeti does not ship the terracotta set at all.

## Space Race blocks actually visible in `mcc_spacerace`

| Block | Count | Asset note |
|---|---|---|
| stone_bricks | 9,177 | grey solid brick |
| sea_lantern | 6,831 | shared exact file with Clouds 2 |
| glass | 1,678 | fully transparent |
| light_blue_stained_glass | 1,690 | blue transparent grid |
| light_gray_wool | 1,508 | flat light grey |
| smooth_stone | 1,351 | light grey panel |
| gray_wool | 873 | dark grey |
| orange_wool | 821 | orange |
| blackstone | 752 | flat red |
| magenta_glazed_terracotta | 723 | orange/yellow chevron; speed pad |
| black_wool | 226 | near-black |
| jungle_planks | 512 | tan brick |
| red_concrete | 477 | red grid; levitation/speed trigger |
| yellow_wool | 314 | yellow |
| white_stained_glass | 522 | white grid |
| red_stained_glass | 284 | red grid |
| diamond_block | 130 | cyan panel; elytra trigger |
| end_stone | 90 | yellow |
| birch_trapdoor | 65 | white/black panel |
| iron_bars | 13 | grey lattice |
| acacia_planks, birch_planks, crimson_planks, dark_oak_planks, blackstone_top | 0 | unused in this world |

## Sound usage

Only Clouds 2 ships sounds. 44 `playsound` commands, all vanilla IDs:
18 × `block.bell.use`, 6 × `block.candle.extinguish`, 6 × `block.cave_vines.break`,
6 × `block.chain.break`, 6 × `block.chain.step`, 1 × `block.brewing_stand.brew`,
1 × `music_disc.far`. Zero commands reference `acerace.*`.
