# A10 — Resource-pack forensics

**Scope:** every resource pack found in the nine staged MCC map archives.
**Owner:** A10. **Writes:** this folder only. **Shared tools/sources:** read-only.
**Date:** 2026-11-25.
**Confidence convention:** every claim is tagged **H** (directly observed file, hash, pixel, command or frame), **M** (inference from two sources), or **L** (guess). Unmarked statements in tables are H observations of the listed files.
**Verdict:** three packs exist, not two. Clouds 2 and Yeti Set Go are directly usable archives (Yeti is a byte-subset of Clouds 2); Space Race is a wrapped archive that Minecraft will not detect as a pack. Clouds 2's custom sound index is invalid JSON (H), so its six `acerace.*` events do not register (M); the map's audible feedback is instead driven by OGGs replacing vanilla files. A 5:10 MCC track is duplicated in two codecs and dominates the pack size.

---

## 1. Sources (read-only)

| Pack | Archive | SHA-256 | Bytes | Files | `pack_format` |
|---|---|---|---|---|---|
| Clouds 2 | `...\mcc_maps\Clouds_2_-_Electric_Boogaloo__Download_Version_\Clouds 2 - Electric Boogaloo\resources.zip` | `32463656ab2b6d4130bd547294be42969c6bb018ae7bc6193a358dab319b1ad4` | 22,265,365 | 83 | 15 |
| Space Race | `...\mcc_maps\Space_Race\Space Race\resources.zip.zip` | `e42fe64fd36b33acbb2ca3e3ceb3c76f743faf7d5b85861c2c0a8b10cae61fb9` | 82,469 | 39 | 6 |
| Yeti Set Go | `...\mcc_maps\YetiSetGo\resources.zip` | `03ad22071157222fd8df18c0422f992e2b88b7c7f06f46cf10e34f5123df055d` | 97,599 | 23 | 9 |
| Yeti duplicate | `...\mcc_maps\zipped-map\resources.zip` | `03ad22071157222fd8df18c0422f992e2b88b7c7f06f46cf10e34f5123df055d` | 97,599 | 23 | 9 |

The duplicate is byte-identical (H: same SHA-256); `YetiSetGo.zip` and `zipped-map.zip` are the same map.

**Format vs map version (H/M):** Clouds 2 map 1.20.1 ↔ format 15 (1.20–1.20.1); Yeti map 1.19.2 ↔ format 9 (1.19–1.19.2); Space Race map 1.18.1 ↔ format 6 (1.16.2–1.16.5), so its pack was already old when the map was saved (1.18 uses format 8).

Copies and extractions are in `packs/<slug>/resources.zip` and `packs/<slug>/extracted/`, with original paths preserved. `yeti_zipped_map_dup/` is kept only as hash evidence.

### Other staged worlds

| World | Pack? | Checked |
|---|---|---|
| City Marathon | no `resources.zip`; `advancements.zip` is player advancements + Distant Horizons LOD data, not a resource pack | H |
| Decision Dome | no resource pack; only VanillaTweaks datapacks | H |
| daniellaur's minigames v3 | no pack, no zip | H |
| TGTTOS Badlands | no pack, no zip | H |
| TGTTOS Industry | no pack, no zip | H |
| TGTTOS Pits | no pack, no zip | H |
| Yeti Set Go | yes | H |
| Clouds 2 | yes | H |
| Space Race | yes, but misnamed/wrapped | H |

The server also has a separate `resourcepack/` project (`McmGlyphs`). It is **not** one of the downloaded packs and is out of scope here; `server.properties` has `resource-pack=` empty and `require-resource-pack=false` (H), and no imported server world contains a `resources.zip` (H), so no pack is active on the review server.

---

## 2. Inventory

Full per-file listing with size, SHA-256, type, PNG dimensions, colour mode and CRC32: **`data/inventory.csv`**.
Pack hashes and source paths: **`data/pack_hashes.csv`**.

| Pack | Textures | Models | Blockstates | Sounds | `sounds.json` | Lang | Fonts | Shaders | CIT/OptiFine | Other |
|---|---|---|---|---|---|---|---|---|---|---|
| Clouds 2 | 29 PNG (+7 `.mcmeta`) | 6 | 0 | 36 OGG | 1 (invalid) | 1 (312 KB) | 0 | 0 | 0 | `pack.mcmeta`, `pack.png`, `AnimPad.psd` |
| Yeti Set Go | 7 PNG (+6 `.mcmeta`) | 6 | 0 | 0 | 0 | 1 (312 KB) | 0 | 0 | 0 | `pack.mcmeta`, `pack.png`, `AnimPad.psd` |
| Space Race | 35 PNG (+1 `.mcmeta`) | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | `pack.mcmeta`, `pack.png`, `desktop.ini` |

No pack ships blockstates, fonts, shaders, or OptiFine/CIT files (H: recursive scan for `blockstates`, `font`, `shader`, `cit`, `optifine`, `.properties`). Block models are referenced through **vanilla** blockstates; this is why the glazed-terracotta override works without shipping blockstates.

### 2.1 Clouds 2

- `pack.mcmeta`: `pack_format 15`, description `§c Ace Race Resource Pack` (H).
- 29 textures: 16 flat terracotta colours, 5 animated glazed terracotta, white glowstone, 5-frame sea lantern, diorite, oak planks, red nether bricks, red stained glass, soul sand, and the 2-frame "Energy" item (H).
- 6 models: five `cube_all` glazed-terracotta overrides and one vanilla-identical `item/generated` rotten-flesh model (H).
- 36 OGGs in 10 event families: 4 unique `acerace/*` effects (two also copied under vanilla paths), five unique vanilla-path effect families, and the 5:10 track (H/M).
- `en_us.json`: 311,999 bytes, 5,155 keys, 5,084 identical to 1.19.2, 44 changed, 27 extra, 305 missing. Only gameplay-relevant change: `item.minecraft.rotten_flesh` = **"Energy"** (H).
- `AnimPad.psd`: Photoshop CS6 source, 32×32, 6 frames (H); client ignores it.

### 2.2 Yeti Set Go

Strict byte-subset of Clouds 2 (H: every file except `pack.mcmeta` has a Clouds 2 twin with the same SHA-256):

- same 5 glazed textures + `.mcmeta`, same glowstone, same Energy item + `.mcmeta`, same 6 models, same `en_us.json`, same `AnimPad.psd`, same `pack.png`;
- **no** `sounds/`, **no** `sounds.json`, **no** terracotta/stone/glass/plank textures;
- `pack.mcmeta` is format 9, description `§c Ace Race Animation Example` (H).

It is the "animation example" cut of the same author's pack, not a separate art set.

### 2.3 Space Race

- `pack.mcmeta` is inside a wrapper folder, not at zip root; format 6, description `Space Race Textures.` (H).
- 25 block textures, 8 GUI sheets, 2 item textures, 1 sea-lantern `.mcmeta` (H).
- 5 of 8 GUI sheets are dead in 1.21.1 (see compatibility).
- No lang, models, sounds, fonts, shaders or CIT.
- `desktop.ini` (56 bytes) is Windows junk (H).

---

## 3. Exact asset → block/item → map

The master table is **`data/asset_map.csv`** (108 rows). It separates:

- `own_map_placements` — placements in the world that actually uses the pack;
- `other_maps_same_block` — the same block elsewhere, where the **vanilla** texture is used because that world does not ship the pack;
- `trigger` — the command/datapack/playerdata mechanism;
- `status` — used / unused / orphan / broken / dead;
- `confidence` — H/M/L per row.

Readable summary: **`data/map_asset_matrix.md`**.

### 3.1 Coverage

| Pack | Own world | Placed blocks using an overridden texture | Distinct overridden blocks placed |
|---|---|---|---|
| Clouds 2 | `mcc_clouds2` | **11,152** | 24 of 28 |
| Yeti Set Go | `mcc_yetisetgo` | **584** | 4 of 6 |
| Space Race | `mcc_spacerace` | **28,037** | 20 of 25 |

Counts are decoded block states from every region file, not palette entries (H).

### 3.2 Clouds 2 pads and their art

The command blocks test the block under the player. Grouped by tested block (H: 322 command blocks decoded; only the asset-relevant conditions shown):

| Tested block | Effect | Custom sound event | Art |
|---|---|---|---|
| red / magenta / pink glazed terracotta | speed + jump boost | `block.bell.use` | animated 7-frame spiral/stripe pads |
| yellow glazed terracotta | speed + trident | `block.chain.break` | animated 16-frame upward chevrons |
| orange shulker box | jump boost + speed | `block.candle.extinguish` | vanilla shulker texture |
| lime shulker box | jump boost 6 | `block.cave_vines.break` | vanilla shulker texture |
| green glazed terracotta | slowness / clear | none | **not overridden**; stays vanilla |
| red terracotta (5 layers) | white depth-strider boots | none | flat red team colour |
| cave_air below player | fall sound | `block.chain.step` | n/a |

`purple_glazed_terracotta` is fully modelled and animated but **zero** are placed (H). `glowstone` is overridden but zero placed in Clouds 2/Yeti (H). `brown_terracotta` and `cyan_terracotta` are shipped but zero in Clouds 2; cyan is used in Yeti/City/Badlands with vanilla textures (H).

### 3.3 Space Race trigger blocks

| Block | Count | Command use |
|---|---|---|
| `diamond_block` | 130 | elytra auto-equip: commands test `diamond_block` under the player and replace chest with elytra |
| `magenta_glazed_terracotta` | 723 | speed pad and world-spawn test |
| `red_concrete` | 477 | levitation/speed test |
| `orange_concrete` | (not overridden) | levitation/speed test; vanilla texture |
| `gray_concrete`, `lime_stained_glass` | (not overridden) | kill tests; vanilla textures |
| `glass` | 1,678 | invisible walls/windows |
| `stone_bricks`, `sea_lantern`, `smooth_stone` | 9,177 / 6,831 / 1,351 | build palette |

No Space Race command uses the carrot-on-a-stick, trident or acacia sign; those are saved playerdata only (H).

### 3.4 Sounds

**`data/sounds.csv`** has every event. Summary:

- 44 `playsound` commands, all in Clouds 2, all vanilla IDs.
- No command anywhere uses `acerace.*` (H: all 455 command blocks across nine worlds scanned).
- The pack's audible effect is **file replacement**: `block/bell.use` (18), `block.candle.extinguish` (6), `block.cave_vines.break` (6), `block.chain.break` (6), `block.chain.step` (6), `block.brewing_stand.brew` (1), `music_disc.far` (1).
- Yeti and Space Race ship **no sounds at all** (H).

---

## 4. Custom items, names, lore, model overrides

**`data/item_mapping.csv`** has the full table. There is **no `CustomModelData` anywhere** in any world, container, entity or command (H: all NBT walked). Model changes are global file overrides, not per-item data.

| Map | Item | Name / NBT | Model/texture override | How triggered |
|---|---|---|---|---|
| Clouds 2 / Yeti | `minecraft:rotten_flesh` | **Energy** (lang rename) | `models/item/rotten_flesh.json` + 2-frame red/blue icon | **None found.** No give, container, item frame, armor stand or playerdata reference. Likely a server-plugin item. |
| Space Race | `minecraft:carrot_on_a_stick` | **Restart Attempt** (red, italic false) | red gem icon | Saved `playerdata/00000000-...dat` slot 1 only; no command/datapack |
| Space Race | `minecraft:trident` | **Trident** (yellow, italic false) | vanilla model | Same saved hotbar, slot 0 |
| Space Race | `minecraft:acacia_sign` | no name | fully transparent icon (alpha 0) | Same saved hotbar, slot 23; builder item |
| Clouds 2 | `minecraft:leather_boots` | white (`color:16777215`), depth_strider 255, `HideFlags:1` | vanilla | 30 `item replace` command blocks (6 pads × 5 layers) over red terracotta |

**Containers are empty.** All 225 Clouds 2 shulkers, 156 City Marathon containers and 6 Yeti containers have no `Items` (H). There are also no item frames/armor stands in Clouds 2 or Yeti; City Marathon has 39 item frames but only vanilla blocks/panes (H). So no custom item is distributed through containers or displays in these downloads.

**Lore:** none found anywhere (H).

**Model overrides:** only the five glazed-terracotta `cube_all` models and the rotten-flesh item model. There are no custom item models, no `overrides` predicates, no CIT.

---

## 5. Missing / broken references

Full table: **`data/missing_refs.csv`** (19 rows). The important ones:

| # | Reference | Problem | Severity | Confidence |
|---|---|---|---|---|
| 1 | Clouds 2 `sounds.json` | **Invalid JSON** (`{ "path" }` instead of `[ "path" ]`). Parser rejects it; all six `acerace.*` events then fail to register. | high | H (parse) / M (effect) |
| 2 | `acerace.start` | OGG absent, and the index is invalid | high | H |
| 3 | `acerace.checkpoint` | OGG absent, and the index is invalid | high | H |
| 4 | `block/cave_vines/break5.ogg` | Missing; event has 5 entries | low | H |
| 5 | `block/cherrywood_trapdoor/toggle1-3.ogg` | OggOpus, 310.52 s, 5.27 MB each; Java expects Vorbis; path is valid for `block.cherry_wood_trapdoor.open/close` | high | M |
| 6 | `records/far.ogg` | 310.52 s MCC track, copyright; replaces 174.46 s vanilla disc | high | H |
| 7 | Space Race archive | `resources.zip.zip`, wrapper folder, no root `pack.mcmeta`; not detected | high | H |
| 8 | Space Race GUI | 5 of 8 sheets removed in 1.20.2+/1.21.1 | medium | H |
| 9 | All packs | `pack_format` 15/9/6 vs 34 for 1.21.1; warning, not hard failure | medium | M |
| 10 | Clouds 2/Yeti `en_us.json` | 1.19-era; 305 keys short of 1.19.2; only one line matters | low | H |
| 11 | Clouds 2/Yeti `AnimPad.psd` | Author source shipped; no client effect | low | H |
| 12 | Space Race `desktop.ini` | Windows junk | low | H |
| 13 | Purple glazed, glowstone, Energy, azalea sounds, 5 Space Race block textures | Shipped but unused in their own maps | low–medium | H |

The brief's specific flag — `acerace.start` and `acerace.checkpoint` named but absent — is confirmed (H), but it is the **smaller** half of the problem: the index that names them cannot parse at all.

---

## 6. Pack-format compatibility (summary)

Full detail: **`data/compatibility.md`**. A12 cross-check with exact archive paths and roots: **`data/a12_crosscheck.md`**.

### 6.1 A12 cross-check (verified)

| A12 statement | Result | Exact evidence |
|---|---|---|
| Clouds 2 is format 15 | **confirmed** | `packs/clouds2/extracted/pack.mcmeta` → `"pack_format": 15`; zip roots `assets/`, `pack.mcmeta`, `pack.png` |
| Yeti is format 9 | **confirmed** | `packs/yeti_set_go/extracted/pack.mcmeta` → `"pack_format": 9`; same clean root |
| 1.21.1 requires 34 | **confirmed** | 1.21.1 client `version.json`: `"resource": 34` |
| Space Race is format 6 | **confirmed** | `packs/space_race/extracted/Space Race Resource Pack/pack.mcmeta` → `"pack_format": 6` |
| Space Race is a "nested/double zip" | **partly** | It is a **wrapper folder** inside `resources.zip.zip`, **not** a zip-inside-zip. The only root entry is `Space Race Resource Pack/`; no inner `.zip`/`.jar`. The filename is double-extension. |
| Space Race is invalid as shipped | **confirmed** | No `pack.mcmeta` at zip root; not named `resources.zip`; both facts are independently fatal |
| Clouds 2 references missing `acerace/start.ogg` and `acerace/checkpoint.ogg` | **confirmed, plus worse** | Both OGGs absent; `sounds.json` is also invalid JSON, so all six `acerace.*` events fail to register |

Exact Space Race root layout:

```
resources.zip.zip
└── Space Race Resource Pack/
    ├── pack.mcmeta                 <- not at zip root
    ├── pack.png
    └── assets/minecraft/...
```

The correct root for a world pack is:

```
resources.zip
├── pack.mcmeta                     <- must be here
├── pack.png
└── assets/minecraft/...
```

Nothing was enabled, repacked or ported for this check (H: no `resource-pack=` in `server.properties`; no `resources.zip` in any imported server world).

- 1.21.1 expects resource format **34**; stacks are 15 (1.20.x), 9 (1.19.x), 6 (1.16.x).
- Old packs load with a "made for older version" warning; no hard crash expected (M).
- Block textures, animations, `cube_all` models, the item model and the live GUI sheets still work (H).
- The sound index is invalid (H); the custom events therefore fail to register (M).
- Opus trapdoor files are not a supported codec (M).
- 5 of 8 Space Race GUI sheets are dead in 1.21.1 (H).
- The Space Race archive is not a pack as delivered (H).
- `sounds.json` event paths that **do** work in 1.21.1: bell, candle, cave vines, chain, brewing stand, azalea, music disc far. No 1.20→1.21 path rename affects the used events (H: 1.19.2/1.20.1/1.21.1 `sounds.json` compared).
- `block/cherrywood_trapdoor/toggle1-3` is the correct **file path** for `block.cherry_wood_trapdoor.open/close` in 1.20.1/1.21.1 (H); the original brief's assumption that only the event name matters would miss this.

---

## 7. Licensing (summary)

Full detail: **`data/licensing.md`**.

- **No licence file in any pack** (H). Treat as all rights reserved.
- The **5:10 track is "Ace Race (From \"MC Championship Season 2\")"** (H: OGG title tags), present as one Vorbis file plus three Opus files with the same title and duration. It is 20,795,556 of 22,642,170 bytes (92%). **Do not ship.**
- The 311,999-byte `en_us.json` is Mojang-derived; only `item.minecraft.rotten_flesh = "Energy"` matters. Replace with a one-key override.
- All 71 textures are custom (H: 59 same-size comparisons against 1.16.5–1.21.1 vanilla, none matched; 12 animated/item strips have no same-size vanilla counterpart and were inspected visually); they are the author's art and must be recreated, not copied.
- The only Mojang-identical file is `models/item/rotten_flesh.json` (H); it is a trivial functional model.
- Short SFX provenance is unknown; assume restricted and recreate.
- `AnimPad.psd` is author source; exclude from any release.

---

## 8. Evidence and method

- **Archives:** listed, hashed, extracted with Python `zipfile`; original paths preserved (`packs/*/extracted/`).
- **Textures:** every PNG opened, measured, alpha-checked; contact sheets in `textures/`; all 71 pack textures checked against 1.16.5/1.19.2/1.20.1/1.21.1 vanilla jars (`data/vanilla_pixel_match.csv`). 59 had a same-size vanilla counterpart and none matched; the 12 animated/item strips have no same-size vanilla counterpart. The 3 icons were measured but have no vanilla counterpart.
- **Models/blockstates:** pack JSON parsed; compared with 1.20.1/1.21.1 jar blockstate/model JSON.
- **Language:** parsed and diffed key-by-key against the 1.19.2 jar (`scripts/lang_diff.py`).
- **Sounds:** codec/duration/bitrate via `mutagen`; waveforms via `soundfile` + PIL; compared with official 1.19.2/1.20.1/1.21.1 asset objects and `sounds.json`.
- **Maps:** region block entities, containers, playerdata and entity files decoded with `nbtlib`; 455 command blocks scanned; 11,152 / 584 / 28,037 overridden-block placements counted by decoding section palettes.
- **Scope guard:** no route analysis, no command-block logic beyond the condition/sound/item needed to map an asset; A1/A4 own the map reports.

### Image evidence

- **`REFERENCE_IMAGES.md`** — every cited image, source, what it shows, what to take.
- `frames/pack_icons.png`
- `textures/sheet_clouds2_blocks.png`
- `textures/sheet_clouds2_terracotta.png`
- `textures/sheet_clouds2_animation_frames.png`
- `textures/sheet_rotten_flesh_energy.png`
- `textures/sheet_yeti_blocks.png`
- `textures/sheet_space_race_blocks.png`
- `textures/sheet_space_race_items.png`
- `textures/sheet_space_race_gui.png`
- `frames/AnimPad_preview.png`
- `frames/waveforms_clouds2.png`
- `frames/audio_durations.png`
- `frames/pack_composition.png`

---

## 9. Key findings, numbered

1. **Only three packs exist** among nine worlds; Space Race's `resources.zip.zip` is not in the original brief (H).
2. **Yeti Set Go is a byte-identical subset of Clouds 2** except `pack.mcmeta` (H). There is no separate Yeti art set.
3. **Space Race's pack is not a valid pack archive**: a wrapper folder (`Space Race Resource Pack/`), a double extension (`resources.zip.zip`), and no root `pack.mcmeta` (H). It is not a zip-inside-zip; it is inert unless repacked and renamed.
4. **Clouds 2 `sounds.json` is invalid JSON** (H), so its six `acerace.*` events do not register (M). This is broader than the two missing OGGs.
5. **`acerace.start` and `acerace.checkpoint` OGGs are absent** (H), as flagged; even a repaired index would leave them broken.
6. **The map's audio is driven by vanilla-path file replacement, not by the custom events** (H: 44 commands all use vanilla IDs). The replaced files should still play provided the loader keeps the vanilla index alongside the rejected pack index (M).
7. **A 5:10 MCC track dominates the pack**: 4,959,594-byte Vorbis + 3 × 5,278,654-byte Opus = 20,795,556 of 22,642,170 bytes (92%) (H/M). It is copyrighted and must not ship.
8. **The three Opus files are at a valid cherry-trapdoor path**, so any cherry trapdoor in a pack-enabled world either fails to decode or plays 5 minutes of music (M). No cherry trapdoors are placed in Clouds 2, Yeti, Space Race or City Marathon (H).
9. **Five of eight Space Race GUI sheets are dead in 1.21.1** after the 1.20.2 sprite split (H).
10. **The glazed-terracotta `cube_all` override works in 1.21.1**, but changes all six faces and removes vanilla side-face rotation (H). Purple is modelled but unused.
11. **Clouds 2's only custom-named item is "Energy"** (rotten flesh, lang rename), and **no staged map distributes it** (H). The only other custom names are Space Race playerdata "Restart Attempt" and "Trident" (H).
12. **All 387 containers are empty**; no custom item is hidden in a shulker/chest (H).
13. **No `CustomModelData` and no lore anywhere** (H); customisation is global file override + lang/NBT names.
14. **Coverage is high**: 11,152 Clouds 2 blocks, 584 Yeti blocks and 28,037 Space Race blocks use an overridden texture (H).
15. **Licensing is unresolved and hostile to reuse**: no licence, MCC music, Mojang language dump, author PSD, unknown SFX (H/M).

---

## 10. Folder layout

```
resource_packs/
  README.md                     this dossier
  STATUS.md                     done / blocked / open questions
  REFERENCE_IMAGES.md           image index
  packs/
    clouds2/resources.zip       original archive
    clouds2/extracted/          original paths preserved
    space_race/…                same
    yeti_set_go/…               same
    yeti_zipped_map_dup/…       duplicate hash proof
  textures/                     labelled contact sheets (derived)
  frames/                       icons, PSD preview, waveforms, charts
  data/
    inventory.csv               every file, size, hash, dimensions
    pack_hashes.csv             archive hashes
    pack_meta.csv               format/description/root check
    animation_manifest.csv      14 animated textures: size, frames, frametime
    model_inventory.csv         12 models vs 1.20.1 vanilla
    lang_diff.csv               142 changed/extra language keys
    asset_map.csv               master mapping (108 rows)
    a12_crosscheck.md           A12 findings vs actual archives
    map_asset_matrix.md         readable pack→map matrix
    sounds.csv                  every sound event
    audio.csv                   codec/duration/bitrate metadata
    item_mapping.csv            custom names/lore/models/triggers
    missing_refs.csv            19 broken/unused references
    vanilla_match.csv           hash-level vanilla comparison
    vanilla_pixel_match.csv     pixel-level vanilla comparison
    block_usage.csv             per-map counts for overridden blocks
    custom_items.csv            playerdata/level.dat item names
    command_asset_refs.csv      all 455 command blocks (asset scan)
    sound_refs.csv              44 playsound commands
    item_nbt.csv                item stacks with display/model NBT
    containers.csv              all containers and item counts
    compatibility.md            detailed 1.21.1 analysis
    licensing.md                rights and clean-room notes
    vanilla_compare/            1.19.2/1.20.1/1.21.1 comparison downloads
  scripts/                      all analysis scripts (read-only on sources)
```

---

## 11. Links to other agents (no duplication)

- [A1 — Clouds 2 map dossier](../clouds2/) — route, command blocks, shulkers, signs, video. This dossier only maps assets.
- [A4 — Yeti Set Go dossier](../yeti_set_go/) — route and command logic.
- [A12 — server import verification](../server_import/) — whether/how a pack should be enabled server-side, 1.21 command compatibility.
- [A11 — video analysis](../video_clouds2/) — what the pack looks like in motion.

A1/A4 should link here for the asset inventory rather than copying it (PLAN §3).

---

## 12. Open questions

1. Who authored the glazed-pad art and the short SFX? No metadata, no licence, no URL.
2. Is the 5:10 track licensed to the map author, or lifted from the MCC soundtrack? The title says MCC Season 2; assume not licensed.
3. Was `sounds.json` ever valid, or was it hand-edited after the map's last test? The syntax is a set-like shorthand, suggesting a manual edit.
4. Was `acerace.start`/`checkpoint` intended to be the brewing/azalea sounds? The azalea chime is unused and the brewing sound is 12.9 s.
5. Is the "Energy" item a server-plugin item (MCC stamina) rather than a map item? It is renamed but never distributed.
6. Should a replacement pack use the 1.21.1 GUI sprite atlas or skip GUI theming entirely? 5/8 current files are dead.

See `STATUS.md` for what is blocked.
