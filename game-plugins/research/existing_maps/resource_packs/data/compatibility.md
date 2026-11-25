# Pack-format and 1.21.1 compatibility

**Scope:** the three resource packs found in the staged map archives.
**Target server:** Paper 1.21.1, resource-pack format **34**, data-pack format 48 (H: `version.json` in the 1.21.1 client jar; H: `logs/latest.log` says "Starting minecraft server version 1.21.1").
**Activation:** `server.properties` has `resource-pack=` empty and `require-resource-pack=false` (H). None of these packs is active on the review server. A dedicated server does not auto-send a world-folder `resources.zip`; only the singleplayer client does (M).

## 1. Format table

| Pack | `pack_format` | Format's real target | Files | Bytes | On 1.21.1 |
|---|---|---|---|---|---|
| Clouds 2 | 15 | 1.20–1.20.1 | 83 | 22,642,170 | Loads with "made for older version" warning; content paths still valid. |
| Yeti Set Go | 9 | 1.19–1.19.2 | 23 | 368,223 | Same; strict subset of Clouds 2 with an older `pack.mcmeta`. |
| Space Race | 6 | 1.16.2–1.16.5 | 39 | 165,841 | Same, but the archive is wrapped and named `resources.zip.zip`; not detected as a pack as-is. |

`pack_format` values are direct reads (H). The version-to-format mapping is standard Minecraft metadata (M). `1.21.1` expects 34 (H, client `version.json`).

Only Clouds 2 matches its own map's version: Clouds 2 map is 1.20.1 and format 15 is 1.20–1.20.1 (H/M). Yeti map is 1.19.2 and format 9 is 1.19–1.19.2 (H/M). Space Race map is 1.18.1, but its pack is format 6 (1.16.2–1.16.5), so it was already two formats old when the map was saved (1.18 uses format 8; M).

None of the packs declares `supported_formats`. On 1.20.2+ the client uses `pack_format` and flags older packs; it does not hard-refuse them in normal play (M). [A12](../server_import/) owns whether the server should offer any pack; this dossier only states what happens when one is applied.

## 2. What will load

| Asset class | Result on 1.21.1 | Evidence |
|---|---|---|
| Block textures (Clouds 2 29, Space Race 25) | Load. Every block path in the packs still exists in 1.21.1. | H: path names exist in the 1.21.1 jar; H: files are valid PNGs. |
| Animated textures + `.mcmeta` | Load. `frametime` and `frames` format is unchanged. | H: `.mcmeta` parsed; 1.21.1 uses the same animation schema. |
| `models/block/*.json` `cube_all` overrides | Load. 1.21.1 blockstates still point at `minecraft:block/<colour>_glazed_terracotta`. | H: 1.20.1 and 1.21.1 blockstate/model JSON compared byte-for-byte for the reference colour. |
| `models/item/rotten_flesh.json` | Load. 1.21.1 still uses `models/item`; even 1.21.11's `items/rotten_flesh.json` points at `minecraft:item/rotten_flesh`. | H: both jars inspected. |
| `en_us.json` | Loads as an override. Missing 1.21.1 keys fall back to built-in English. | H: 5,084 strings match 1.19.2; 305 keys missing vs 1.19.2; 27 obsolete extras. |
| Live GUI textures | `container/inventory.png`, `recipe_book.png`, `book.png` still exist in 1.21.1. | H: jar listing. |
| Sounds with valid paths | `block.bell.use`, `block.candle.extinguish`, `block.cave_vines.break1-4`, `block.chain.break/step`, `block.brewing_stand.brew`, `block.azalea.break`, `music_disc.far` all still resolve in 1.21.1. | H: 1.21.1 `sounds.json` compared with 1.19.2/1.20.1. |

## 3. What will warn

| Warning | Cause | Severity |
|---|---|---|
| "This pack was made for a different version" | format 15/9/6 vs 34 | cosmetic; user can enable |
| Missing sound `block/cave_vines/break5` | pack ships break1-4 only; vanilla event has five entries | intermittent vanilla crack, 1 in 5 picks (H) |
| Missing `acerace/start`, `acerace/checkpoint` | files absent | only matters if an `acerace.*` event is ever called (H) |
| Invalid `sounds.json` | malformed JSON | logs a parse error; custom events do not register (H) |
| Unsupported OggOpus | cherry-trapdoor files are Opus | decode failure / silence when a cherry trapdoor is used (M) |
| Unused assets | purple glazed, glowstone, azalea sounds, 5 Space Race block textures | no warning; just dead weight (H) |

## 4. What will break

### 4.1 The whole custom sound index (Clouds 2)

`assets/minecraft/sounds.json` is **not valid JSON** (H). It uses object/set syntax:

```json
"acerace.start": { "sounds": { "acerace/start" } }
```

instead of:

```json
"acerace.start": { "sounds": [ "acerace/start" ] }
```

A JSON parser rejects it at line 5 (`Expecting ':' delimiter`). Consequences:

- `acerace.start`, `acerace.checkpoint`, `acerace.jumppad`, `acerace.megajumppad`, `acerace.speed`, `acerace.waterboost` never register (H: file is invalid; M: loader skips the malformed index and keeps vanilla events).
- `acerace/start.ogg` and `acerace/checkpoint.ogg` are absent anyway (H), so even a repaired index would leave two broken events.
- The map does not call `acerace.*` at all (H: 44 `playsound` commands, all vanilla IDs). Its audible feedback comes from **file replacement** at vanilla paths, which still works.

**Practical result:** the map's jump/mega-jump/speed/fall/start audio survives, but every custom named event is dead. This is a different failure from the one in the brief, which assumed only the two OGGs were missing.

### 4.2 The 5:10 music track is duplicated and partly Opus

`records/far.ogg` is a 310.52 s OggVorbis track (H) tagged **"Ace Race (From \"MC Championship Season 2\")"** (H). The same title and duration appear in three Opus files at `block/cherrywood_trapdoor/toggle1-3.ogg`, 5.27 MB each (H). Vanilla 1.20.1 and 1.21.1 use exactly that path for `block.cherry_wood_trapdoor.open` and `.close` (H: sounds.json in both jars), so the files are reachable by any cherry trapdoor.

- Java Edition's sound loader expects Vorbis; Opus is not a supported resource-pack codec (M: observed codec + documented requirement).
- If decoded, a trapdoor click would play 5 minutes of music.
- Zero cherry trapdoors are placed in Clouds 2, Yeti, Space Race or City Marathon (H: region decode), so the bug is latent in these maps.

Size impact: 4,959,594 bytes (Vorbis) + 3 × 5,278,654 bytes (Opus) = **20,795,556 of 22,642,170 bytes (92%)** is the same 5:10 track (M: same title and duration, different codec; audio correlation is low, so they may be different masters, but both are the same licensed composition).

### 4.3 Space Race archive is not a pack

H: the source file is `resources.zip.zip`, and its zip root contains exactly one entry, the directory `Space Race Resource Pack/`. There is **no** inner `.zip` or `.jar`; this is a wrapper folder inside a normal zip, not a zip-inside-zip. `pack.mcmeta` is at `Space Race Resource Pack/pack.mcmeta`, **not** at the zip root.

Four independent facts stop normal use:

1. a world `resources.zip` must have `pack.mcmeta` at its root;
2. the double extension is not the filename Minecraft looks for;
3. the pack is a folder down, so even renaming leaves `pack.mcmeta` in the wrong place;
4. if unpacked into `resourcepacks/`, the same folder depth must be removed.

Renaming alone is insufficient. The exact working layout is:

```
resources.zip
├── pack.mcmeta
├── pack.png
└── assets/minecraft/...
```

The current layout is:

```
resources.zip.zip
└── Space Race Resource Pack/
    ├── pack.mcmeta
    ├── pack.png
    └── assets/minecraft/...
```

### 4.4 Five of eight Space Race GUI textures are dead in 1.21.1

H: 1.21.1 has 422 files under `textures/gui/sprites/` and no longer has these whole-sheet files:

| File | 1.21.1 status |
|---|---|
| `gui/widgets.png` | removed; replaced by `gui/sprites/widget/*` |
| `gui/icons.png` | removed; replaced by sprites |
| `gui/recipe_button.png` | removed |
| `gui/options_background.png` | removed |
| `gui/accessibility.png` | removed |
| `gui/container/inventory.png` | present |
| `gui/recipe_book.png` | present |
| `gui/book.png` | present |

So the orange/red HUD theme is only partly visible on 1.21.1. The pack was authored for 1.16.5 (format 6), before the 1.20.2 sprite split.

### 4.5 Glazed-terracotta model override changes more than the texture

H: the pack replaces each colour model with:

```json
{ "parent": "minecraft:block/cube_all", "textures": { "all": "minecraft:block/<colour>_glazed_terracotta" } }
```

Vanilla uses `template_glazed_terracotta`, which rotates the pattern on side faces. With `cube_all`:

- the animated texture appears on all six faces;
- side-face UV rotation is lost;
- the change applies to every block of that colour while the pack is active, not only pads.

It is a deliberate simplification for animated pads, not a 1.21 break. `cube_all` still exists in 1.21.1 (H).

### 4.6 Slot-machine details

- `block/bell/bell_use03.ogg` is shipped but vanilla `sounds.json` only references 01/02 (H). Dead file.
- `block/cave_vines/break5.ogg` is missing (H). The event has five entries in 1.19.2/1.20.1/1.21.1; 1 in 5 triggers stays vanilla.
- `block/azalea/break1-6.ogg` are custom but no command uses `block.azalea.break` (H). Likely an intended checkpoint chime that was never wired.
- `block/cherrywood_trapdoor/toggle*` path is valid for the **cherry_wood_trapdoor** events, despite the event name not containing `cherrywood` (H: both 1.20.1 and 1.21.1 sounds.json).
- `music_disc.far` still resolves to `records/far` in 1.21.1 (H). The 310 s file will play in full at the start area.

## 5. Sound path matrix (verified against 1.19.2 / 1.20.1 / 1.21.1 `sounds.json`)

| Event | Path used by vanilla | Pack file | 1.21.1 result |
|---|---|---|---|
| `block.bell.use` | `block/bell/bell_use01/02` | 01-03, 0.97 s | works; 03 unused |
| `block.candle.extinguish` | `block/candle/extinguish1/2/3` | 1-3, 2.51 s | works |
| `block.cave_vines.break` | `.../break1..5` | 1-4, 0.35 s | 4/5 works |
| `block.chain.break` | `.../break1..4` | 1-4, 2.12 s | works |
| `block.chain.step` | `.../step1..6` | 1-6, 2.11 s | works |
| `block.brewing_stand.brew` | `.../brew1/2` | 1-2, 12.91 s | works |
| `block.azalea.break` | `.../break1..6` | 1-6, 1.14 s | works but never called |
| `block.cherry_wood_trapdoor.open/.close` | `block/cherrywood_trapdoor/toggle1..3` | 1-3, Opus 310.52 s | decode failure or 5:10 music; no trapdoors in these maps |
| `music_disc.far` | `records/far` | 310.52 s Vorbis | works; full track at start |
| `acerace.*` | custom | invalid index | none register |

## 6. Item-model future note

1.21.1 uses `assets/minecraft/models/item/*.json` (H). 1.21.4+ adds `assets/minecraft/items/*.json` item-model definitions, but 1.21.11's definition for rotten flesh still points at `minecraft:item/rotten_flesh`, so this pack's model override remains effective even there (H). A future rewrite should use the item-definition format only if it wants to target 1.21.4+ semantics; not required for 1.21.1.

## 7. Minimal test plan (not executed; no server change)

1. Client-side only: drop the fixed pack into `.minecraft/resourcepacks/`, enable it in a **singleplayer copy** of the world, not on the review server.
2. Verify: pad textures animate; glazed blocks show the pattern on all faces; `Energy` appears as the rotten-flesh name; cherry trapdoor sound does not hang.
3. Check `logs/latest.log` for `sounds.json` parse errors and Opus decode errors.
4. Do not set `resource-pack=` in `server.properties`; A12 owns server activation.

## 8. Confidence

- H: file existence, sizes, byte hashes, JSON validity, PNG dimensions/alpha, OGG codecs/durations, block counts, command strings, jar paths for 1.20.1/1.21.1/1.21.11.
- M: client warning behaviour for old formats; Opus decode failure; whether a malformed `sounds.json` is skipped while vanilla stays loaded; equivalence of the two 5:10 encodes.
- L: author intent (e.g. azalea as checkpoint sound, purple as a planned pad).
