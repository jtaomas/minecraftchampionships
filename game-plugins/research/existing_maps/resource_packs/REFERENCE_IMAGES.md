# REFERENCE_IMAGES — A10 resource packs

Every image below was opened with the read tool during this analysis. Derived sheets are built from the extracted originals with nearest-neighbour scaling and checkerboard behind transparency; labels are added, pixels are not repainted.

## 1. Pack icons

| Image | Source | Shows | Take |
|---|---|---|---|
| `frames/pack_icons.png` | derived from the three `pack.png` files | Clouds2 and Yeti share a 16×16 yellow chevron icon; Space Race is a 128×128 green crowned character on orange | Use as evidence that Clouds2/Yeti are the same author's pack family; replace all icons in any clean-room build |

Originals opened:
- `packs/clouds2/extracted/pack.png` (16×16, yellow/orange chevrons)
- `packs/yeti_set_go/extracted/pack.png` (identical 2,912 bytes)
- `packs/space_race/extracted/Space Race Resource Pack/pack.png` (128×128, green character with gold crown, orange background)

## 2. Clouds 2 block textures

| Image | Source | Shows | Take |
|---|---|---|---|
| `textures/sheet_clouds2_blocks.png` | 12 block PNGs, frame 0, 8× | magenta/pink/purple/red/yellow glazed pads, white glowstone, 5-frame sea lantern, diorite, oak planks, red nether bricks, red stained glass, soul sand | The visual language: saturated pad patterns, flat building colours, white lights |
| `textures/sheet_clouds2_terracotta.png` | 16 terracotta PNGs, 8× | 16 flat team colours with faint horizontal banding | These are team-colour solids, not vanilla terracotta |
| `textures/sheet_clouds2_animation_frames.png` | animation strips, 6× | every frame of magenta (7), pink (7), purple (7), red (7), yellow (16), sea lantern (5) | Exact motion: square pulse, corner spiral, scrolling stripes, upward chevrons, light pulse |
| `textures/sheet_rotten_flesh_energy.png` | `item/rotten_flesh.png`, 12× | two 16×16 frames: red/orange sun vs blue sun | This is the "Energy" item icon; a 2-frame strobe |

Originals opened:
- `packs/clouds2/extracted/assets/minecraft/textures/block/yellow_glazed_terracotta.png` (16×256, 16 frames)
- `.../magenta_glazed_terracotta.png` (16×112, 7 frames)
- `.../red_glazed_terracotta.png` (16×112, 7 frames)
- `.../pink_glazed_terracotta.png` (16×112, 7 frames)
- `.../purple_glazed_terracotta.png` (16×112, 7 frames)
- `.../sea_lantern.png` (16×80, 5 frames)
- `.../glowstone.png` (16×16, flat white)
- `.../red_terracotta.png` (16×16, flat red with bands)
- `.../diorite.png` (16×16, grey speckle)
- `.../oak_planks.png` (16×16, pale planks)
- `.../red_nether_bricks.png` (16×16, flat red brick)
- `.../red_stained_glass.png` (16×16, red lattice)
- `.../soul_sand.png` (16×16, dark brown noise)
- `.../item/rotten_flesh.png` (32×16, two frames)

## 3. Yeti Set Go

| Image | Source | Shows | Take |
|---|---|---|---|
| `textures/sheet_yeti_blocks.png` | 6 block PNGs, frame 0, 8× | exactly the same five pad patterns + white glowstone as Clouds 2 | Yeti is a subset; do not treat it as separate art |

## 4. Space Race

| Image | Source | Shows | Take |
|---|---|---|---|
| `textures/sheet_space_race_blocks.png` | 25 block PNGs, 8× | flat/clean palette: grey checker acacia, dark birch, white/black trapdoor, orange crimson, light dark-oak, tan jungle, red blackstone, grey stone brick/smooth stone, cyan diamond panel, yellow end stone, transparent glass, grey iron bars, white sea lantern, five flat wools, red grid concrete, three transparent glass grids, orange/yellow chevron "magenta" glazed | The pack turns a saturated block palette into a flat, bright, low-noise one; the chevron glazed block is the speed pad |
| `textures/sheet_space_race_items.png` | two item PNGs, 12× | carrot-on-a-stick = glowing red gem; acacia sign = fully transparent | "Restart Attempt" icon; invisible builder sign |
| `textures/sheet_space_race_gui.png` | eight GUI sheets, 2× (16× for the small tiles) | orange/red widgets, hearts/food icons, inventory with crown/shield, recipe book, recipe button, book, orange noise background, blank accessibility sheet | 5 of these are dead in 1.21.1; the inventory/recipe/book three are the only live ones |

Originals opened:
- `packs/space_race/extracted/Space Race Resource Pack/assets/minecraft/textures/block/blackstone.png`
- `.../blackstone_top.png`
- `.../diamond_block.png`
- `.../glass.png`
- `.../black_wool.png`
- `.../gray_wool.png`
- `.../gui/widgets.png`
- `.../gui/icons.png`
- `.../gui/container/inventory.png`
- `.../gui/recipe_book.png`
- `.../gui/recipe_button.png`
- `.../gui/book.png`
- `.../gui/options_background.png`
- `.../gui/accessibility.png`
- `.../item/carrot_on_a_stick.png`
- `.../item/acacia_sign.png`

## 5. Source-file evidence

| Image | Source | Shows | Take |
|---|---|---|---|
| `frames/AnimPad_preview.png` | `packs/clouds2/extracted/assets/minecraft/textures/block/AnimPad.psd`, frame 1, 32×32 | magenta pad pattern at source resolution | The shipped PSD is the author's working file; exclude it from any release |

The PSD was also read for metadata: Photoshop CS6 (Windows), 32×32, 6 frames, XMP present. No author name or licence was found in the XMP.

## 6. Audio evidence

| Image | Source | Shows | Take |
|---|---|---|---|
| `frames/waveforms_clouds2.png` | every distinct OGG in Clouds2 | 13 rows: 4 short `acerace/*` effects, 7 vanilla-path replacements, the 5:10 track in Vorbis and Opus; durations and codecs labelled | Visual proof that the pack's audio is dominated by long files and that several paths are orphans |
| `frames/audio_durations.png` | `data/audio.csv` + vanilla asset objects | pack vs vanilla duration per comparable event; `records/far` 310.52 s vs 174.46 s dominates | Use in any report about pack weight or music licensing |
| `frames/pack_composition.png` | `data/inventory.csv` | Clouds2 is 21.2 MB of sounds; Space Race is 0.1 MB of textures; Yeti is 0.3 MB of language | Shows where the bytes are: music, not art |

OGG metadata (not an image, but the source for the charts): `data/audio.csv`, including codec (`OggVorbis`/`OggOpus`), duration, sample rate, bitrate and the embedded title `Ace Race (From "MC Championship Season 2")`.

## 7. Not imaged

- No in-game screenshots were taken. The review server is read-only for this agent and the packs are not enabled; the task explicitly forbids enabling a server-wide resource pack.
- No blockstate/model renders: the packs ship none; the relevant vanilla blockstate/model JSON was compared as text.
- No audio playback: identification is by codec, duration, path and metadata, not by listening.
