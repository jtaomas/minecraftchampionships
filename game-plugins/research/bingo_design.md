# Bingo / Lockout: community design research (feedback round 1)

Researched 2026-11-24 for the McmBingo plugin (Paper 1.21.1, plugin only, no client or server mods).
Confidence notes: **[read]** means I read the project's own source or data files, **[listing]** means I only saw a store page or a search summary, **[inferred]** means my own reading of vanilla behaviour that I did not test on a server.

## 1. Projects looked at, with licences

| Project | Kind | Licence (as written) | Used how |
|---|---|---|---|
| **Fetchr** (NeunEinser/bingo, GitHub; Modrinth/CurseForge "Fetchr Bingo") | Datapack + map, the best-known bingo map | **MIT** ("Copyright © 2023 NeunEinser", LICENCE file) [read] | Item pool and its **categories** (mutual exclusion groups) copied as the base of our pool; its per-item category weights inspired our weighting. |
| **Extremelyd1/minecraft-bingo** (GitHub, Paper plugin, 43 stars) | Plugin | **GPL-3.0** (LICENSE file) [read] | **Tier lists S/A/B/C/D**, card distribution and item groups recorded below as data; used as the reference for our difficulty tiers. GPL allows reuse but a jar that embeds their files must be GPL too, so we did not copy their files into the jar: our tiers are our own assignments, checked against theirs. |
| **BingoReloaded** (Steaf23, GitHub/Hangar/Modrinth) | Plugin (needs PacketEvents) | **GPL-3.0** [read: GitHub licence field] | Not copied; modes (Regular, Lockout, Complete-all) match ours. |
| **UltimateBingo** (shantek, GitHub) | Plugin | **GPL-3.0** [listing] | Five difficulty names (easy, normal, hard, extreme, impossible) [listing]. Not copied. |
| **Lockout Bingo** (CurseForge/Modrinth mod) | NeoForge/Fabric mod | not stated on the pages seen [listing] | Not usable (mod). Its goal list lives in a public spreadsheet with an easy/normal/hard/random difficulty filter [listing]. Not copied (no licence). |
| chalkinshmeal/lockout (Hangar) | Paper plugin | MIT [listing] | Task-based lockout, no difficulty tiers [listing]. Not used. |

Raw files (data only, read as text, never loaded into a server) are in the session scratchpad under `dl/bingo/fetchr/` and `dl/bingo/extremelyd1/`.

## 2. How the good projects build a card

### Fetchr (MIT) [read: `datapack/data/fetchr/function/init/fill_registries.mcfunction`, `docs/modifying_item_pool.md`]
- 209 item entries (current develop branch; it already contains items from Minecraft versions after 1.21.1, such as pale garden, cactus flower, bush, harness, poplar; we skip those).
- Every item lists one or more **categories** (for example `jungle`, `warm_biome`, `village`, `lush_cave`, `skeleton_drops`, `gunpowder`, `paper`). "All items within this category are mutually exclusive, meaning for each category at most one item of that category can appear on a card at once."
- Each item has a **weight** within its category (default 1; odd items get more, for example dead bush weight 6 in `warm_biome`, eyeblossom 3), "Higher weight means more likely". The 5.0 release notes say this "promotes more unusual items".
- Categories carry `tags` (`fetchr:default`) so whole item pools can be switched.
- No difficulty tiers: difficulty is controlled by the pool itself (everything is early-game survival).
- QoL in the map: card shown on the HUD, coordinates, a "teleport to spawn" item, a pre-generated spawn, spectator mode, seeded cards (same seed = same card).

### Extremelyd1/minecraft-bingo (GPL-3.0) [read: `src/main/resources/item_data/*.txt`, `config.yml`, `BingoBootstrap.java`, README]
- Five tiers, hardest first: **S (261 entries), A (522), B (260), C (221), D (146)**, plus a blacklist (200) and **groups** (150 lines; one group per line, "items in a group never appear together", for example `APPLE|GOLDEN_APPLE|ENCHANTED_GOLDEN_APPLE`, `CARROT|GOLDEN_CARROT`, all wood of one species in one group).
- Card = `default-item-distribution: "2,6,9,6,2"` = 2 S, 6 A, 9 B, 6 C, 2 D. Changeable with `/itemdistribution <S> <A> <B> <C> <D>`. Picking inside a tier is uniform random, one item per group.
- Defaults: 30-minute timer, 3000-block overworld border, 1 line to win, lockout mode available, `give-all-recipes: True`, `prevent-water-spawns: True`, a 5 s freeze at start, an optional **start kit** (default `minecraft:cooked_beef 20`, disabled by default).
- QoL commands: `/bingo` (view card), `/card` (new card item if lost), `/reroll`, `/coordinates [message]` to team, `/all`, `/teamchat`, `/pvp` toggle, `/wincondition full|lines|lockout`, `/timer`.
- **Biome size**: an optional "small biomes" datapack (`datapacks/biome-size`) that overrides `minecraft:worldgen/noise/{temperature,vegetation,continentalness,erosion}` with higher `first_octave` values (temperature -8 vs vanilla -10, vegetation -7 vs -8, continentalness -8 vs -9, erosion -9 vs -9) so climate changes 2-4x faster. It is registered through a Paper **bootstrapper** (`paper-plugin.yml`, `DatapackRegistrar`) at server start, so it applies to every world on the server [read].

### BingoReloaded / Lockout mod / UltimateBingo [listing]
- BingoReloaded: Regular, Lockout and Complete-all modes, item lists and "card" GUI, needs PacketEvents.
- Lockout Bingo mod: 2 teams race to 13 of 25 goals; goal difficulty filter easy/normal/hard/random.
- UltimateBingo: 5 difficulty classes.

### Obtainability in these projects
- Fetchr ships its own world generation (dimension + noise settings in the pack) and curates the pool to things that exist in "normal" survival; its categories group items by source (biome, structure, mob), which is exactly the information needed to filter by availability. [read]
- Extremelyd1 relies on a large border (3000) and a blacklist of items that are too rare. [read]
- None of them filter the card by what the generated world actually contains. We do (section 5).

## 3. Our pool (what we took)
- Base list and exclusion **groups**: Fetchr's categories (MIT), filtered to items that exist in 1.21.1 and that our world can provide (no Nether, no End, no slime-only items because time is locked to day and swamp slimes need night).
- **Tiers** (easy / medium / hard / very hard): our own assignment, checked against Extremelyd1's S..D tiers (GPL, used as a reference only). Rough mapping: D and C → easy, B → medium, A → hard, S (only the obtainable ones) → very hard, moved one tier up or down where our 10-minute game with Speed I and iron tools differs from their 30-minute default.
- **Card mix**: Extremelyd1's idea of a per-tier count (their 2/6/9/6/2 for 30 min). Ours (10 and 12 min) default to Bingo 8 easy / 10 medium / 5 hard / 2 very hard and Lockout 6 / 10 / 6 / 3, in `config.yml` (defaults, not researched).
- **Weighting**: Fetchr style. One item per group per card; inside a tier, biome-, structure- and mob-specific items get weight 2 and generic items weight 1, so the odd items show up more often.
- **Wood-only cap**: at most 2 squares per card that can be made from wood alone (owner complaint), configurable.

## 4. World generation: many small biomes in a 1200-block border, and more caves

### Options
1. **World-gen datapack** (custom noise `first_octave`, custom `noise_settings` with more caves, or a custom `multi_noise` biome source in a dimension file).
   - On Paper, datapacks are server-wide (loaded from the main world's `datapacks` folder or a bootstrapper). Overriding `minecraft:worldgen/noise/*` changes every vanilla-generated world, including any normal lobby world [inferred].
   - A custom `noise_settings`/dimension only applies to a dimension defined in the datapack; `WorldCreator` in 1.21.1 cannot point a plugin world at a datapack `noise_settings` or dimension type. So "for that world only" is not possible without NMS [inferred from the 1.21.1 API: no such method].
   - It needs a server restart and a file in the server folder outside our plugin folder (not allowed for this task), and the Extremelyd1 route needs converting the plugin to a Paper plugin with a bootstrapper.
   - Even with small biomes, the biome **set** stays random (the owner's seed was "ice spikes and taiga"), so the plugin still would not know which biomes exist without sampling.
2. **Public world-gen datapack or plugin** (Terralith, Tectonic, Biome-size packs). Same server-wide problem; Terralith/Tectonic make biomes larger and more varied, not smaller; plugin generators (Terra, Iris) are big external plugins with their own configs. Not chosen.
3. **Custom `BiomeProvider` on the `WorldCreator`, vanilla chunk generator** (chosen).
   - Paper wraps it in `CustomWorldChunkManager` and creates the vanilla noise generator with it [read: Paper `CraftWorld.vanillaBiomeProvider` and `CustomWorldChunkManager` source, ver/1.21.4 branch; same design since 1.18].
   - Terrain shape (height, oceans, rivers, noise caves, aquifers) still comes from the vanilla noise router, which does not read the biome. Surface rules (sand, terracotta, snow, mycelium), features (trees, flowers, ores, lush-cave plants), mob spawns and structure biome checks all follow the biome we return [inferred from 1.18+ worldgen design].
   - Paper passes the climate point (`BiomeParameterPoint`: continentalness, erosion, depth, weirdness...) to `getBiome`, so the provider can see where vanilla would put ocean, river or underground, and `World#vanillaBiomeProvider()` gives the vanilla answer for any position [read: API javap].
   - Works per world, needs no restart and no files outside the plugin.

### Chosen design
- Each game area (1200x1200, 4096 blocks apart) gets a **cell layout** (Voronoi with a light domain warp) of **13 chosen surface biomes**: 4 inner cells meeting at the start point, 9 outer cells reaching the border. One biome is drawn from each of 11 families (plains/meadow, oak or birch forest, dark forest, taiga, desert, badlands, snowy, jungle/bamboo, swamp/mangrove, cherry grove or mushroom fields, savanna) and 2 extras.
- Water stays where vanilla terrain has it: where the vanilla biome is ocean, river or beach, the provider returns the matching water biome, recoloured to the cell's climate (frozen, cold, lukewarm, warm), so fish, kelp, squid, sand and clay exist and land is never relabelled as sea.
- Underground (depth > 0.2) one inner cell has **lush caves** and one outer cell **dripstone caves** below it; deep dark is never returned (no ancient cities).
- The plugin samples its own layout on a 24-block grid before the game (async, from the vanilla noise only, no chunk loads) and knows exactly which biomes exist and roughly where.

### Structures
- Vanilla structure placement uses spacing grids larger than a 1200 border (village 34 chunks, temples 32, ruined portal 40) [inferred from vanilla structure sets], so a given structure is often missing. Mineshafts are per-chunk (about 0.4 % of chunks, so ~20 inside a border) and can be relied on.
- We therefore place a small fixed set ourselves with `/place structure` (the vanilla command, run by the plugin during the preparing phase, one per tick, only in loaded chunks): one village (type matching its cell), a desert pyramid (if desert), a jungle pyramid (if jungle), a ruined portal near the start, a beached shipwreck and a buried treasure (if a beach exists, otherwise a shipwreck in the ocean). Vanilla may add more; the card only counts on the ones we placed plus mineshafts.

### Caves
- Cave density is set by the noise router and carvers, which only a datapack can change. Without one, we add our own worm caves in a world `BlockPopulator` (runs on the world-gen threads, per chunk, deterministic from the seed): about 6 % of chunks start a tunnel system (configurable), a third of them start above the surface and dig down (cave entrances). They only replace natural ground blocks and skip any spot next to water or lava.
- A **Cave Finder** compass in the kit scans the loaded chunks around the player (chunk snapshots taken 2 per tick, scan on an async thread) for a sizeable underground air pocket and points to it.

## 5. QoL commands we copied
- `/top` (common in speedrun and lockout servers): teleport to the surface when underground; cooldown and combat block.
- Card item that opens the card (Fetchr/BingoReloaded), teleport to spawn and to teammates (already ours), start kit with food (Extremelyd1's cooked beef idea, owner asked for a full stack).

---

## Appendix A: Fetchr item pool (MIT), item | categories | weight if not 1
Extracted from `fill_registries.mcfunction` (develop branch). Empty item ids are entries defined by components (maps, potions).

```
pumpkin_seeds | pumpkin
pumpkin_pie | pumpkin, egg
egg | egg
cake | milk, egg
acacia_sapling | warm_biome | w6
spruce_sapling | taiga
cocoa_beans | jungle | w3
cookie | jungle | w3
melon_slice | jungle | w2
glistering_melon_slice | jungle | w2
dead_bush | { id: shearable, weight: 2 }, { id: warm_biome, weight: 6 } | w26
dead_bush | shearable, { id: warm_biome, weight: 3 } | w3
fern | { id: shearable, weight: 2 }, { id: taiga } | w2
vine | shearable
rail | rail
powered_rail | gold_redstone
flint | flint
flint_and_steel | flint
arrow | skeleton_drops | w2
ender_pearl | nighttime_mob_drops | w6
slime_ball | nighttime_mob_drops | w2
slime_ball | swamp, { id: nighttime_mob_drops, weight: 2 } | w2
slime_ball | swamp, { id: nighttime_mob_drops, weight: 3 } | w3
firework_rocket | gunpowder | w2
milk_bucket | milk
cauldron | basic_iron
carrot | village
golden_carrot | village
painting | wool
item_frame | leather
emerald | village | w3
 | lodestone, compass
diamond_hoe | diamond
cactus_flower | warm_biome | w6
axolotl_bucket | lush_cave | w3
flower_pot | clay
brick | clay
mushroom_stew | mushroom
mushroom_stew | mushroom | w3
mangrove_propagule | mangrove
apple | oak
golden_apple | oak
golden_shovel | gold | w2
golden_sword | gold | w2
clock | gold_redstone
repeater | redstone
compass | compass
map | compass
book | paper
writable_book | paper
enchanted_book | diamond | w1
spider_eye | spider
fermented_spider_eye | spider
gunpowder | gunpowder | w2
tnt_minecart | gunpowder
hopper | chest_iron
hopper_minecart | chest_iron
furnace_minecart | furnace
chest_minecart | chest_iron
bone | skeleton_drops | w2
ink_sac | ink
gray_dye | ink
green_dye | warm_biome | w2
lime_dye | warm_biome | w2
lapis_lazuli | lapis
cyan_dye | lapis | w2
purple_dye | lapis | w2
suspicious_stew | mushroom
suspicious_stew | mushroom | w3
glass_bottle | sand
cod | fish
cod_bucket | fish
salmon | fish
salmon_bucket | fish
tropical_fish | fish
tropical_fish_bucket | fish
firefly_bush | swamp | w3
firefly_bush | swamp | w2
dark_oak_sapling | oak
lead | nighttime_mob_drops | w2
lead | swamp, { id: nighttime_mob_drops, weight: 2 } | w2
lead | spider | w2
detector_rail | rail
activator_rail | rail
heart_of_the_sea | shipwreck
glow_berries | lush_cave | w3
sweet_berries | taiga
short_dry_grass | shearable, { id: warm_biome, weight: 3 } | w3
diamond_pickaxe | diamond | w1
golden_axe | gold | w2
 | shipwreck
bush | shearable | w2
crossbow | bow
bamboo | jungle | w3
sea_pickle | warm_biome | w2
seagrass | shearable | w2
rabbit_hide | rabbit
cooked_rabbit | rabbit
dried_kelp | kelp
 | { id: snowy_biomes }, { id: skeleton_drops }, { id: nighttime_mob_drops, weight: 3 } | w3
glow_ink_sac | glow_ink
glow_item_frame | glow_ink, leather
pointed_dripstone | dripstone_cave
hanging_roots | lush_cave
glow_lichen | shearable | w2
amethyst_shard | amethyst
spyglass | amethyst
 | shipwreck
 | shipwreck
acacia_hanging_sign | basic_iron | w3
brush | copper | w3
cherry_chest_boat | cherry_grove
pink_petals | cherry_grove
cherry_sapling | cherry_grove
carrot_on_a_stick | village
baked_potato | village | w3
jungle_sapling | jungle | w6
light_blue_bundle | leather | w2
armadillo_scute | armadillo
wolf_armor | armadillo
 | { id: skeleton_drops }, { id: nighttime_mob_drops, weight: 3 } | w3
 | { id: skeleton_drops, weight: 1 }, nighttime_mob_drops, swamp | w3
 | skeleton_drops, { id: nighttime_mob_drops, weight: 3 }, { id: swamp, weight: 2 } | w32
closed_eyeblossom | pale_garden | w3
open_eyeblossom | pale_garden | w3
resin_clump | pale_garden, nighttime_mob_drops | w2
pale_hanging_moss | { id: pale_garden, weight: 6 }, { id: shearable, weight: 2 } | w62
copper_lantern | copper | w3
yellow_harness | leather | w2
saddle | leather | w2
iron_spear | basic_iron | w3
 | skeleton_drops, { id: warm_biome, weight: 6 }, { id: nighttime_mob_drops, weight: 3 } | w63
name_tag | paper | w4
golden_dandelion | gold | w6
nether_brick | ruined_portal
prismarine_crystals | shipwreck
bell | village | w3
poplar_sapling | dappled_forest
red_shrub | dappled_forest
brown_mushroom | mushroom
red_mushroom | mushroom
brown_mushroom | mushroom | w2
red_mushroom | mushroom | w2
magenta_cushion | wool
magenta_cushion | wool, abandoned_camp
buried_treasure_map | shipwreck
abandoned_camp_map | abandoned_camp
iron_block | basic_iron
redstone_block | redstone
bookshelf | paper
obsidian | { id: diamond }, { id: ruined_portal, weight: 2 } | w2
melon | jungle | w2
mossy_stone_bricks | shearable
jukebox | diamond | w2
magma_block | ruined_portal | w2
bone_block | skeleton_drops | w2
blast_furnace | furnace
raw_iron_block | basic_iron
red_bed | wool
target | wheat
snow | snowy_biomes
stonecutter | basic_iron | w3
jack_o_lantern | pumpkin
tnt | gunpowder
lapis_block | lapis
dispenser | bow
smoker | furnace
dried_kelp_block | kelp
hay_block | wheat
fletching_table | flint
lectern | paper
raw_gold_block | gold | w3
gold_block | gold | w3
scaffolding | jungle | w3
piston | piston
sticky_piston | { id: piston }, { id : nighttime_mob_drops, weight: 2 } | w2
sticky_piston | { id: piston, weight: 1 }, nighttime_mob_drops, swamp | w2
sticky_piston | piston, { id: nighttime_mob_drops, weight: 3 }, swamp | w3
orange_concrete | sand | w2
pink_glazed_terracotta | clay | w2
deepslate | deepslate
cracked_deepslate_bricks | deepslate
deepslate_tile_wall | deepslate
muddy_mangrove_roots | mangrove
packed_mud | mangrove
mud_bricks | mangrove
rooted_dirt | lush_cave
moss_block | lush_cave | w3
flowering_azalea | lush_cave
spore_blossom | lush_cave | w3
dripstone_block | dripstone_cave
raw_copper_block | copper
copper_block | copper
exposed_cut_copper | copper
big_dripleaf | lush_cave | w3
tuff | deepslate | w3
white_stained_glass | sand
brown_banner | wool
amethyst_block | amethyst | w2
calcite | amethyst | w2
chiseled_resin_bricks | pale_garden, nighttime_mob_drops | w2
creaking_heart | pale_garden, nighttime_mob_drops | w2
pale_moss_carpet | pale_garden | w6
lodestone | lodestone, { id: ruined_portal, weight: 2 } | w2
netherrack | ruined_portal
poplar_shelf | dappled_forest
yellow_poplar_leaves | dappled_forest, { id: shearable, weight: 2 } | w2
shelf_mushroom | dappled_forest, { id: mushroom, weight: 2 } | w2
straw_bed | wheat
```

## Appendix B: Extremelyd1/minecraft-bingo tier lists (GPL-3.0), recorded as reference data

Card distribution default: 2 S, 6 A, 9 B, 6 C, 2 D. Lists as shipped (they include items from versions after 1.21.1).

### Tier S

AMETHYST_CLUSTER ANGLER_POTTERY_SHERD ARCHER_POTTERY_SHERD ARMS_UP_POTTERY_SHERD BEACON BEE_NEST BLACK_SHULKER_BOX BLADE_POTTERY_SHERD BLUE_ICE 
BLUE_SHULKER_BOX BOLT_ARMOR_TRIM_SMITHING_TEMPLATE BRAIN_CORAL BRAIN_CORAL_BLOCK BRAIN_CORAL_FAN BREWER_POTTERY_SHERD BROWN_MUSHROOM_BLOCK BROWN_SHULKER_BOX 
BUBBLE_CORAL BUBBLE_CORAL_BLOCK BUBBLE_CORAL_FAN BURN_POTTERY_SHERD CALIBRATED_SCULK_SENSOR CHAINMAIL_BOOTS CHAINMAIL_CHESTPLATE CHAINMAIL_HELMET 
CHAINMAIL_LEGGINGS CHORUS_FLOWER CHORUS_FRUIT COAL_ORE COAST_ARMOR_TRIM_SMITHING_TEMPLATE CONDUIT COPPER_ORE CREAKING_HEART CREEPER_HEAD CRIMSON_NYLIUM 
CYAN_SHULKER_BOX DANGER_POTTERY_SHERD DEEPSLATE_COAL_ORE DEEPSLATE_COPPER_ORE DEEPSLATE_DIAMOND_ORE DEEPSLATE_EMERALD_ORE DEEPSLATE_GOLD_ORE DEEPSLATE_IRON_ORE 
DEEPSLATE_LAPIS_ORE DEEPSLATE_REDSTONE_ORE DIAMOND_ORE DISC_FRAGMENT_5 DRAGON_BREATH DRAGON_EGG DRAGON_HEAD DUNE_ARMOR_TRIM_SMITHING_TEMPLATE ECHO_SHARD ELYTRA 
EMERALD_ORE ENCHANTED_GOLDEN_APPLE END_STONE END_STONE_BRICKS END_STONE_BRICK_STAIRS END_STONE_BRICK_WALL EXPLORER_POTTERY_SHERD 
EYE_ARMOR_TRIM_SMITHING_TEMPLATE FIRE_CORAL FIRE_CORAL_BLOCK FIRE_CORAL_FAN FLOW_ARMOR_TRIM_SMITHING_TEMPLATE FRIEND_POTTERY_SHERD GLOBE_BANNER_PATTERN 
GOAT_HORN GOLD_ORE GRASS_BLOCK GRAY_SHULKER_BOX GREEN_SHULKER_BOX HEARTBREAK_POTTERY_SHERD HEART_POTTERY_SHERD HEAVY_CORE HONEY_BLOCK HORN_CORAL 
HORN_CORAL_BLOCK HORN_CORAL_FAN HOST_ARMOR_TRIM_SMITHING_TEMPLATE HOWL_POTTERY_SHERD ICE IRON_ORE LAPIS_ORE LARGE_AMETHYST_BUD LIGHT_BLUE_SHULKER_BOX 
LIGHT_GRAY_SHULKER_BOX LIME_SHULKER_BOX LINGERING_POTION MACE MAGENTA_SHULKER_BOX MEDIUM_AMETHYST_BUD MINER_POTTERY_SHERD MOURNER_POTTERY_SHERD MUSHROOM_STEM 
MUSIC_DISC_11 MUSIC_DISC_13 MUSIC_DISC_5 MUSIC_DISC_BLOCKS MUSIC_DISC_BOUNCE MUSIC_DISC_CAT MUSIC_DISC_CHIRP MUSIC_DISC_CREATOR MUSIC_DISC_CREATOR_MUSIC_BOX 
MUSIC_DISC_FAR MUSIC_DISC_LAVA_CHICKEN MUSIC_DISC_MALL MUSIC_DISC_MELLOHI MUSIC_DISC_OTHERSIDE MUSIC_DISC_PIGSTEP MUSIC_DISC_PRECIPICE MUSIC_DISC_RELIC 
MUSIC_DISC_STAL MUSIC_DISC_STRAD MUSIC_DISC_TEARS MUSIC_DISC_WAIT MUSIC_DISC_WARD MYCELIUM NETHERITE_AXE NETHERITE_BLOCK NETHERITE_BOOTS NETHERITE_CHESTPLATE 
NETHERITE_HELMET NETHERITE_HOE NETHERITE_HORSE_ARMOR NETHERITE_LEGGINGS NETHERITE_NAUTILUS_ARMOR NETHERITE_PICKAXE NETHERITE_SHOVEL NETHERITE_SPEAR 
NETHERITE_SWORD NETHERITE_UPGRADE_SMITHING_TEMPLATE NETHER_GOLD_ORE NETHER_QUARTZ_ORE NETHER_STAR OCHRE_FROGLIGHT ORANGE_SHULKER_BOX OXIDIZED_CHISELED_COPPER 
OXIDIZED_COPPER OXIDIZED_COPPER_BARS OXIDIZED_COPPER_BULB OXIDIZED_COPPER_CHAIN OXIDIZED_COPPER_CHEST OXIDIZED_COPPER_DOOR OXIDIZED_COPPER_GOLEM_STATUE 
OXIDIZED_COPPER_GRATE OXIDIZED_COPPER_LANTERN OXIDIZED_COPPER_TRAPDOOR OXIDIZED_CUT_COPPER OXIDIZED_CUT_COPPER_SLAB OXIDIZED_CUT_COPPER_STAIRS 
OXIDIZED_LIGHTNING_ROD PACKED_ICE PALE_HANGING_MOSS PEARLESCENT_FROGLIGHT PHANTOM_MEMBRANE PINK_SHULKER_BOX PITCHER_PLANT PITCHER_POD PLENTY_POTTERY_SHERD 
PODZOL POPPED_CHORUS_FRUIT PRIZE_POTTERY_SHERD PURPLE_SHULKER_BOX PURPUR_BLOCK PURPUR_PILLAR PURPUR_SLAB PURPUR_STAIRS RAISER_ARMOR_TRIM_SMITHING_TEMPLATE 
RECOVERY_COMPASS REDSTONE_ORE RED_MUSHROOM_BLOCK RED_SHULKER_BOX RIB_ARMOR_TRIM_SMITHING_TEMPLATE SCULK SCULK_CATALYST SCULK_SENSOR SCULK_SHRIEKER SCULK_VEIN 
SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE SHEAF_POTTERY_SHERD SHELTER_POTTERY_SHERD SHULKER_BOX SHULKER_SHELL 
SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE SKELETON_SKULL SKULL_BANNER_PATTERN SKULL_POTTERY_SHERD SMALL_AMETHYST_BUD SNIFFER_EGG SNORT_POTTERY_SHERD 
SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE TIDE_ARMOR_TRIM_SMITHING_TEMPLATE TORCHFLOWER TORCHFLOWER_SEEDS TOTEM_OF_UNDYING TRIDENT 
TUBE_CORAL TUBE_CORAL_BLOCK TUBE_CORAL_FAN TURTLE_EGG TURTLE_HELMET TURTLE_SCUTE VERDANT_FROGLIGHT VEX_ARMOR_TRIM_SMITHING_TEMPLATE 
WARD_ARMOR_TRIM_SMITHING_TEMPLATE WARPED_NYLIUM WAXED_OXIDIZED_CHISELED_COPPER WAXED_OXIDIZED_COPPER WAXED_OXIDIZED_COPPER_BARS WAXED_OXIDIZED_COPPER_BULB 
WAXED_OXIDIZED_COPPER_CHAIN WAXED_OXIDIZED_COPPER_CHEST WAXED_OXIDIZED_COPPER_DOOR WAXED_OXIDIZED_COPPER_GOLEM_STATUE WAXED_OXIDIZED_COPPER_GRATE 
WAXED_OXIDIZED_COPPER_LANTERN WAXED_OXIDIZED_COPPER_TRAPDOOR WAXED_OXIDIZED_CUT_COPPER WAXED_OXIDIZED_CUT_COPPER_SLAB WAXED_OXIDIZED_CUT_COPPER_STAIRS 
WAXED_OXIDIZED_LIGHTNING_ROD WAXED_WEATHERED_CHISELED_COPPER WAXED_WEATHERED_COPPER WAXED_WEATHERED_COPPER_BARS WAXED_WEATHERED_COPPER_BULB 
WAXED_WEATHERED_COPPER_CHAIN WAXED_WEATHERED_COPPER_CHEST WAXED_WEATHERED_COPPER_DOOR WAXED_WEATHERED_COPPER_GOLEM_STATUE WAXED_WEATHERED_COPPER_GRATE 
WAXED_WEATHERED_COPPER_LANTERN WAXED_WEATHERED_COPPER_TRAPDOOR WAXED_WEATHERED_CUT_COPPER WAXED_WEATHERED_CUT_COPPER_SLAB WAXED_WEATHERED_CUT_COPPER_STAIRS 
WAXED_WEATHERED_LIGHTNING_ROD WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE WEATHERED_CHISELED_COPPER WEATHERED_COPPER WEATHERED_COPPER_BARS WEATHERED_COPPER_BULB 
WEATHERED_COPPER_CHAIN WEATHERED_COPPER_CHEST WEATHERED_COPPER_DOOR WEATHERED_COPPER_GOLEM_STATUE WEATHERED_COPPER_GRATE WEATHERED_COPPER_LANTERN 
WEATHERED_COPPER_TRAPDOOR WEATHERED_CUT_COPPER WEATHERED_CUT_COPPER_SLAB WEATHERED_CUT_COPPER_STAIRS WEATHERED_LIGHTNING_ROD WHITE_SHULKER_BOX 
WILD_ARMOR_TRIM_SMITHING_TEMPLATE WITHER_ROSE WITHER_SKELETON_SKULL YELLOW_SHULKER_BOX ZOMBIE_HEAD

### Tier A

ACACIA_BOAT ACACIA_BUTTON ACACIA_CHEST_BOAT ACACIA_DOOR ACACIA_FENCE ACACIA_FENCE_GATE ACACIA_HANGING_SIGN ACACIA_LEAVES ACACIA_LOG ACACIA_PLANKS 
ACACIA_SAPLING ACACIA_SHELF ACACIA_SIGN ACACIA_SLAB ACACIA_STAIRS ACACIA_TRAPDOOR ALLIUM AMETHYST_BLOCK AMETHYST_SHARD ANCIENT_DEBRIS AXOLOTL_BUCKET AZALEA 
AZALEA_LEAVES BAKED_POTATO BAMBOO BAMBOO_BLOCK BAMBOO_BUTTON BAMBOO_CHEST_RAFT BAMBOO_DOOR BAMBOO_FENCE BAMBOO_FENCE_GATE BAMBOO_HANGING_SIGN BAMBOO_MOSAIC 
BAMBOO_MOSAIC_SLAB BAMBOO_MOSAIC_STAIRS BAMBOO_PLANKS BAMBOO_RAFT BAMBOO_SHELF BAMBOO_SIGN BAMBOO_SLAB BAMBOO_STAIRS BAMBOO_TRAPDOOR BASALT BEEHIVE BEETROOT 
BEETROOT_SEEDS BEETROOT_SOUP BELL BIG_DRIPLEAF BLACKSTONE BLACKSTONE_SLAB BLACKSTONE_STAIRS BLACKSTONE_WALL BLACK_CANDLE BLAZE_POWDER BLAZE_ROD BLUE_CANDLE 
BLUE_ORCHID BORDURE_INDENTED_BANNER_PATTERN BREEZE_ROD BREWING_STAND BROWN_BED BROWN_BUNDLE BROWN_CANDLE BROWN_CONCRETE BROWN_CONCRETE_POWDER 
BROWN_CONCRETE_SLAB BROWN_CONCRETE_STAIRS BROWN_CUSHION BROWN_DYE BROWN_GLAZED_TERRACOTTA BROWN_HARNESS BROWN_STAINED_GLASS BROWN_TERRACOTTA BROWN_WOOL 
BROWN_WOOL_SLAB BROWN_WOOL_STAIRS CACTUS CALCITE CANDLE CARROT CARROT_ON_A_STICK CARVED_PUMPKIN CHERRY_BOAT CHERRY_BUTTON CHERRY_CHEST_BOAT CHERRY_DOOR 
CHERRY_FENCE CHERRY_FENCE_GATE CHERRY_HANGING_SIGN CHERRY_LEAVES CHERRY_LOG CHERRY_PLANKS CHERRY_SAPLING CHERRY_SHELF CHERRY_SIGN CHERRY_SLAB CHERRY_STAIRS 
CHERRY_TRAPDOOR CHISELED_POLISHED_BLACKSTONE CHISELED_RED_SANDSTONE CHISELED_RESIN_BRICKS CHISELED_TUFF CLOSED_EYEBLOSSOM COBWEB COCOA_BEANS COD_BUCKET 
COOKED_RABBIT COOKIE COPPER_BULB COPPER_GOLEM_STATUE COPPER_HORSE_ARMOR COPPER_NAUTILUS_ARMOR CRACKED_POLISHED_BLACKSTONE_BRICKS CRIMSON_BUTTON CRIMSON_DOOR 
CRIMSON_FENCE CRIMSON_FENCE_GATE CRIMSON_FUNGUS CRIMSON_HANGING_SIGN CRIMSON_PLANKS CRIMSON_ROOTS CRIMSON_SHELF CRIMSON_SIGN CRIMSON_SLAB CRIMSON_STAIRS 
CRIMSON_STEM CRIMSON_TRAPDOOR CRYING_OBSIDIAN CUT_RED_SANDSTONE CUT_RED_SANDSTONE_SLAB CYAN_BED CYAN_BUNDLE CYAN_CANDLE CYAN_CONCRETE CYAN_CONCRETE_POWDER 
CYAN_CONCRETE_SLAB CYAN_CONCRETE_STAIRS CYAN_CUSHION CYAN_DYE CYAN_GLAZED_TERRACOTTA CYAN_HARNESS CYAN_STAINED_GLASS CYAN_TERRACOTTA CYAN_WOOL CYAN_WOOL_SLAB 
CYAN_WOOL_STAIRS DARK_PRISMARINE DARK_PRISMARINE_SLAB DARK_PRISMARINE_STAIRS DEAD_BRAIN_CORAL DEAD_BRAIN_CORAL_BLOCK DEAD_BRAIN_CORAL_FAN DEAD_BUBBLE_CORAL 
DEAD_BUBBLE_CORAL_BLOCK DEAD_BUBBLE_CORAL_FAN DEAD_BUSH DEAD_FIRE_CORAL DEAD_FIRE_CORAL_BLOCK DEAD_FIRE_CORAL_FAN DEAD_HORN_CORAL DEAD_HORN_CORAL_BLOCK 
DEAD_HORN_CORAL_FAN DEAD_TUBE_CORAL DEAD_TUBE_CORAL_BLOCK DEAD_TUBE_CORAL_FAN DIAMOND DIAMOND_AXE DIAMOND_BLOCK DIAMOND_BOOTS DIAMOND_CHESTPLATE DIAMOND_HELMET 
DIAMOND_HOE DIAMOND_HORSE_ARMOR DIAMOND_LEGGINGS DIAMOND_NAUTILUS_ARMOR DIAMOND_PICKAXE DIAMOND_SHOVEL DIAMOND_SPEAR DIAMOND_SWORD DRIED_GHAST DRIPSTONE_BLOCK 
EMERALD EMERALD_BLOCK ENCHANTED_BOOK ENCHANTING_TABLE ENDER_CHEST END_CRYSTAL EXPERIENCE_BOTTLE EXPOSED_CHISELED_COPPER EXPOSED_COPPER EXPOSED_COPPER_BARS 
EXPOSED_COPPER_BULB EXPOSED_COPPER_CHAIN EXPOSED_COPPER_CHEST EXPOSED_COPPER_DOOR EXPOSED_COPPER_GOLEM_STATUE EXPOSED_COPPER_GRATE EXPOSED_COPPER_LANTERN 
EXPOSED_COPPER_TRAPDOOR EXPOSED_CUT_COPPER EXPOSED_CUT_COPPER_SLAB EXPOSED_CUT_COPPER_STAIRS EXPOSED_LIGHTNING_ROD FERN FLOWERING_AZALEA 
FLOWERING_AZALEA_LEAVES GHAST_TEAR GILDED_BLACKSTONE GLISTERING_MELON_SLICE GLOW_BERRIES GLOW_LICHEN GOLDEN_HORSE_ARMOR GOLDEN_NAUTILUS_ARMOR GRAY_CANDLE 
GREEN_BED GREEN_BUNDLE GREEN_CANDLE GREEN_CONCRETE GREEN_CONCRETE_POWDER GREEN_CONCRETE_SLAB GREEN_CONCRETE_STAIRS GREEN_CUSHION GREEN_DYE 
GREEN_GLAZED_TERRACOTTA GREEN_HARNESS GREEN_STAINED_GLASS GREEN_TERRACOTTA GREEN_WOOL GREEN_WOOL_SLAB GREEN_WOOL_STAIRS HANGING_ROOTS HEART_OF_THE_SEA 
HONEYCOMB HONEYCOMB_BLOCK HONEY_BOTTLE IRON_HORSE_ARMOR IRON_NAUTILUS_ARMOR JACK_O_LANTERN JUKEBOX JUNGLE_BOAT JUNGLE_BUTTON JUNGLE_CHEST_BOAT JUNGLE_DOOR 
JUNGLE_FENCE JUNGLE_FENCE_GATE JUNGLE_HANGING_SIGN JUNGLE_LEAVES JUNGLE_LOG JUNGLE_PLANKS JUNGLE_SAPLING JUNGLE_SHELF JUNGLE_SIGN JUNGLE_SLAB JUNGLE_STAIRS 
JUNGLE_TRAPDOOR LARGE_FERN LIGHT_BLUE_CANDLE LIGHT_GRAY_CANDLE LILY_PAD LIME_BED LIME_BUNDLE LIME_CANDLE LIME_CONCRETE LIME_CONCRETE_POWDER LIME_CONCRETE_SLAB 
LIME_CONCRETE_STAIRS LIME_CUSHION LIME_DYE LIME_GLAZED_TERRACOTTA LIME_HARNESS LIME_STAINED_GLASS LIME_TERRACOTTA LIME_WOOL LIME_WOOL_SLAB LIME_WOOL_STAIRS 
MAGENTA_CANDLE MAGMA_BLOCK MAGMA_CREAM MANGROVE_BOAT MANGROVE_BUTTON MANGROVE_CHEST_BOAT MANGROVE_DOOR MANGROVE_FENCE MANGROVE_FENCE_GATE MANGROVE_HANGING_SIGN 
MANGROVE_LEAVES MANGROVE_LOG MANGROVE_PLANKS MANGROVE_PROPAGULE MANGROVE_ROOTS MANGROVE_SHELF MANGROVE_SIGN MANGROVE_SLAB MANGROVE_STAIRS MANGROVE_TRAPDOOR 
MELON MELON_SEEDS MELON_SLICE MOSSY_COBBLESTONE MOSSY_COBBLESTONE_SLAB MOSSY_COBBLESTONE_STAIRS MOSSY_COBBLESTONE_WALL MOSSY_STONE_BRICKS 
MOSSY_STONE_BRICK_SLAB MOSSY_STONE_BRICK_STAIRS MOSSY_STONE_BRICK_WALL MOSS_BLOCK MUDDY_MANGROVE_ROOTS MUSIC_DISC_TEARS NAUTILUS_SHELL NETHERITE_INGOT 
NETHERITE_SCRAP NETHER_SPROUTS NETHER_WART NETHER_WART_BLOCK OMINOUS_BOTTLE OMINOUS_TRIAL_KEY OPEN_EYEBLOSSOM ORANGE_CANDLE ORANGE_POPLAR_LEAVES ORANGE_TULIP 
PALE_MOSS_BLOCK PALE_OAK_BOAT PALE_OAK_BUTTON PALE_OAK_CHEST_BOAT PALE_OAK_DOOR PALE_OAK_FENCE PALE_OAK_FENCE_GATE PALE_OAK_HANGING_SIGN PALE_OAK_LEAVES 
PALE_OAK_LOG PALE_OAK_PLANKS PALE_OAK_SAPLING PALE_OAK_SHELF PALE_OAK_SIGN PALE_OAK_SLAB PALE_OAK_STAIRS PALE_OAK_TRAPDOOR PINK_CANDLE PINK_PETALS PINK_TULIP 
POINTED_DRIPSTONE POISONOUS_POTATO POLISHED_BASALT POLISHED_BLACKSTONE POLISHED_BLACKSTONE_BRICKS POLISHED_BLACKSTONE_BRICK_SLAB 
POLISHED_BLACKSTONE_BRICK_STAIRS POLISHED_BLACKSTONE_BRICK_WALL POLISHED_BLACKSTONE_BUTTON POLISHED_BLACKSTONE_SLAB POLISHED_BLACKSTONE_STAIRS 
POLISHED_BLACKSTONE_WALL POLISHED_TUFF POLISHED_TUFF_SLAB POLISHED_TUFF_STAIRS POLISHED_TUFF_WALL POPLAR_BOAT POPLAR_BUTTON POPLAR_CHEST_BOAT POPLAR_DOOR 
POPLAR_FENCE POPLAR_FENCE_GATE POPLAR_HANGING_SIGN POPLAR_LOG POPLAR_PLANKS POPLAR_SAPLING POPLAR_SHELF POPLAR_SIGN POPLAR_SLAB POPLAR_STAIRS POPLAR_TRAPDOOR 
POTATO POWDER_SNOW_BUCKET PRISMARINE PRISMARINE_BRICKS PRISMARINE_BRICK_SLAB PRISMARINE_BRICK_STAIRS PRISMARINE_CRYSTALS PRISMARINE_SHARD PRISMARINE_SLAB 
PRISMARINE_STAIRS PRISMARINE_WALL PUFFERFISH_BUCKET PUMPKIN PUMPKIN_PIE PUMPKIN_SEEDS PURPLE_CANDLE RABBIT RABBIT_FOOT RABBIT_HIDE RABBIT_STEW RED_CANDLE 
RED_NETHER_BRICKS RED_NETHER_BRICK_SLAB RED_NETHER_BRICK_STAIRS RED_NETHER_BRICK_WALL RED_POPLAR_LEAVES RED_SAND RED_SANDSTONE RED_SANDSTONE_SLAB 
RED_SANDSTONE_STAIRS RED_SANDSTONE_WALL RED_SHRUB RED_TULIP RESIN_BLOCK RESIN_BRICK RESIN_BRICK_SLAB RESIN_BRICK_STAIRS RESIN_BRICK_WALL RESIN_CLUMP 
RESPAWN_ANCHOR ROOTED_DIRT SALMON_BUCKET SCAFFOLDING SEAGRASS SEA_LANTERN SEA_PICKLE SHELF_MUSHROOM SHORT_GRASS SLIME_BALL SLIME_BLOCK SMALL_DRIPLEAF 
SMOOTH_BASALT SMOOTH_RED_SANDSTONE SMOOTH_RED_SANDSTONE_SLAB SMOOTH_RED_SANDSTONE_STAIRS SNOW SNOWBALL SNOW_BLOCK SOUL_CAMPFIRE SOUL_LANTERN SOUL_SAND 
SOUL_SOIL SOUL_TORCH SPLASH_POTION SPONGE SPORE_BLOSSOM SPYGLASS STICKY_PISTON STRIPPED_ACACIA_LOG STRIPPED_BAMBOO_BLOCK STRIPPED_CHERRY_LOG 
STRIPPED_CRIMSON_STEM STRIPPED_JUNGLE_LOG STRIPPED_MANGROVE_LOG STRIPPED_PALE_OAK_LOG STRIPPED_POPLAR_LOG STRIPPED_WARPED_STEM SULFUR_CUBE_BUCKET SULFUR_SPIKE 
SUNFLOWER SWEET_BERRIES TADPOLE_BUCKET TALL_GRASS TINTED_GLASS TIPPED_ARROW TROPICAL_FISH_BUCKET TUFF TUFF_BRICKS TUFF_BRICK_SLAB TUFF_BRICK_STAIRS 
TUFF_BRICK_WALL TUFF_SLAB TUFF_STAIRS TUFF_WALL TWISTING_VINES VINE WARPED_BUTTON WARPED_DOOR WARPED_FENCE WARPED_FENCE_GATE WARPED_FUNGUS 
WARPED_FUNGUS_ON_A_STICK WARPED_HANGING_SIGN WARPED_PLANKS WARPED_ROOTS WARPED_SHELF WARPED_SIGN WARPED_SLAB WARPED_STAIRS WARPED_STEM WARPED_TRAPDOOR 
WARPED_WART_BLOCK WAXED_CHISELED_COPPER WAXED_COPPER_BARS WAXED_COPPER_BLOCK WAXED_COPPER_BULB WAXED_COPPER_CHAIN WAXED_COPPER_CHEST WAXED_COPPER_DOOR 
WAXED_COPPER_GOLEM_STATUE WAXED_COPPER_GRATE WAXED_COPPER_LANTERN WAXED_COPPER_TRAPDOOR WAXED_CUT_COPPER WAXED_CUT_COPPER_SLAB WAXED_CUT_COPPER_STAIRS 
WAXED_EXPOSED_CHISELED_COPPER WAXED_EXPOSED_COPPER WAXED_EXPOSED_COPPER_BARS WAXED_EXPOSED_COPPER_BULB WAXED_EXPOSED_COPPER_CHAIN WAXED_EXPOSED_COPPER_CHEST 
WAXED_EXPOSED_COPPER_DOOR WAXED_EXPOSED_COPPER_GOLEM_STATUE WAXED_EXPOSED_COPPER_GRATE WAXED_EXPOSED_COPPER_LANTERN WAXED_EXPOSED_COPPER_TRAPDOOR 
WAXED_EXPOSED_CUT_COPPER WAXED_EXPOSED_CUT_COPPER_SLAB WAXED_EXPOSED_CUT_COPPER_STAIRS WAXED_EXPOSED_LIGHTNING_ROD WAXED_LIGHTNING_ROD WEEPING_VINES WET_SPONGE 
WHITE_CANDLE WHITE_TULIP WIND_CHARGE YELLOW_CANDLE YELLOW_POPLAR_LEAVES

### Tier B

ACTIVATOR_RAIL ANVIL ARMADILLO_SCUTE AZURE_BLUET BIRCH_HANGING_SIGN BIRCH_LEAVES BLAST_FURNACE BLUE_BED BLUE_BUNDLE BLUE_CONCRETE BLUE_CONCRETE_POWDER 
BLUE_CONCRETE_SLAB BLUE_CONCRETE_STAIRS BLUE_CUSHION BLUE_DYE BLUE_EGG BLUE_GLAZED_TERRACOTTA BLUE_HARNESS BLUE_STAINED_GLASS BLUE_TERRACOTTA BLUE_WOOL 
BLUE_WOOL_SLAB BLUE_WOOL_STAIRS BROWN_EGG BROWN_MUSHROOM BRUSH BUCKET BUSH CAKE CAULDRON CHEST_MINECART CHISELED_CINNABAR CHISELED_COPPER 
CHISELED_NETHER_BRICKS CHISELED_QUARTZ_BLOCK CHISELED_SULFUR CINNABAR CINNABAR_BRICKS CINNABAR_BRICK_SLAB CINNABAR_BRICK_STAIRS CINNABAR_BRICK_WALL 
CINNABAR_SLAB CINNABAR_STAIRS CINNABAR_WALL CLOCK COMPARATOR COMPASS COPPER_AXE COPPER_BARS COPPER_BLOCK COPPER_BOOTS COPPER_CHAIN COPPER_CHEST 
COPPER_CHESTPLATE COPPER_DOOR COPPER_GRATE COPPER_HELMET COPPER_HOE COPPER_INGOT COPPER_LANTERN COPPER_LEGGINGS COPPER_NUGGET COPPER_PICKAXE COPPER_SHOVEL 
COPPER_SPEAR COPPER_SWORD COPPER_TORCH COPPER_TRAPDOOR CORNFLOWER CRACKED_NETHER_BRICKS CRAFTER CROSSBOW CUT_COPPER CUT_COPPER_SLAB CUT_COPPER_STAIRS 
DARK_OAK_HANGING_SIGN DARK_OAK_LEAVES DAYLIGHT_DETECTOR DETECTOR_RAIL DISPENSER DROPPER EGG ENDER_EYE FIRE_CHARGE FLINT_AND_STEEL FLOWER_BANNER_PATTERN 
FLOW_POTTERY_SHERD FURNACE_MINECART GLOWSTONE GLOWSTONE_DUST GLOW_INK_SAC GLOW_ITEM_FRAME GOLDEN_APPLE GOLDEN_AXE GOLDEN_BOOTS GOLDEN_CARROT GOLDEN_CHESTPLATE 
GOLDEN_DANDELION GOLDEN_HELMET GOLDEN_HOE GOLDEN_LEGGINGS GOLDEN_PICKAXE GOLDEN_SHOVEL GOLDEN_SPEAR GOLDEN_SWORD GOLD_BLOCK GOLD_INGOT GOLD_NUGGET 
GUSTER_POTTERY_SHERD HOPPER HOPPER_MINECART IRON_AXE IRON_BARS IRON_BLOCK IRON_BOOTS IRON_CHAIN IRON_CHESTPLATE IRON_DOOR IRON_HELMET IRON_HOE IRON_INGOT 
IRON_LEGGINGS IRON_NUGGET IRON_PICKAXE IRON_SHOVEL IRON_SPEAR IRON_SWORD IRON_TRAPDOOR LANTERN LAPIS_BLOCK LAPIS_LAZULI LAVA_BUCKET LIGHTNING_ROD 
LIGHT_BLUE_BED LIGHT_BLUE_BUNDLE LIGHT_BLUE_CONCRETE LIGHT_BLUE_CONCRETE_POWDER LIGHT_BLUE_CONCRETE_SLAB LIGHT_BLUE_CONCRETE_STAIRS LIGHT_BLUE_CUSHION 
LIGHT_BLUE_DYE LIGHT_BLUE_GLAZED_TERRACOTTA LIGHT_BLUE_HARNESS LIGHT_BLUE_STAINED_GLASS LIGHT_BLUE_TERRACOTTA LIGHT_BLUE_WOOL LIGHT_BLUE_WOOL_SLAB 
LIGHT_BLUE_WOOL_STAIRS LILAC LODESTONE MAGENTA_BED MAGENTA_BUNDLE MAGENTA_CONCRETE MAGENTA_CONCRETE_POWDER MAGENTA_CONCRETE_SLAB MAGENTA_CONCRETE_STAIRS 
MAGENTA_CUSHION MAGENTA_DYE MAGENTA_GLAZED_TERRACOTTA MAGENTA_HARNESS MAGENTA_STAINED_GLASS MAGENTA_TERRACOTTA MAGENTA_WOOL MAGENTA_WOOL_SLAB 
MAGENTA_WOOL_STAIRS MILK_BUCKET MINECART MUSHROOM_STEW NAME_TAG NETHERRACK NETHER_BRICK NETHER_BRICKS NETHER_BRICK_FENCE NETHER_BRICK_SLAB NETHER_BRICK_STAIRS 
NETHER_BRICK_WALL NOTE_BLOCK OAK_HANGING_SIGN OAK_LEAVES OBSERVER OBSIDIAN OXEYE_DAISY PEONY PISTON POLISHED_CINNABAR POLISHED_CINNABAR_SLAB 
POLISHED_CINNABAR_STAIRS POLISHED_CINNABAR_WALL POLISHED_SULFUR POLISHED_SULFUR_SLAB POLISHED_SULFUR_STAIRS POLISHED_SULFUR_WALL POTENT_SULFUR POWERED_RAIL 
PURPLE_BED PURPLE_BUNDLE PURPLE_CONCRETE PURPLE_CONCRETE_POWDER PURPLE_CONCRETE_SLAB PURPLE_CONCRETE_STAIRS PURPLE_CUSHION PURPLE_DYE PURPLE_GLAZED_TERRACOTTA 
PURPLE_HARNESS PURPLE_STAINED_GLASS PURPLE_TERRACOTTA PURPLE_WOOL PURPLE_WOOL_SLAB PURPLE_WOOL_STAIRS QUARTZ QUARTZ_BLOCK QUARTZ_BRICKS QUARTZ_PILLAR 
QUARTZ_SLAB QUARTZ_STAIRS RAIL RAW_COPPER RAW_COPPER_BLOCK RAW_GOLD RAW_GOLD_BLOCK RAW_IRON RAW_IRON_BLOCK REDSTONE REDSTONE_BLOCK REDSTONE_LAMP REDSTONE_TORCH 
RED_MUSHROOM REPEATER ROSE_BUSH SADDLE SCRAPE_POTTERY_SHERD SHEARS SHIELD SHORT_DRY_GRASS SHROOMLIGHT SMITHING_TABLE SMOOTH_QUARTZ SMOOTH_QUARTZ_SLAB 
SMOOTH_QUARTZ_STAIRS SPECTRAL_ARROW SPRUCE_HANGING_SIGN SPRUCE_LEAVES STONECUTTER SULFUR SULFUR_BRICKS SULFUR_BRICK_SLAB SULFUR_BRICK_STAIRS SULFUR_BRICK_WALL 
SULFUR_SLAB SULFUR_STAIRS SULFUR_WALL SUSPICIOUS_STEW TALL_DRY_GRASS TARGET TNT_MINECART TRAPPED_CHEST TRIAL_KEY TRIPWIRE_HOOK WATER_BUCKET WOLF_ARMOR

### Tier C

ANDESITE ANDESITE_SLAB ANDESITE_STAIRS ANDESITE_WALL ARMOR_STAND ARROW BLACK_BED BLACK_BUNDLE BLACK_CONCRETE BLACK_CONCRETE_POWDER BLACK_CONCRETE_SLAB 
BLACK_CONCRETE_STAIRS BLACK_CUSHION BLACK_DYE BLACK_GLAZED_TERRACOTTA BLACK_HARNESS BLACK_STAINED_GLASS BLACK_TERRACOTTA BLACK_WOOL BLACK_WOOL_SLAB 
BLACK_WOOL_STAIRS BONE BONE_BLOCK BONE_MEAL BOOKSHELF BOW BRICK BRICKS BRICK_SLAB BRICK_STAIRS BRICK_WALL BUNDLE CAMPFIRE CHISELED_DEEPSLATE 
CHISELED_STONE_BRICKS COAL COAL_BLOCK COBBLED_DEEPSLATE COBBLED_DEEPSLATE_SLAB COBBLED_DEEPSLATE_STAIRS COBBLED_DEEPSLATE_WALL COBBLESTONE COBBLESTONE_SLAB 
COBBLESTONE_STAIRS COBBLESTONE_WALL COOKED_BEEF COOKED_CHICKEN COOKED_COD COOKED_MUTTON COOKED_PORKCHOP COOKED_SALMON CRACKED_DEEPSLATE_BRICKS 
CRACKED_DEEPSLATE_TILES CRACKED_STONE_BRICKS DECORATED_POT DEEPSLATE DEEPSLATE_BRICKS DEEPSLATE_BRICK_SLAB DEEPSLATE_BRICK_STAIRS DEEPSLATE_BRICK_WALL 
DEEPSLATE_TILES DEEPSLATE_TILE_SLAB DEEPSLATE_TILE_STAIRS DEEPSLATE_TILE_WALL DIORITE DIORITE_SLAB DIORITE_STAIRS DIORITE_WALL DRIED_KELP DRIED_KELP_BLOCK 
ENDER_PEARL FERMENTED_SPIDER_EYE FIELD_MASONED_BANNER_PATTERN FIREWORK_ROCKET FIREWORK_STAR FISHING_ROD FLOWER_POT FURNACE GLASS GLASS_BOTTLE GRANITE 
GRANITE_SLAB GRANITE_STAIRS GRANITE_WALL GRAY_BED GRAY_BUNDLE GRAY_CONCRETE GRAY_CONCRETE_POWDER GRAY_CONCRETE_SLAB GRAY_CONCRETE_STAIRS GRAY_CUSHION GRAY_DYE 
GRAY_GLAZED_TERRACOTTA GRAY_HARNESS GRAY_STAINED_GLASS GRAY_TERRACOTTA GRAY_WOOL GRAY_WOOL_SLAB GRAY_WOOL_STAIRS GRINDSTONE GUNPOWDER INK_SAC LEAD LECTERN 
LEVER LIGHT_GRAY_BED LIGHT_GRAY_BUNDLE LIGHT_GRAY_CONCRETE LIGHT_GRAY_CONCRETE_POWDER LIGHT_GRAY_CONCRETE_SLAB LIGHT_GRAY_CONCRETE_STAIRS LIGHT_GRAY_CUSHION 
LIGHT_GRAY_DYE LIGHT_GRAY_GLAZED_TERRACOTTA LIGHT_GRAY_HARNESS LIGHT_GRAY_STAINED_GLASS LIGHT_GRAY_TERRACOTTA LIGHT_GRAY_WOOL LIGHT_GRAY_WOOL_SLAB 
LIGHT_GRAY_WOOL_STAIRS LILY_OF_THE_VALLEY LOOM MUD MUD_BRICKS MUD_BRICK_SLAB MUD_BRICK_STAIRS MUD_BRICK_WALL ORANGE_BUNDLE ORANGE_CONCRETE 
ORANGE_CONCRETE_POWDER ORANGE_CONCRETE_SLAB ORANGE_CONCRETE_STAIRS ORANGE_GLAZED_TERRACOTTA ORANGE_HARNESS ORANGE_STAINED_GLASS ORANGE_TERRACOTTA PACKED_MUD 
PINK_BED PINK_BUNDLE PINK_CONCRETE PINK_CONCRETE_POWDER PINK_CONCRETE_SLAB PINK_CONCRETE_STAIRS PINK_CUSHION PINK_DYE PINK_GLAZED_TERRACOTTA PINK_HARNESS 
PINK_STAINED_GLASS PINK_TERRACOTTA PINK_WOOL PINK_WOOL_SLAB PINK_WOOL_STAIRS POLISHED_ANDESITE POLISHED_ANDESITE_SLAB POLISHED_ANDESITE_STAIRS 
POLISHED_DEEPSLATE POLISHED_DEEPSLATE_SLAB POLISHED_DEEPSLATE_STAIRS POLISHED_DEEPSLATE_WALL POLISHED_DIORITE POLISHED_DIORITE_SLAB POLISHED_DIORITE_STAIRS 
POLISHED_GRANITE POLISHED_GRANITE_SLAB POLISHED_GRANITE_STAIRS POTION PUFFERFISH RED_BUNDLE RED_CONCRETE RED_CONCRETE_POWDER RED_CONCRETE_SLAB 
RED_CONCRETE_STAIRS RED_GLAZED_TERRACOTTA RED_HARNESS RED_STAINED_GLASS RED_TERRACOTTA REINFORCED_DEEPSLATE ROTTEN_FLESH SMOKER SMOOTH_SANDSTONE 
SMOOTH_SANDSTONE_STAIRS SMOOTH_STONE SMOOTH_STONE_SLAB SPIDER_EYE STONE STONE_AXE STONE_BRICKS STONE_BRICK_SLAB STONE_BRICK_STAIRS STONE_BRICK_WALL 
STONE_BUTTON STONE_HOE STONE_PICKAXE STONE_SHOVEL STONE_SLAB STONE_SPEAR STONE_STAIRS STONE_SWORD STRING TERRACOTTA TNT TROPICAL_FISH WHITE_BUNDLE 
WHITE_CONCRETE WHITE_CONCRETE_POWDER WHITE_CONCRETE_SLAB WHITE_CONCRETE_STAIRS WHITE_DYE WHITE_GLAZED_TERRACOTTA WHITE_HARNESS WHITE_STAINED_GLASS 
WHITE_TERRACOTTA YELLOW_BUNDLE YELLOW_CONCRETE YELLOW_CONCRETE_SLAB YELLOW_CONCRETE_STAIRS YELLOW_CONCRETE_POWDER YELLOW_GLAZED_TERRACOTTA YELLOW_HARNESS 
YELLOW_STAINED_GLASS YELLOW_TERRACOTTA

### Tier D

APPLE BARREL BEEF BIRCH_BOAT BIRCH_BUTTON BIRCH_CHEST_BOAT BIRCH_DOOR BIRCH_FENCE BIRCH_FENCE_GATE BIRCH_LOG BIRCH_PLANKS BIRCH_SAPLING BIRCH_SHELF BIRCH_SIGN 
BIRCH_SLAB BIRCH_STAIRS BIRCH_TRAPDOOR BOOK BOWL BREAD CACTUS_FLOWER CARTOGRAPHY_TABLE CHARCOAL CHICKEN CHISELED_BOOKSHELF CHISELED_SANDSTONE CLAY CLAY_BALL 
COARSE_DIRT COD COMPOSTER CRAFTING_TABLE CUT_SANDSTONE CUT_SANDSTONE_SLAB DANDELION DARK_OAK_BOAT DARK_OAK_BUTTON DARK_OAK_CHEST_BOAT DARK_OAK_DOOR 
DARK_OAK_FENCE DARK_OAK_FENCE_GATE DARK_OAK_LOG DARK_OAK_PLANKS DARK_OAK_SAPLING DARK_OAK_SHELF DARK_OAK_SIGN DARK_OAK_SLAB DARK_OAK_STAIRS DARK_OAK_TRAPDOOR 
DIRT FEATHER FIREFLY_BUSH FLETCHING_TABLE FLINT GRAVEL HAY_BLOCK ITEM_FRAME KELP LADDER LEAF_LITTER LEATHER LEATHER_BOOTS LEATHER_CHESTPLATE LEATHER_HELMET 
LEATHER_HORSE_ARMOR LEATHER_LEGGINGS MUTTON OAK_BOAT OAK_BUTTON OAK_CHEST_BOAT OAK_DOOR OAK_FENCE OAK_FENCE_GATE OAK_LOG OAK_PLANKS OAK_SAPLING OAK_SHELF 
OAK_SIGN OAK_SLAB OAK_STAIRS OAK_TRAPDOOR ORANGE_BED ORANGE_CUSHION ORANGE_DYE ORANGE_WOOL ORANGE_WOOL_SLAB ORANGE_WOOL_STAIRS PAINTING PAPER POPPY PORKCHOP 
RED_BED RED_CUSHION RED_DYE RED_WOOL RED_WOOL_SLAB RED_WOOL_STAIRS SALMON SAND SANDSTONE SANDSTONE_SLAB SANDSTONE_STAIRS SANDSTONE_WALL SMOOTH_SANDSTONE_SLAB 
SPRUCE_BOAT SPRUCE_BUTTON SPRUCE_CHEST_BOAT SPRUCE_DOOR SPRUCE_FENCE SPRUCE_FENCE_GATE SPRUCE_LOG SPRUCE_PLANKS SPRUCE_PLANKS SPRUCE_SAPLING SPRUCE_SHELF 
SPRUCE_SIGN SPRUCE_SLAB SPRUCE_STAIRS SPRUCE_TRAPDOOR STICK STRAW_BED STRIPPED_BIRCH_LOG STRIPPED_DARK_OAK_LOG STRIPPED_OAK_LOG SUGAR SUGAR_CANE TORCH WHEAT 
WHEAT_SEEDS WHITE_BED WHITE_CUSHION WHITE_WOOL WHITE_WOOL_SLAB WHITE_WOOL_STAIRS WILDFLOWERS WOODEN_AXE WOODEN_HOE WOODEN_PICKAXE WOODEN_SHOVEL WOODEN_SPEAR 
WOODEN_SWORD WRITABLE_BOOK WRITTEN_BOOK YELLOW_BED YELLOW_DYE YELLOW_WOOL

### Groups (one per line; never two from a group on a card)

```
ACACIA_PLANKS|ACACIA_SAPLING|ACACIA_LOG|STRIPPED_ACACIA_LOG|ACACIA_LEAVES|ACACIA_SLAB|ACACIA_FENCE|ACACIA_STAIRS|ACACIA_BUTTON|ACACIA_DOOR|ACACIA_TRAPDOOR|ACACIA_FENCE_GATE|ACACIA_BOAT|ACACIA_CHEST_BOAT|ACACIA_SIGN|ACACIA_HANGING_SIGN|ACACIA_SHELF
AMETHYST_SHARD|CALCITE|TUFF|TUFF_SLAB|TUFF_STAIRS|TUFF_WALL|CHISELED_TUFF|POLISHED_TUFF|POLISHED_TUFF_SLAB|POLISHED_TUFF_STAIRS|POLISHED_TUFF_WALL|TUFF_BRICKS|TUFF_BRICK_SLAB|TUFF_BRICK_STAIRS|TUFF_BRICK_WALL|CHISELED_TUFF_BRICKS|AMETHYST_BLOCK|SPYGLASS|SMALL_AMETHYST_BUD|MEDIUM_AMETHYST_BUD|LARGE_AMETHYST_BUD|AMETHYST_CLUSTER|TINTED_GLASS
ANCIENT_DEBRIS|NETHERITE_SCRAP|NETHERITE_INGOT|NETHERITE_BLOCK|NETHERITE_SWORD|NETHERITE_SHOVEL|NETHERITE_PICKAXE|NETHERITE_AXE|NETHERITE_HOE|NETHERITE_HELMET|NETHERITE_CHESTPLATE|NETHERITE_LEGGINGS|NETHERITE_BOOTS|NETHERITE_SPEAR
ANDESITE|POLISHED_ANDESITE|ANDESITE_WALL|ANDESITE_STAIRS|POLISHED_ANDESITE_STAIRS|ANDESITE_SLAB|POLISHED_ANDESITE_SLAB
APPLE|GOLDEN_APPLE|ENCHANTED_GOLDEN_APPLE
ARMADILLO_SCUTE|WOLF_ARMOR
AZALEA_LEAVES|FLOWERING_AZALEA_LEAVES|AZALEA|FLOWERING_AZALEA|SPORE_BLOSSOM|MOSS_BLOCK|BIG_DRIPLEAF|SMALL_DRIPLEAF|HANGING_ROOTS|ROOTED_DIRT
BAMBOO_PLANKS|BAMBOO_BLOCK|STRIPPED_BAMBOO_BLOCK|BAMBOO_MOSAIC|BAMBOO|BAMBOO_SLAB|BAMBOO_MOSAIC_SLAB|BAMBOO_FENCE|BAMBOO_STAIRS|BAMBOO_MOSAIC_STAIRS|BAMBOO_BUTTON|BAMBOO_DOOR|BAMBOO_TRAPDOOR|BAMBOO_FENCE_GATE|BAMBOO_RAFT|BAMBOO_CHEST_RAFT|BAMBOO_SIGN|BAMBOO_HANGING_SIGN|SCAFFOLDING|BAMBOO_SHELF
BASALT|POLISHED_BASALT|SMOOTH_BASALT
BEEF|COOKED_BEEF
BELL
BIRCH_PLANKS|BIRCH_SAPLING|BIRCH_LOG|STRIPPED_BIRCH_LOG|BIRCH_LEAVES|BIRCH_SLAB|BIRCH_FENCE|BIRCH_STAIRS|BIRCH_BUTTON|BIRCH_DOOR|BIRCH_TRAPDOOR|BIRCH_FENCE_GATE|BIRCH_BOAT|BIRCH_CHEST_BOAT|BIRCH_SIGN|BIRCH_HANGING_SIGN|BIRCH_SHELF
BLACKSTONE|BLACKSTONE_WALL|POLISHED_BLACKSTONE_WALL|POLISHED_BLACKSTONE_BRICK_WALL|POLISHED_BLACKSTONE_BUTTON|BLACKSTONE_SLAB|BLACKSTONE_STAIRS|GILDED_BLACKSTONE|POLISHED_BLACKSTONE|POLISHED_BLACKSTONE_SLAB|POLISHED_BLACKSTONE_STAIRS|CHISELED_POLISHED_BLACKSTONE|POLISHED_BLACKSTONE_BRICKS|POLISHED_BLACKSTONE_BRICK_SLAB|POLISHED_BLACKSTONE_BRICK_STAIRS|CRACKED_POLISHED_BLACKSTONE_BRICKS
BLACK_DYE|GRAY_DYE|LIGHT_GRAY_DYE|INK_SAC|AZURE_BLUET|OXEYE_DAISY
BLACK_HARNESS|BLUE_HARNESS|BROWN_HARNESS|CYAN_HARNESS|GRAY_HARNESS|GREEN_HARNESS|LIGHT_BLUE_HARNESS|LIGHT_GRAY_HARNESS|LIME_HARNESS|MAGENTA_HARNESS|ORANGE_HARNESS|PINK_HARNESS|PURPLE_HARNESS|RED_HARNESS|WHITE_HARNESS|YELLOW_HARNESS
BLAZE_ROD|BLAZE_POWDER|BREWING_STAND|FIRE_CHARGE|SPLASH_POTION
BLUE_DYE|LAPIS_LAZULI|LAPIS_BLOCK|LIGHT_BLUE_DYE|PURPLE_DYE|MAGENTA_DYE|BLUE_ORCHID|ALLIUM|CORNFLOWER|LILAC
BOOKSHELF|CHISELED_BOOKSHELF|LECTERN
BREEZE_ROD|WIND_CHARGE
BROWN_DYE|COOKIE|COCOA_BEANS
BROWN_MUSHROOM_BLOCK|RED_MUSHROOM_BLOCK|MUSHROOM_STEM
BROWN_MUSHROOM|RED_MUSHROOM|MUSHROOM_STEW|SUSPICIOUS_STEW
BRUSH|ANGLER_POTTERY_SHERD|ARCHER_POTTERY_SHERD|ARMS_UP_POTTERY_SHERD|BLADE_POTTERY_SHERD|BREWER_POTTERY_SHERD|BURN_POTTERY_SHERD|DANGER_POTTERY_SHERD|EXPLORER_POTTERY_SHERD|FRIEND_POTTERY_SHERD|HEART_POTTERY_SHERD|HEARTBREAK_POTTERY_SHERD|HOWL_POTTERY_SHERD|MINER_POTTERY_SHERD|MOURNER_POTTERY_SHERD|PLENTY_POTTERY_SHERD|PRIZE_POTTERY_SHERD|SHEAF_POTTERY_SHERD|SHELTER_POTTERY_SHERD|SKULL_POTTERY_SHERD|SNORT_POTTERY_SHERD|FLOW_POTTERY_SHERD|GUSTER_POTTERY_SHERD|SCRAPE_POTTERY_SHERD
BUNDLE|BLACK_BUNDLE|BLUE_BUNDLE|BROWN_BUNDLE|CYAN_BUNDLE|GRAY_BUNDLE|GREEN_BUNDLE|LIGHT_BLUE_BUNDLE|LIGHT_GRAY_BUNDLE|LIME_BUNDLE|MAGENTA_BUNDLE|ORANGE_BUNDLE|PINK_BUNDLE|PURPLE_BUNDLE|RED_BUNDLE|WHITE_BUNDLE|YELLOW_BUNDLE
CARROT|GOLDEN_CARROT
CHAINMAIL_HELMET|CHAINMAIL_CHESTPLATE|CHAINMAIL_LEGGINGS|CHAINMAIL_BOOTS
CHERRY_PLANKS|CHERRY_SAPLING|CHERRY_LOG|STRIPPED_CHERRY_LOG|CHERRY_LEAVES|CHERRY_SLAB|CHERRY_FENCE|CHERRY_STAIRS|CHERRY_BUTTON|CHERRY_DOOR|CHERRY_TRAPDOOR|CHERRY_FENCE_GATE|CHERRY_BOAT|CHERRY_CHEST_BOAT|CHERRY_SIGN|CHERRY_HANGING_SIGN|CHERRY_SHELF
CHEST|CRAFTING_TABLE|LADDER|CHARCOAL|WOODEN_SWORD|WOODEN_SHOVEL|WOODEN_PICKAXE|WOODEN_AXE|WOODEN_HOE|STICK|BOWL|COMPOSTER|BARREL|CAMPFIRE|WOODEN_SPEAR
CHICKEN|COOKED_CHICKEN
CHISELED_CINNABAR|CINNABAR|CINNABAR_BRICK_SLAB|CINNABAR_BRICK_STAIRS|CINNABAR_BRICK_WALL|CINNABAR_BRICKS|CINNABAR_SLAB|CINNABAR_STAIRS|CINNABAR_WALL|POLISHED_CINNABAR|POLISHED_CINNABAR_SLAB|POLISHED_CINNABAR_STAIRS|POLISHED_CINNABAR_WALL
CHISELED_SULFUR|POLISHED_SULFUR|POLISHED_SULFUR_SLAB|POLISHED_SULFUR_STAIRS|POLISHED_SULFUR_WALL|POTENT_SULFUR|SULFUR|SULFUR_BRICK_SLAB|SULFUR_BRICK_STAIRS|SULFUR_BRICK_WALL|SULFUR_BRICKS|SULFUR_SLAB|SULFUR_SPIKE|SULFUR_STAIRS|SULFUR_WALL|SULFUR_CUBE_BUCKET
CHORUS_FRUIT|POPPED_CHORUS_FRUIT|PURPUR_SLAB|CHORUS_FLOWER|PURPUR_BLOCK|PURPUR_PILLAR|PURPUR_STAIRS
CLAY|BRICKS|BRICK_SLAB|DECORATED_POT|BRICK_STAIRS|BRICK_WALL|BRICK|CLAY_BALL|FLOWER_POT
CLOSED_EYEBLOSSOM|OPEN_EYEBLOSSOM
COAL_ORE|DEEPSLATE_COAL_ORE|IRON_ORE|DEEPSLATE_IRON_ORE|COPPER_ORE|DEEPSLATE_COPPER_ORE|GOLD_ORE|DEEPSLATE_GOLD_ORE|REDSTONE_ORE|DEEPSLATE_REDSTONE_ORE|EMERALD_ORE|DEEPSLATE_EMERALD_ORE|LAPIS_ORE|DEEPSLATE_LAPIS_ORE|DIAMOND_ORE|DEEPSLATE_DIAMOND_ORE|NETHER_GOLD_ORE|NETHER_QUARTZ_ORE|CRIMSON_NYLIUM|WARPED_NYLIUM
COAL|COAL_BLOCK|TORCH
COBWEB
COD|SALMON|TROPICAL_FISH|PUFFERFISH|COOKED_COD|COOKED_SALMON
COPPER_BULB|EXPOSED_COPPER_BULB|WEATHERED_COPPER_BULB|OXIDIZED_COPPER_BULB|WAXED_COPPER_BULB|WAXED_EXPOSED_COPPER_BULB|WAXED_WEATHERED_COPPER_BULB|WAXED_OXIDIZED_COPPER_BULB
COPPER_GOLEM_STATUE|EXPOSED_COPPER_GOLEM_STATUE|OXIDIZED_COPPER_GOLEM_STATUE|WAXED_COPPER_GOLEM_STATUE|WAXED_EXPOSED_COPPER_GOLEM_STATUE|WAXED_OXIDIZED_COPPER_GOLEM_STATUE|WAXED_WEATHERED_COPPER_GOLEM_STATUE|WEATHERED_COPPER_GOLEM_STATUE
COPPER_HORSE_ARMOR
COPPER_NAUTILUS_ARMOR|DIAMOND_NAUTILUS_ARMOR|GOLDEN_NAUTILUS_ARMOR|IRON_NAUTILUS_ARMOR|NETHERITE_NAUTILUS_ARMOR
CRAFTER
CRIMSON_FUNGUS|WARPED_FUNGUS|CRIMSON_ROOTS|WARPED_ROOTS|NETHER_SPROUTS|WEEPING_VINES|TWISTING_VINES
CRIMSON_PLANKS|CRIMSON_STEM|STRIPPED_CRIMSON_STEM|CRIMSON_SLAB|CRIMSON_FENCE|CRIMSON_STAIRS|CRIMSON_BUTTON|CRIMSON_DOOR|CRIMSON_TRAPDOOR|CRIMSON_FENCE_GATE|CRIMSON_SIGN|CRIMSON_HANGING_SIGN|NETHER_WART_BLOCK|CRIMSON_SHELF
CRYING_OBSIDIAN|RESPAWN_ANCHOR
DARK_OAK_PLANKS|DARK_OAK_SAPLING|DARK_OAK_LOG|STRIPPED_DARK_OAK_LOG|DARK_OAK_LEAVES|DARK_OAK_SLAB|DARK_OAK_FENCE|DARK_OAK_STAIRS|DARK_OAK_BUTTON|DARK_OAK_DOOR|DARK_OAK_TRAPDOOR|DARK_OAK_FENCE_GATE|DARK_OAK_BOAT|DARK_OAK_CHEST_BOAT|DARK_OAK_SIGN|DARK_OAK_HANGING_SIGN|DARK_OAK_SHELF
DEAD_TUBE_CORAL_BLOCK|DEAD_BRAIN_CORAL_BLOCK|DEAD_BUBBLE_CORAL_BLOCK|DEAD_FIRE_CORAL_BLOCK|DEAD_HORN_CORAL_BLOCK|TUBE_CORAL_BLOCK|BRAIN_CORAL_BLOCK|BUBBLE_CORAL_BLOCK|FIRE_CORAL_BLOCK|HORN_CORAL_BLOCK|TUBE_CORAL|BRAIN_CORAL|BUBBLE_CORAL|FIRE_CORAL|HORN_CORAL|DEAD_BRAIN_CORAL|DEAD_BUBBLE_CORAL|DEAD_FIRE_CORAL|DEAD_HORN_CORAL|DEAD_TUBE_CORAL|TUBE_CORAL_FAN|BRAIN_CORAL_FAN|BUBBLE_CORAL_FAN|FIRE_CORAL_FAN|HORN_CORAL_FAN|DEAD_TUBE_CORAL_FAN|DEAD_BRAIN_CORAL_FAN|DEAD_BUBBLE_CORAL_FAN|DEAD_FIRE_CORAL_FAN|DEAD_HORN_CORAL_FAN
DEEPSLATE|COBBLED_DEEPSLATE|POLISHED_DEEPSLATE|DEEPSLATE_BRICKS|CRACKED_DEEPSLATE_BRICKS|DEEPSLATE_TILES|CRACKED_DEEPSLATE_TILES|CHISELED_DEEPSLATE|REINFORCED_DEEPSLATE|COBBLED_DEEPSLATE_WALL|POLISHED_DEEPSLATE_WALL|DEEPSLATE_BRICK_WALL|DEEPSLATE_TILE_WALL|COBBLED_DEEPSLATE_STAIRS|POLISHED_DEEPSLATE_STAIRS|DEEPSLATE_BRICK_STAIRS|DEEPSLATE_TILE_STAIRS|COBBLED_DEEPSLATE_SLAB|POLISHED_DEEPSLATE_SLAB|DEEPSLATE_BRICK_SLAB|DEEPSLATE_TILE_SLAB
DIAMOND|DIAMOND_BLOCK|DIAMOND_SWORD|DIAMOND_SHOVEL|DIAMOND_PICKAXE|DIAMOND_AXE|DIAMOND_HOE|DIAMOND_HELMET|DIAMOND_CHESTPLATE|DIAMOND_LEGGINGS|DIAMOND_BOOTS|JUKEBOX|DIAMOND_SPEAR
DIORITE|POLISHED_DIORITE|DIORITE_WALL|POLISHED_DIORITE_STAIRS|DIORITE_STAIRS|POLISHED_DIORITE_SLAB|DIORITE_SLAB
DIRT|GRASS_BLOCK|COARSE_DIRT|PODZOL|MYCELIUM
DRAGON_BREATH|TIPPED_ARROW|LINGERING_POTION
DRAGON_EGG
DRIED_GHAST
DRIPSTONE_BLOCK|POINTED_DRIPSTONE
ECHO_SHARD|RECOVERY_COMPASS
EGG|BLUE_EGG|BROWN_EGG|CAKE
ELYTRA
EMERALD|EMERALD_BLOCK
ENDER_PEARL|ENDER_EYE|OBSIDIAN|ENCHANTING_TABLE|ENDER_CHEST
END_STONE|END_STONE_BRICKS|END_STONE_BRICK_WALL|END_STONE_BRICK_STAIRS|END_STONE_BRICK_SLAB
EXPERIENCE_BOTTLE
FEATHER|ARROW
FISHING_ROD|CARROT_ON_A_STICK|WARPED_FUNGUS_ON_A_STICK
FLOWER_BANNER_PATTERN|CREEPER_BANNER_PATTERN|SKULL_BANNER_PATTERN|MOJANG_BANNER_PATTERN|GLOBE_BANNER_PATTERN|PIGLIN_BANNER_PATTERN|FLOW_BANNER_PATTERN|GUSTER_BANNER_PATTERN|FIELD_MASONED_BANNER_PATTERN|BORDURE_INDENTED_BANNER_PATTERN
GHAST_TEAR|END_CRYSTAL
GLOWSTONE|REDSTONE_LAMP|GLOWSTONE_DUST|SPECTRAL_ARROW
GLOW_BERRIES
GLOW_INK_SAC|GLOW_ITEM_FRAME
GOAT_HORN
GRANITE|POLISHED_GRANITE|GRANITE_WALL|POLISHED_GRANITE_STAIRS|GRANITE_STAIRS|POLISHED_GRANITE_SLAB|GRANITE_SLAB
GRAVEL|FLINT|FLETCHING_TABLE
GREEN_DYE|LIME_DYE|CYAN_DYE|CACTUS|SEA_PICKLE
GUNPOWDER|TNT|FIREWORK_ROCKET|FIREWORK_STAR
HEAVY_CORE|MACE
HONEYCOMB|HONEY_BLOCK|BEE_NEST|BEEHIVE|HONEY_BOTTLE|HONEYCOMB_BLOCK|CANDLE|WHITE_CANDLE|ORANGE_CANDLE|MAGENTA_CANDLE|LIGHT_BLUE_CANDLE|YELLOW_CANDLE|LIME_CANDLE|PINK_CANDLE|GRAY_CANDLE|LIGHT_GRAY_CANDLE|CYAN_CANDLE|PURPLE_CANDLE|BLUE_CANDLE|BROWN_CANDLE|GREEN_CANDLE|RED_CANDLE|BLACK_CANDLE
ICE|PACKED_ICE|BLUE_ICE
IRON_HORSE_ARMOR|GOLDEN_HORSE_ARMOR|DIAMOND_HORSE_ARMOR
JUNGLE_PLANKS|JUNGLE_SAPLING|JUNGLE_LOG|STRIPPED_JUNGLE_LOG|JUNGLE_LEAVES|JUNGLE_SLAB|JUNGLE_FENCE|JUNGLE_STAIRS|JUNGLE_BUTTON|JUNGLE_DOOR|JUNGLE_TRAPDOOR|JUNGLE_FENCE_GATE|JUNGLE_BOAT|JUNGLE_CHEST_BOAT|JUNGLE_SIGN|JUNGLE_HANGING_SIGN|JUNGLE_SHELF
KELP|DRIED_KELP|DRIED_KELP_BLOCK
LEAF_LITTER|WILDFLOWERS|BUSH|FIREFLY_BUSH|CACTUS_FLOWER|SHORT_DRY_GRASS|TALL_DRY_GRASS
LEATHER|LEATHER_HELMET|LEATHER_CHESTPLATE|LEATHER_LEGGINGS|LEATHER_BOOTS|ITEM_FRAME|LEATHER_HORSE_ARMOR
LILY_PAD
MAGMA_BLOCK
MANGROVE_PLANKS|MANGROVE_PROPAGULE|MANGROVE_LOG|MANGROVE_ROOTS|MUDDY_MANGROVE_ROOTS|STRIPPED_MANGROVE_LOG|MANGROVE_LEAVES|MANGROVE_SLAB|MANGROVE_FENCE|MANGROVE_STAIRS|MANGROVE_BUTTON|MANGROVE_DOOR|MANGROVE_TRAPDOOR|MANGROVE_FENCE_GATE|MANGROVE_BOAT|MANGROVE_CHEST_BOAT|MANGROVE_SIGN|MANGROVE_HANGING_SIGN|MANGROVE_SHELF
MELON|MELON_SEEDS|MELON_SLICE|GLISTERING_MELON_SLICE
MINECART|CHEST_MINECART|FURNACE_MINECART|TNT_MINECART|HOPPER_MINECART
MOSSY_COBBLESTONE|MOSSY_STONE_BRICKS|MOSSY_COBBLESTONE_WALL|MOSSY_STONE_BRICK_WALL|MOSSY_STONE_BRICK_STAIRS|MOSSY_COBBLESTONE_STAIRS|MOSSY_STONE_BRICK_SLAB|MOSSY_COBBLESTONE_SLAB
MUD|MUD_BRICK_SLAB|PACKED_MUD|MUD_BRICKS|MUD_BRICK_STAIRS|MUD_BRICK_WALL
MUSIC_DISC_13|MUSIC_DISC_CAT|MUSIC_DISC_BLOCKS|MUSIC_DISC_CHIRP|MUSIC_DISC_FAR|MUSIC_DISC_MALL|MUSIC_DISC_MELLOHI|MUSIC_DISC_STAL|MUSIC_DISC_STRAD|MUSIC_DISC_WARD|MUSIC_DISC_11|MUSIC_DISC_WAIT|MUSIC_DISC_OTHERSIDE|MUSIC_DISC_RELIC|MUSIC_DISC_5|MUSIC_DISC_PIGSTEP|DISC_FRAGMENT_5|MUSIC_DISC_PRECIPICE|MUSIC_DISC_CREATOR|MUSIC_DISC_CREATOR_MUSIC_BOX|MUSIC_DISC_LAVA_CHICKEN|MUSIC_DISC_BOUNCE
MUTTON|COOKED_MUTTON
NAME_TAG
NAUTILUS_SHELL|CONDUIT|HEART_OF_THE_SEA
NETHERITE_UPGRADE_SMITHING_TEMPLATE|SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE|DUNE_ARMOR_TRIM_SMITHING_TEMPLATE|COAST_ARMOR_TRIM_SMITHING_TEMPLATE|WILD_ARMOR_TRIM_SMITHING_TEMPLATE|WARD_ARMOR_TRIM_SMITHING_TEMPLATE|EYE_ARMOR_TRIM_SMITHING_TEMPLATE|VEX_ARMOR_TRIM_SMITHING_TEMPLATE|TIDE_ARMOR_TRIM_SMITHING_TEMPLATE|SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE|RIB_ARMOR_TRIM_SMITHING_TEMPLATE|SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE|WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE|SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE|SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE|RAISER_ARMOR_TRIM_SMITHING_TEMPLATE|HOST_ARMOR_TRIM_SMITHING_TEMPLATE|BOLT_ARMOR_TRIM_SMITHING_TEMPLATE|FLOW_ARMOR_TRIM_SMITHING_TEMPLATE
NETHERRACK|NETHER_BRICK_SLAB|NETHER_BRICKS|CRACKED_NETHER_BRICKS|CHISELED_NETHER_BRICKS|NETHER_BRICK_FENCE|NETHER_BRICK_STAIRS|NETHER_BRICK_WALL|NETHER_BRICK
NETHER_STAR|BEACON|WITHER_ROSE
NETHER_WART|RED_NETHER_BRICK_WALL|RED_NETHER_BRICKS|RED_NETHER_BRICK_STAIRS|RED_NETHER_BRICK_SLAB
OAK_PLANKS|OAK_SAPLING|OAK_LOG|STRIPPED_OAK_LOG|OAK_LEAVES|OAK_SLAB|OAK_FENCE|OAK_STAIRS|OAK_BUTTON|OAK_DOOR|OAK_TRAPDOOR|OAK_FENCE_GATE|OAK_BOAT|OAK_CHEST_BOAT|OAK_SIGN|OAK_HANGING_SIGN|OAK_SHELF
OCHRE_FROGLIGHT|VERDANT_FROGLIGHT|PEARLESCENT_FROGLIGHT
OMINOUS_BOTTLE
PALE_HANGING_MOSS|PALE_MOSS_BLOCK
PALE_OAK_PLANKS|PALE_OAK_SAPLING|PALE_OAK_LOG|STRIPPED_PALE_OAK_LOG|PALE_OAK_LEAVES|PALE_OAK_SLAB|PALE_OAK_FENCE|PALE_OAK_STAIRS|PALE_OAK_BUTTON|PALE_OAK_DOOR|PALE_OAK_TRAPDOOR|PALE_OAK_FENCE_GATE|PALE_OAK_BOAT|PALE_OAK_CHEST_BOAT|PALE_OAK_SIGN|PALE_OAK_HANGING_SIGN|PALE_OAK_SHELF
PHANTOM_MEMBRANE
POPLAR_PLANKS|POPLAR_SAPLING|POPLAR_LOG|STRIPPED_POPLAR_LOG|ORANGE_POPLAR_LEAVES|RED_POPLAR_LEAVES|YELLOW_POPLAR_LEAVES|POPLAR_SLAB|POPLAR_FENCE|POPLAR_STAIRS|POPLAR_BUTTON|POPLAR_DOOR|POPLAR_TRAPDOOR|POPLAR_FENCE_GATE|POPLAR_BOAT|POPLAR_CHEST_BOAT|POPLAR_SIGN|POPLAR_HANGING_SIGN|POPLAR_SHELF|RED_SHRUB|SHELF_MUSHROOM
PORKCHOP|COOKED_PORKCHOP
POTATO|BAKED_POTATO|POISONOUS_POTATO
PRISMARINE|PRISMARINE_SHARD|PRISMARINE_CRYSTALS|PRISMARINE_SLAB|PRISMARINE_BRICK_SLAB|DARK_PRISMARINE_SLAB|PRISMARINE_WALL|PRISMARINE_BRICKS|DARK_PRISMARINE|PRISMARINE_STAIRS|PRISMARINE_BRICK_STAIRS|DARK_PRISMARINE_STAIRS|SEA_LANTERN
PUMPKIN|PUMPKIN_SEEDS|CARVED_PUMPKIN|JACK_O_LANTERN|PUMPKIN_PIE
QUARTZ|QUARTZ_SLAB|SMOOTH_QUARTZ|CHISELED_QUARTZ_BLOCK|QUARTZ_BLOCK|QUARTZ_BRICKS|QUARTZ_PILLAR|QUARTZ_STAIRS|SMOOTH_QUARTZ_STAIRS|SMOOTH_QUARTZ_SLAB|DAYLIGHT_DETECTOR|COMPARATOR|OBSERVER
RABBIT|COOKED_RABBIT|RABBIT_STEW|RABBIT_FOOT|RABBIT_HIDE
RAIL|POWERED_RAIL|DETECTOR_RAIL|ACTIVATOR_RAIL
RAW_COPPER|COPPER_INGOT|COPPER_NUGGET|RAW_COPPER_BLOCK|COPPER_BLOCK|EXPOSED_COPPER|WEATHERED_COPPER|OXIDIZED_COPPER|CUT_COPPER|EXPOSED_CUT_COPPER|WEATHERED_CUT_COPPER|OXIDIZED_CUT_COPPER|CUT_COPPER_STAIRS|EXPOSED_CUT_COPPER_STAIRS|WEATHERED_CUT_COPPER_STAIRS|OXIDIZED_CUT_COPPER_STAIRS|CUT_COPPER_SLAB|EXPOSED_CUT_COPPER_SLAB|WEATHERED_CUT_COPPER_SLAB|OXIDIZED_CUT_COPPER_SLAB|LIGHTNING_ROD|EXPOSED_LIGHTNING_ROD|OXIDIZED_LIGHTNING_ROD|WAXED_LIGHTNING_ROD|WAXED_EXPOSED_LIGHTNING_ROD|WAXED_OXIDIZED_LIGHTNING_ROD|WAXED_WEATHERED_LIGHTNING_ROD|WEATHERED_LIGHTNING_ROD|CHISELED_COPPER|EXPOSED_CHISELED_COPPER|WEATHERED_CHISELED_COPPER|OXIDIZED_CHISELED_COPPER|WAXED_CHISELED_COPPER|WAXED_EXPOSED_CHISELED_COPPER|WAXED_WEATHERED_CHISELED_COPPER|WAXED_OXIDIZED_CHISELED_COPPER|COPPER_DOOR|EXPOSED_COPPER_DOOR|WEATHERED_COPPER_DOOR|OXIDIZED_COPPER_DOOR|WAXED_COPPER_DOOR|WAXED_EXPOSED_COPPER_DOOR|WAXED_WEATHERED_COPPER_DOOR|WAXED_OXIDIZED_COPPER_DOOR|COPPER_GRATE|EXPOSED_COPPER_GRATE|WEATHERED_COPPER_GRATE|OXIDIZED_COPPER_GRATE|WAXED_COPPER_GRATE|WAXED_EXPOSED_COPPER_GRATE|WAXED_WEATHERED_COPPER_GRATE|WAXED_OXIDIZED_COPPER_GRATE|COPPER_TRAPDOOR|EXPOSED_COPPER_TRAPDOOR|WEATHERED_COPPER_TRAPDOOR|OXIDIZED_COPPER_TRAPDOOR|WAXED_COPPER_TRAPDOOR|WAXED_EXPOSED_COPPER_TRAPDOOR|WAXED_WEATHERED_COPPER_TRAPDOOR|WAXED_OXIDIZED_COPPER_TRAPDOOR|WAXED_CUT_COPPER|WAXED_EXPOSED_CUT_COPPER|WAXED_WEATHERED_CUT_COPPER|WAXED_OXIDIZED_CUT_COPPER|WAXED_CUT_COPPER_STAIRS|WAXED_EXPOSED_CUT_COPPER_STAIRS|WAXED_WEATHERED_CUT_COPPER_STAIRS|WAXED_OXIDIZED_CUT_COPPER_STAIRS|WAXED_CUT_COPPER_SLAB|WAXED_EXPOSED_CUT_COPPER_SLAB|WAXED_WEATHERED_CUT_COPPER_SLAB|WAXED_OXIDIZED_CUT_COPPER_SLAB|WAXED_COPPER_BLOCK|WAXED_EXPOSED_COPPER|WAXED_WEATHERED_COPPER|WAXED_OXIDIZED_COPPER|COPPER_BARS|EXPOSED_COPPER_BARS|OXIDIZED_COPPER_BARS|WAXED_COPPER_BARS|WAXED_EXPOSED_COPPER_BARS|WAXED_OXIDIZED_COPPER_BARS|WAXED_WEATHERED_COPPER_BARS|WEATHERED_COPPER_BARS|COPPER_CHAIN|EXPOSED_COPPER_CHAIN|OXIDIZED_COPPER_CHAIN|WAXED_COPPER_CHAIN|WAXED_EXPOSED_COPPER_CHAIN|WAXED_OXIDIZED_COPPER_CHAIN|WAXED_WEATHERED_COPPER_CHAIN|WEATHERED_COPPER_CHAIN|COPPER_CHEST|EXPOSED_COPPER_CHEST|OXIDIZED_COPPER_CHEST|WAXED_COPPER_CHEST|WAXED_EXPOSED_COPPER_CHEST|WAXED_OXIDIZED_COPPER_CHEST|WAXED_WEATHERED_COPPER_CHEST|WEATHERED_COPPER_CHEST|COPPER_TORCH|COPPER_LANTERN|EXPOSED_COPPER_LANTERN|OXIDIZED_COPPER_LANTERN|WAXED_COPPER_LANTERN|WAXED_EXPOSED_COPPER_LANTERN|WAXED_OXIDIZED_COPPER_LANTERN|WAXED_WEATHERED_COPPER_LANTERN|WEATHERED_COPPER_LANTERN|COPPER_HELMET|COPPER_CHESTPLATE|COPPER_LEGGINGS|COPPER_BOOTS|COPPER_AXE|COPPER_HOE|COPPER_PICKAXE|COPPER_SHOVEL|COPPER_SWORD|COPPER_SPEAR
RAW_GOLD|GOLD_INGOT|RAW_GOLD_BLOCK|GOLD_BLOCK|GOLDEN_SWORD|GOLDEN_SHOVEL|GOLDEN_PICKAXE|GOLDEN_AXE|GOLDEN_HOE|GOLDEN_HELMET|GOLDEN_CHESTPLATE|GOLDEN_LEGGINGS|GOLDEN_BOOTS|CLOCK|GOLD_NUGGET|GOLDEN_SPEAR
RAW_IRON|IRON_INGOT|RAW_IRON_BLOCK|IRON_BLOCK|IRON_BARS|IRON_CHAIN|ANVIL|HOPPER|TRIPWIRE_HOOK|TRAPPED_CHEST|IRON_DOOR|IRON_TRAPDOOR|FLINT_AND_STEEL|IRON_SWORD|IRON_SHOVEL|IRON_PICKAXE|IRON_AXE|IRON_HOE|IRON_HELMET|IRON_CHESTPLATE|IRON_LEGGINGS|IRON_BOOTS|BUCKET|SHEARS|CAULDRON|SHIELD|IRON_NUGGET|BLAST_FURNACE|SMITHING_TABLE|STONECUTTER|LANTERN|LODESTONE|IRON_SPEAR
REDSTONE|REDSTONE_TORCH|REDSTONE_BLOCK|REPEATER|PISTON|DROPPER|DISPENSER|NOTE_BLOCK|COMPASS
RED_DYE|BEETROOT|BEETROOT_SEEDS|BEETROOT_SOUP|PINK_DYE|ORANGE_DYE|YELLOW_DYE|DANDELION|POPPY|RED_TULIP|ORANGE_TULIP|PINK_TULIP|ROSE_BUSH|PINK_PETALS|PEONY|SUNFLOWER|GOLDEN_DANDELION
RED_SAND|RED_SANDSTONE|RED_SANDSTONE_SLAB|CUT_RED_SANDSTONE_SLAB|SMOOTH_RED_SANDSTONE|RED_SANDSTONE_WALL|CHISELED_RED_SANDSTONE|CUT_RED_SANDSTONE|RED_SANDSTONE_STAIRS|SMOOTH_RED_SANDSTONE_STAIRS|SMOOTH_RED_SANDSTONE_SLAB
RESIN_CLUMP|RESIN_BLOCK|RESIN_BRICK|RESIN_BRICK_SLAB|RESIN_BRICK_STAIRS|RESIN_BRICK_WALL|CHISELED_RESIN_BRICKS
ROTTEN_FLESH
SADDLE
SAND|SANDSTONE|CHISELED_SANDSTONE|CUT_SANDSTONE|GLASS|SANDSTONE_SLAB|CUT_SANDSTONE_SLAB|SMOOTH_SANDSTONE|SANDSTONE_STAIRS|SANDSTONE_WALL|SMOOTH_SANDSTONE_STAIRS|SMOOTH_SANDSTONE_SLAB|GLASS_BOTTLE|POTION
SCULK|SCULK_VEIN|SCULK_CATALYST|SCULK_SHRIEKER|SCULK_SENSOR|CALIBRATED_SCULK_SENSOR
SHORT_GRASS|FERN|DEAD_BUSH|SEAGRASS|TALL_GRASS|LARGE_FERN|VINE|GLOW_LICHEN
SHROOMLIGHT
SHULKER_SHELL|SHULKER_BOX|WHITE_SHULKER_BOX|ORANGE_SHULKER_BOX|MAGENTA_SHULKER_BOX|LIGHT_BLUE_SHULKER_BOX|YELLOW_SHULKER_BOX|LIME_SHULKER_BOX|PINK_SHULKER_BOX|GRAY_SHULKER_BOX|LIGHT_GRAY_SHULKER_BOX|CYAN_SHULKER_BOX|PURPLE_SHULKER_BOX|BLUE_SHULKER_BOX|BROWN_SHULKER_BOX|GREEN_SHULKER_BOX|RED_SHULKER_BOX|BLACK_SHULKER_BOX
SKELETON_SKULL|WITHER_SKELETON_SKULL|ZOMBIE_HEAD|CREEPER_HEAD|DRAGON_HEAD
SLIME_BALL|STICKY_PISTON|SLIME_BLOCK|MAGMA_CREAM|LEAD
SNIFFER_EGG|TORCHFLOWER_SEEDS|PITCHER_POD|TORCHFLOWER|PITCHER_PLANT
SNOW|SNOW_BLOCK|SNOWBALL
SOUL_SAND|SOUL_TORCH|SOUL_LANTERN|SOUL_CAMPFIRE|SOUL_SOIL
SPONGE|WET_SPONGE
SPRUCE_PLANKS|SPRUCE_SAPLING|SPRUCE_LOG|STRIPPED_SPRUCE_LOG|SPRUCE_LEAVES|SPRUCE_SLAB|SPRUCE_FENCE|SPRUCE_STAIRS|SPRUCE_BUTTON|SPRUCE_DOOR|SPRUCE_TRAPDOOR|SPRUCE_FENCE_GATE|SPRUCE_BOAT|SPRUCE_CHEST_BOAT|SPRUCE_SIGN|SPRUCE_HANGING_SIGN|SPRUCE_SHELF
STONE|STONE_SLAB|SMOOTH_STONE_SLAB|COBBLESTONE|COBBLESTONE_SLAB|STONE_BRICK_SLAB|SMOOTH_STONE|FURNACE|COBBLESTONE_STAIRS|STONE_BRICKS|CRACKED_STONE_BRICKS|CHISELED_STONE_BRICKS|STONE_BRICK_STAIRS|COBBLESTONE_WALL|STONE_BRICK_WALL|STONE_STAIRS|LEVER|STONE_BUTTON|STONE_SWORD|STONE_SHOVEL|STONE_PICKAXE|STONE_AXE|STONE_HOE|ARMOR_STAND|SMOKER|GRINDSTONE|STONE_SPEAR
STRING|BOW|CROSSBOW|LOOM|SPIDER_EYE|FERMENTED_SPIDER_EYE|LEAD
SUGAR_CANE|PAPER|BOOK|SUGAR|WRITABLE_BOOK|WRITTEN_BOOK|ENCHANTED_BOOK|CARTOGRAPHY_TABLE
SWEET_BERRIES
TERRACOTTA|WHITE_TERRACOTTA|ORANGE_TERRACOTTA|MAGENTA_TERRACOTTA|LIGHT_BLUE_TERRACOTTA|YELLOW_TERRACOTTA|LIME_TERRACOTTA|PINK_TERRACOTTA|GRAY_TERRACOTTA|LIGHT_GRAY_TERRACOTTA|CYAN_TERRACOTTA|PURPLE_TERRACOTTA|BLUE_TERRACOTTA|BROWN_TERRACOTTA|GREEN_TERRACOTTA|RED_TERRACOTTA|BLACK_TERRACOTTA
TOTEM_OF_UNDYING
TRIAL_KEY|OMINOUS_TRIAL_KEY
TRIDENT
TURTLE_EGG|TURTLE_HELMET|TURTLE_SCUTE
WARPED_PLANKS|WARPED_STEM|STRIPPED_WARPED_STEM|WARPED_SLAB|WARPED_FENCE|WARPED_STAIRS|WARPED_BUTTON|WARPED_DOOR|WARPED_TRAPDOOR|WARPED_FENCE_GATE|WARPED_SIGN|WARPED_HANGING_SIGN|WARPED_WART_BLOCK|WARPED_SHELF
WATER_BUCKET|LAVA_BUCKET|POWDER_SNOW_BUCKET|MILK_BUCKET|PUFFERFISH_BUCKET|SALMON_BUCKET|COD_BUCKET|TROPICAL_FISH_BUCKET|AXOLOTL_BUCKET|TADPOLE_BUCKET
WHEAT_SEEDS|WHEAT|BREAD|HAY_BLOCK|TARGET|STRAW_BED
WHITE_CONCRETE|ORANGE_CONCRETE|MAGENTA_CONCRETE|LIGHT_BLUE_CONCRETE|YELLOW_CONCRETE|LIME_CONCRETE|PINK_CONCRETE|GRAY_CONCRETE|LIGHT_GRAY_CONCRETE|CYAN_CONCRETE|PURPLE_CONCRETE|BLUE_CONCRETE|BROWN_CONCRETE|GREEN_CONCRETE|RED_CONCRETE|BLACK_CONCRETE|WHITE_CONCRETE_POWDER|ORANGE_CONCRETE_POWDER|MAGENTA_CONCRETE_POWDER|LIGHT_BLUE_CONCRETE_POWDER|YELLOW_CONCRETE_POWDER|LIME_CONCRETE_POWDER|PINK_CONCRETE_POWDER|GRAY_CONCRETE_POWDER|LIGHT_GRAY_CONCRETE_POWDER|CYAN_CONCRETE_POWDER|PURPLE_CONCRETE_POWDER|BLUE_CONCRETE_POWDER|BROWN_CONCRETE_POWDER|GREEN_CONCRETE_POWDER|RED_CONCRETE_POWDER|BLACK_CONCRETE_POWDER|WHITE_CONCRETE_SLAB|ORANGE_CONCRETE_SLAB|MAGENTA_CONCRETE_SLAB|LIGHT_BLUE_CONCRETE_SLAB|YELLOW_CONCRETE_SLAB|LIME_CONCRETE_SLAB|PINK_CONCRETE_SLAB|GRAY_CONCRETE_SLAB|LIGHT_GRAY_CONCRETE_SLAB|CYAN_CONCRETE_SLAB|PURPLE_CONCRETE_SLAB|BLUE_CONCRETE_SLAB|BROWN_CONCRETE_SLAB|GREEN_CONCRETE_SLAB|RED_CONCRETE_SLAB|BLACK_CONCRETE_SLAB|WHITE_CONCRETE_STAIRS|ORANGE_CONCRETE_STAIRS|MAGENTA_CONCRETE_STAIRS|LIGHT_BLUE_CONCRETE_STAIRS|YELLOW_CONCRETE_STAIRS|LIME_CONCRETE_STAIRS|PINK_CONCRETE_STAIRS|GRAY_CONCRETE_STAIRS|LIGHT_GRAY_CONCRETE_STAIRS|CYAN_CONCRETE_STAIRS|PURPLE_CONCRETE_STAIRS|BLUE_CONCRETE_STAIRS|BROWN_CONCRETE_STAIRS|GREEN_CONCRETE_STAIRS|RED_CONCRETE_STAIRS|BLACK_CONCRETE_STAIRS
WHITE_DYE|BONE|BONE_MEAL|BONE_BLOCK|WHITE_TULIP|LILY_OF_THE_VALLEY
WHITE_GLAZED_TERRACOTTA|ORANGE_GLAZED_TERRACOTTA|MAGENTA_GLAZED_TERRACOTTA|LIGHT_BLUE_GLAZED_TERRACOTTA|YELLOW_GLAZED_TERRACOTTA|LIME_GLAZED_TERRACOTTA|PINK_GLAZED_TERRACOTTA|GRAY_GLAZED_TERRACOTTA|LIGHT_GRAY_GLAZED_TERRACOTTA|CYAN_GLAZED_TERRACOTTA|PURPLE_GLAZED_TERRACOTTA|BLUE_GLAZED_TERRACOTTA|BROWN_GLAZED_TERRACOTTA|GREEN_GLAZED_TERRACOTTA|RED_GLAZED_TERRACOTTA|BLACK_GLAZED_TERRACOTTA
WHITE_STAINED_GLASS|ORANGE_STAINED_GLASS|MAGENTA_STAINED_GLASS|LIGHT_BLUE_STAINED_GLASS|YELLOW_STAINED_GLASS|LIME_STAINED_GLASS|PINK_STAINED_GLASS|GRAY_STAINED_GLASS|LIGHT_GRAY_STAINED_GLASS|CYAN_STAINED_GLASS|PURPLE_STAINED_GLASS|BLUE_STAINED_GLASS|BROWN_STAINED_GLASS|GREEN_STAINED_GLASS|RED_STAINED_GLASS|BLACK_STAINED_GLASS
WHITE_WOOL|ORANGE_WOOL|MAGENTA_WOOL|LIGHT_BLUE_WOOL|YELLOW_WOOL|LIME_WOOL|PINK_WOOL|GRAY_WOOL|LIGHT_GRAY_WOOL|CYAN_WOOL|PURPLE_WOOL|BLUE_WOOL|BROWN_WOOL|GREEN_WOOL|RED_WOOL|BLACK_WOOL|WHITE_WOOL_SLAB|ORANGE_WOOL_SLAB|MAGENTA_WOOL_SLAB|LIGHT_BLUE_WOOL_SLAB|YELLOW_WOOL_SLAB|LIME_WOOL_SLAB|PINK_WOOL_SLAB|GRAY_WOOL_SLAB|LIGHT_GRAY_WOOL_SLAB|CYAN_WOOL_SLAB|PURPLE_WOOL_SLAB|BLUE_WOOL_SLAB|BROWN_WOOL_SLAB|GREEN_WOOL_SLAB|RED_WOOL_SLAB|BLACK_WOOL_SLAB|WHITE_WOOL_STAIRS|ORANGE_WOOL_STAIRS|MAGENTA_WOOL_STAIRS|LIGHT_BLUE_WOOL_STAIRS|YELLOW_WOOL_STAIRS|LIME_WOOL_STAIRS|PINK_WOOL_STAIRS|GRAY_WOOL_STAIRS|LIGHT_GRAY_WOOL_STAIRS|CYAN_WOOL_STAIRS|PURPLE_WOOL_STAIRS|BLUE_WOOL_STAIRS|BROWN_WOOL_STAIRS|GREEN_WOOL_STAIRS|RED_WOOL_STAIRS|BLACK_WOOL_STAIRS|PAINTING|WHITE_BED|ORANGE_BED|MAGENTA_BED|LIGHT_BLUE_BED|YELLOW_BED|LIME_BED|PINK_BED|GRAY_BED|LIGHT_GRAY_BED|CYAN_BED|PURPLE_BED|BLUE_BED|BROWN_BED|GREEN_BED|RED_BED|BLACK_BED|WHITE_CUSHION|ORANGE_CUSHION|MAGENTA_CUSHION|LIGHT_BLUE_CUSHION|YELLOW_CUSHION|LIME_CUSHION|PINK_CUSHION|GRAY_CUSHION|LIGHT_GRAY_CUSHION|CYAN_CUSHION|PURPLE_CUSHION|BLUE_CUSHION|BROWN_CUSHION|GREEN_CUSHION|RED_CUSHION|BLACK_CUSHION```

### Blacklist

BEACON BLACK_SHULKER_BOX BLUE_SHULKER_BOX BROWN_SHULKER_BOX CALIBRATED_SCULK_SENSOR CHISELED_RESIN_BRICKS CHORUS_FLOWER CHORUS_FRUIT CLOSED_EYEBLOSSOM CONDUIT 
COPPER_GOLEM_STATUE CREAKING_HEART CREEPER_BANNER_PATTERN CREEPER_HEAD CYAN_SHULKER_BOX DRAGON_EGG DRAGON_HEAD ELYTRA END_ROD EXPOSED_COPPER_GOLEM_STATUE 
FLOW_BANNER_PATTERN GRAY_SHULKER_BOX GREEN_SHULKER_BOX GUSTER_BANNER_PATTERN LIGHT_BLUE_SHULKER_BOX LIGHT_GRAY_SHULKER_BOX LIME_SHULKER_BOX MAGENTA_SHULKER_BOX 
MOJANG_BANNER_PATTERN MUSIC_DISC_5 MUSIC_DISC_BOUNCE MUSIC_DISC_CREATOR MUSIC_DISC_CREATOR_MUSIC_BOX MUSIC_DISC_LAVA_CHICKEN MUSIC_DISC_OTHERSIDE 
MUSIC_DISC_PIGSTEP MUSIC_DISC_PRECIPICE NETHERITE_AXE NETHERITE_BLOCK NETHERITE_BOOTS NETHERITE_CHESTPLATE NETHERITE_HELMET NETHERITE_HOE NETHERITE_HORSE_ARMOR 
NETHERITE_LEGGINGS NETHERITE_NAUTILUS_ARMOR NETHERITE_PICKAXE NETHERITE_SHOVEL NETHERITE_SPEAR NETHERITE_SWORD NETHER_STAR OPEN_EYEBLOSSOM ORANGE_POPLAR_LEAVES 
ORANGE_SHULKER_BOX OXIDIZED_COPPER OXIDIZED_COPPER_BARS OXIDIZED_COPPER_BULB OXIDIZED_COPPER_CHAIN OXIDIZED_COPPER_CHEST OXIDIZED_COPPER_DOOR 
OXIDIZED_COPPER_GOLEM_STATUE OXIDIZED_COPPER_GRATE OXIDIZED_COPPER_LANTERN OXIDIZED_COPPER_TRAPDOOR OXIDIZED_CUT_COPPER OXIDIZED_CUT_COPPER_SLAB 
OXIDIZED_CUT_COPPER_STAIRS OXIDIZED_LIGHTNING_ROD PALE_HANGING_MOSS PALE_MOSS_BLOCK PALE_OAK_BOAT PALE_OAK_BUTTON PALE_OAK_CHEST_BOAT PALE_OAK_DOOR 
PALE_OAK_FENCE PALE_OAK_FENCE_GATE PALE_OAK_HANGING_SIGN PALE_OAK_LEAVES PALE_OAK_LOG PALE_OAK_PLANKS PALE_OAK_SAPLING PALE_OAK_SHELF PALE_OAK_SIGN 
PALE_OAK_SLAB PALE_OAK_STAIRS PALE_OAK_TRAPDOOR PHANTOM_MEMBRANE PIGLIN_BANNER_PATTERN PIGLIN_HEAD PINK_SHULKER_BOX PITCHER_PLANT PITCHER_POD POPLAR_BOAT 
POPLAR_BUTTON POPLAR_CHEST_BOAT POPLAR_DOOR POPLAR_FENCE POPLAR_FENCE_GATE POPLAR_HANGING_SIGN POPLAR_LOG POPLAR_PLANKS POPLAR_SAPLING POPLAR_SHELF POPLAR_SIGN 
POPLAR_SLAB POPLAR_STAIRS POPLAR_TRAPDOOR POPPED_CHORUS_FRUIT PURPLE_SHULKER_BOX PURPUR_BLOCK PURPUR_PILLAR PURPUR_SLAB PURPUR_STAIRS RECOVERY_COMPASS 
RED_POPLAR_LEAVES RED_SHRUB RED_SHULKER_BOX REINFORCED_DEEPSLATE RESIN_BLOCK RESIN_BRICK RESIN_BRICK_SLAB RESIN_BRICK_STAIRS RESIN_BRICK_WALL RESIN_CLUMP 
SHELF_MUSHROOM SHULKER_BOX SHULKER_SHELL SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE SKELETON_SKULL STRIPPED_PALE_OAK_LOG STRIPPED_POPLAR_LOG TALL_GRASS TORCHFLOWER 
TORCHFLOWER_SEEDS TURTLE_HELMET WARD_ARMOR_TRIM_SMITHING_TEMPLATE WAXED_CHISELED_COPPER WAXED_COPPER_BARS WAXED_COPPER_BULB WAXED_COPPER_CHAIN 
WAXED_COPPER_CHEST WAXED_COPPER_DOOR WAXED_COPPER_GOLEM_STATUE WAXED_COPPER_GRATE WAXED_COPPER_TRAPDOOR WAXED_CUT_COPPER WAXED_CUT_COPPER_SLAB 
WAXED_CUT_COPPER_STAIRS WAXED_EXPOSED_CHISELED_COPPER WAXED_EXPOSED_COPPER WAXED_EXPOSED_COPPER_BARS WAXED_EXPOSED_COPPER_BULB WAXED_EXPOSED_COPPER_CHAIN 
WAXED_EXPOSED_COPPER_CHEST WAXED_EXPOSED_COPPER_DOOR WAXED_EXPOSED_COPPER_GOLEM_STATUE WAXED_EXPOSED_COPPER_GRATE WAXED_EXPOSED_COPPER_LANTERN 
WAXED_EXPOSED_COPPER_TRAPDOOR WAXED_EXPOSED_CUT_COPPER WAXED_EXPOSED_CUT_COPPER_SLAB WAXED_EXPOSED_CUT_COPPER_STAIRS WAXED_EXPOSED_LIGHTNING_ROD 
WAXED_LIGHTNING_ROD WAXED_OXIDIZED_CHISELED_COPPER WAXED_OXIDIZED_COPPER WAXED_OXIDIZED_COPPER_BARS WAXED_OXIDIZED_COPPER_BULB WAXED_OXIDIZED_COPPER_CHAIN 
WAXED_OXIDIZED_COPPER_CHEST WAXED_OXIDIZED_COPPER_DOOR WAXED_OXIDIZED_COPPER_GOLEM_STATUE WAXED_OXIDIZED_COPPER_GRATE WAXED_OXIDIZED_COPPER_LANTERN 
WAXED_OXIDIZED_COPPER_TRAPDOOR WAXED_OXIDIZED_CUT_COPPER WAXED_OXIDIZED_CUT_COPPER_SLAB WAXED_OXIDIZED_CUT_COPPER_STAIRS WAXED_OXIDIZED_LIGHTNING_ROD 
WAXED_WEATHERED_CHISELED_COPPER WAXED_WEATHERED_COPPER WAXED_WEATHERED_COPPER_BARS WAXED_WEATHERED_COPPER_BULB WAXED_WEATHERED_COPPER_CHAIN 
WAXED_WEATHERED_COPPER_CHEST WAXED_WEATHERED_COPPER_DOOR WAXED_WEATHERED_COPPER_GOLEM_STATUE WAXED_WEATHERED_COPPER_GRATE WAXED_WEATHERED_COPPER_LANTERN 
WAXED_WEATHERED_COPPER_TRAPDOOR WAXED_WEATHERED_CUT_COPPER WAXED_WEATHERED_CUT_COPPER_SLAB WAXED_WEATHERED_CUT_COPPER_STAIRS WAXED_WEATHERED_LIGHTNING_ROD 
WEATHERED_COPPER_GOLEM_STATUE WEATHERED_COPPER_LANTERN WHITE_SHULKER_BOX WITHER_ROSE YELLOW_POPLAR_LEAVES YELLOW_SHULKER_BOX ZOMBIE_HEAD
