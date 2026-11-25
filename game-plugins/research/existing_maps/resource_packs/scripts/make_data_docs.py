#!/usr/bin/env python3
"""Write sounds.csv, item_mapping.csv, missing_refs.csv and map_asset_matrix.md."""
import csv
import os

DATA = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs\data"


def write(name, fields, rows):
    path = os.path.join(DATA, name)
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        w.writerows(rows)
    print(path, len(rows))


sounds = [
    dict(event="acerace.start", declared_path="acerace/start", file_present="no", codec="", seconds="",
         map_trigger="none", triggers=0, status="broken", confidence="H",
         notes="Declared in sounds.json, but the file is absent AND sounds.json is invalid JSON, so the event cannot register."),
    dict(event="acerace.checkpoint", declared_path="acerace/checkpoint", file_present="no", codec="", seconds="",
         map_trigger="none", triggers=0, status="broken", confidence="H",
         notes="Same as start: absent file plus invalid index."),
    dict(event="acerace.jumppad", declared_path="acerace/jumppad", file_present="yes", codec="OggVorbis", seconds="0.97",
         map_trigger="none", triggers=0, status="orphan", confidence="H",
         notes="File exists; event unregistered because sounds.json is malformed. Same bytes are installed at block/bell/bell_use01-03."),
    dict(event="acerace.megajumppad", declared_path="acerace/megajumppad", file_present="yes", codec="OggVorbis", seconds="2.51",
         map_trigger="none", triggers=0, status="orphan", confidence="H",
         notes="File exists; event unregistered. Same bytes at block/candle/extinguish1-3."),
    dict(event="acerace.speed", declared_path="acerace/speed", file_present="yes", codec="OggVorbis", seconds="2.32",
         map_trigger="none", triggers=0, status="orphan", confidence="H",
         notes="File exists; event unregistered; not copied to any vanilla path."),
    dict(event="acerace.waterboost", declared_path="acerace/waterboost", file_present="yes", codec="OggVorbis", seconds="1.44",
         map_trigger="none", triggers=0, status="orphan", confidence="H",
         notes="File exists; event unregistered; not copied to any vanilla path."),
    dict(event="block.bell.use", declared_path="block/bell/bell_use01..03", file_present="3 of 3", codec="OggVorbis", seconds="0.97",
         map_trigger="playsound at red/magenta/pink/yellow glazed pads", triggers=18, status="used", confidence="H",
         notes="Vanilla event references only 01/02; 03 is orphaned."),
    dict(event="block.candle.extinguish", declared_path="block/candle/extinguish1..3", file_present="3 of 3", codec="OggVorbis", seconds="2.51",
         map_trigger="playsound at orange shulker mega-jump pads", triggers=6, status="used", confidence="H", notes=""),
    dict(event="block.cave_vines.break", declared_path="block/cave_vines/break1..4", file_present="4 of 5", codec="OggVorbis", seconds="0.35",
         map_trigger="playsound at lime shulker pads", triggers=6, status="partial", confidence="H",
         notes="break5.ogg missing; 1 in 5 vanilla random picks stays vanilla."),
    dict(event="block.chain.break", declared_path="block/chain/break1..4", file_present="4 of 4", codec="OggVorbis", seconds="2.12",
         map_trigger="playsound at yellow glazed speed pads", triggers=6, status="used", confidence="H", notes=""),
    dict(event="block.chain.step", declared_path="block/chain/step1..6", file_present="6 of 6", codec="OggVorbis", seconds="2.11",
         map_trigger="playsound when cave_air below player (falling)", triggers=6, status="used", confidence="H", notes=""),
    dict(event="block.brewing_stand.brew", declared_path="block/brewing_stand/brew1..2", file_present="2 of 2", codec="OggVorbis", seconds="12.91",
         map_trigger="playsound master @a at start area (3,1,22)", triggers=1, status="used", confidence="H", notes=""),
    dict(event="block.azalea.break", declared_path="block/azalea/break1..6", file_present="6 of 6", codec="OggVorbis", seconds="1.14",
         map_trigger="none", triggers=0, status="unused", confidence="H",
         notes="Replaces every azalea break sound but no command plays it."),
    dict(event="block.cherry_wood_trapdoor.open / .close", declared_path="block/cherrywood_trapdoor/toggle1..3", file_present="3 of 3", codec="OggOpus", seconds="310.52",
         map_trigger="none in staged maps (0 cherry trapdoors placed)", triggers=0, status="unused", confidence="M",
         notes="Valid path for cherry trapdoors in 1.20.1/1.21.1. Files are Opus 5:10 music; Java sound playback expects Vorbis, so these will not decode normally."),
    dict(event="music_disc.far", declared_path="records/far", file_present="1 of 1", codec="OggVorbis", seconds="310.52",
         map_trigger="playsound master @a at start area (-35,1,21)", triggers=1, status="used", confidence="H",
         notes="Tagged 'Ace Race (From \"MC Championship Season 2\")'; 174.46 s vanilla replaced by 310.52 s track."),
]
write("sounds.csv", list(sounds[0].keys()), sounds)

items = [
    dict(map="clouds2", item="minecraft:rotten_flesh", display_name="Energy", lore="", custom_model_data="",
         model_override="models/item/rotten_flesh.json + animated texture", trigger="none found",
         evidence="lang item.minecraft.rotten_flesh='Energy'; no give/container/entity/playerdata reference", confidence="H",
         notes="Only custom-named item in either pack's lang. Likely a server-plugin item, not map-supplied."),
    dict(map="space_race", item="minecraft:carrot_on_a_stick", display_name="Restart Attempt", lore="", custom_model_data="",
         model_override="textures/item/carrot_on_a_stick.png (red gem)", trigger="saved playerdata only",
         evidence="playerdata/00000000-...dat Inventory[1]", confidence="H",
         notes="No command block or datapack grants or tests this item; likely a hub/lobby hotbar item."),
    dict(map="space_race", item="minecraft:trident", display_name="Trident", lore="", custom_model_data="",
         model_override="none (vanilla model)", trigger="saved playerdata only",
         evidence="playerdata/00000000-...dat Inventory[0]", confidence="H", notes="Yellow name, italic false."),
    dict(map="space_race", item="minecraft:acacia_sign", display_name="", lore="", custom_model_data="",
         model_override="textures/item/acacia_sign.png (fully transparent)", trigger="saved playerdata only",
         evidence="playerdata/00000000-...dat Inventory[23]; texture alpha=0", confidence="H",
         notes="Invisible item icon; builder hotbar item."),
    dict(map="clouds2", item="minecraft:leather_boots", display_name="", lore="", custom_model_data="",
         model_override="none (vanilla)", trigger="item replace ... armor.feet over red_terracotta",
         evidence="30 command blocks (6 pads x 5 layers) give white boots, depth_strider 255, HideFlags:1", confidence="H",
         notes="No custom name, lore or model; not a pack asset."),
]
write("item_mapping.csv", list(items[0].keys()), items)

missing = [
    dict(ref="sounds.json", kind="invalid_json", where="clouds2 assets/minecraft/sounds.json",
         expected="valid JSON with sounds arrays", actual="uses { \"acerace/start\" } instead of [ \"acerace/start\" ]",
         severity="high", impact="Whole sound index is rejected; all six acerace.* events are unregistered.", confidence="H"),
    dict(ref="acerace.start", kind="missing_file", where="sounds.json -> acerace/start",
         expected="assets/minecraft/sounds/acerace/start.ogg", actual="absent", severity="high",
         impact="Would be silent even if the index were fixed.", confidence="H"),
    dict(ref="acerace.checkpoint", kind="missing_file", where="sounds.json -> acerace/checkpoint",
         expected="assets/minecraft/sounds/acerace/checkpoint.ogg", actual="absent", severity="high",
         impact="Would be silent even if the index were fixed.", confidence="H"),
    dict(ref="block.cave_vines.break", kind="missing_file", where="sounds.json vanilla -> block/cave_vines/break5",
         expected="break5.ogg", actual="only break1-4 shipped", severity="low",
         impact="20% of vanilla random picks fall back to the vanilla crack.", confidence="H"),
    dict(ref="block.bell.use", kind="orphan_file", where="assets/minecraft/sounds/block/bell/bell_use03.ogg",
         expected="not referenced by vanilla sounds.json", actual="present, 13 KB", severity="low",
         impact="Wasted file; no audible effect.", confidence="H"),
    dict(ref="block.azalea.break", kind="unused_override", where="assets/minecraft/sounds/block/azalea/break1-6.ogg",
         expected="some playsound", actual="no command uses this event", severity="low",
         impact="Unused custom chime; possibly intended as checkpoint sound.", confidence="M"),
    dict(ref="block.cherry_wood_trapdoor.open/.close", kind="wrong_codec", where="block/cherrywood_trapdoor/toggle1-3.ogg",
         expected="Ogg Vorbis, short click", actual="OggOpus, 310.52 s music, 5.27 MB each", severity="high",
         impact="Any cherry trapdoor in a world with this pack active cannot decode normally; if a future client supports Opus it would play a 5:10 track.", confidence="M"),
    dict(ref="records/far", kind="licensing", where="assets/minecraft/sounds/records/far.ogg",
         expected="short custom sound or licensed music", actual="310.52 s track titled 'Ace Race (From \"MC Championship Season 2\")'", severity="high",
         impact="Cannot redistribute without MCC/Noxcrew permission; adds 4.96 MB.", confidence="H"),
    dict(ref="cherrywood track", kind="duplicate", where="records/far.ogg vs block/cherrywood_trapdoor/toggle1-3.ogg",
         expected="one encode", actual="same title/duration in Vorbis + three Opus copies", severity="high",
         impact="20.75 MB of the 21.2 MB pack is the same 5:10 track in two codecs.", confidence="M"),
    dict(ref="resources.zip.zip", kind="wrong_archive_root", where="Space_Race/Space Race/resources.zip.zip",
         expected="pack.mcmeta at zip root, file named resources.zip", actual="wrapper folder 'Space Race Resource Pack/' and double .zip extension", severity="high",
         impact="Pack is not auto-detected by the world or a resource-pack slot as-is.", confidence="H"),
    dict(ref="GUI widgets/icons/etc.", kind="removed_texture", where="space_race gui/widgets.png, icons.png, recipe_button.png, options_background.png, accessibility.png",
         expected="referenced by 1.21.1", actual="files removed in 1.20.2+ GUI sprite split", severity="medium",
         impact="5 of 8 GUI overrides do nothing in 1.21.1; only inventory, recipe_book, book remain live.", confidence="H"),
    dict(ref="pack_format", kind="version_mismatch", where="clouds2 15 / yeti 9 / space_race 6",
         expected="34 for 1.21.1", actual="15/9/6", severity="medium",
         impact="Clients show 'made for an older version' and may require manual enable; no hard crash expected.", confidence="M"),
    dict(ref="en_us.json", kind="stale_language", where="clouds2 + yeti assets/minecraft/lang/en_us.json",
         expected="1.21.1 language keys", actual="1.19-era file: 305 keys short of 1.19.2, 27 obsolete extras", severity="low",
         impact="Missing keys fall back to built-in en_us; only the Energy rename matters.", confidence="H"),
    dict(ref="AnimPad.psd", kind="source_file", where="clouds2 + yeti textures/block/AnimPad.psd",
         expected="not shipped", actual="32x32 Photoshop CS6 source, 6 frames, 29 KB", severity="low",
         impact="No client effect; leaks author source and increases size slightly.", confidence="H"),
    dict(ref="desktop.ini", kind="junk_file", where="space_race textures/block/desktop.ini",
         expected="not shipped", actual="56-byte Windows LocalizedFileNames stub", severity="low",
         impact="No effect; evidence the pack was zipped from a Windows folder.", confidence="H"),
    dict(ref="purple glazed terracotta", kind="unused_asset", where="clouds2 + yeti",
         expected="some placement", actual="0 placed in both maps", severity="low",
         impact="Animated model/texture never seen.", confidence="H"),
    dict(ref="glowstone", kind="unused_asset", where="clouds2 + yeti",
         expected="some placement", actual="0 placed in both maps (8 in TGTTOS Pits, no pack)", severity="low",
         impact="White glowstone texture never seen in the pack's own worlds.", confidence="H"),
    dict(ref="Energy / rotten flesh", kind="unused_asset", where="clouds2 + yeti",
         expected="given by map", actual="no command/container/entity/playerdata reference", severity="medium",
         impact="Only custom-named item in the packs is not distributed by these maps.", confidence="H"),
    dict(ref="5 GUI + 20 block textures", kind="unused_asset", where="space_race",
         expected="visible in map", actual="0 placements for acacia_planks, birch_planks, crimson_planks, dark_oak_planks, blackstone_top", severity="low",
         impact="Assets present but not seen in the staged Space Race world.", confidence="H"),
]
write("missing_refs.csv", list(missing[0].keys()), missing)

matrix = """# Pack -> map matrix

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
"""
open(os.path.join(DATA, "map_asset_matrix.md"), "w", encoding="utf-8").write(matrix)
print("wrote map_asset_matrix.md")
