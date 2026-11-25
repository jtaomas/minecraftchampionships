# Licensing and reproduction notes

**Bottom line:** none of the three packs contains a licence, readme, author statement or source URL (H: full zip listings). Absence of a licence means no permission to redistribute. The dossier keeps the extracted files only as internal forensic reference; **nothing here should be shipped, committed to a public repo, or served to players.**

## 1. What is in the packs, by rights holder

| Asset class | Pack(s) | Rights holder / provenance | Can we copy? | Evidence |
|---|---|---|---|---|
| 5:10 track "Ace Race (From \"MC Championship Season 2\")" | Clouds 2, two encodes | MC Championship / Noxcrew (title metadata; no licence) | **No.** Delete before any redistribution. | H: OGG title tags in `records/far.ogg` and `block/cherrywood_trapdoor/toggle1.ogg`; H: 310.52 s both. |
| Short SFX (jump, mega-jump, speed, waterboost, chain, cave-vine, brewing, azalea) | Clouds 2 | Unknown; no metadata except generic Lavc/Lavf encoders. Could be licensed library sounds or hand-made. | **No, until provenance is proven.** Recreate. | H: 4 unique acerace files + 4 unique vanilla-path families; H: no licence file. |
| Block/item textures and animations (29 Clouds 2, 25 Space Race) | Clouds 2, Yeti, Space Race | Pack author's pixel art. Not pixel-identical to any 1.16.5–1.21.1 vanilla texture (H: pixel comparison of every PNG). | **No.** Recreate from the visual description. | H: `vanilla_pixel_match.csv`; H: hashes differ from Mojang jars. |
| Glazed-terracotta models, rotten-flesh model | Clouds 2, Yeti | Functional JSON; rotten-flesh model is byte-identical to 1.16.5 vanilla (H). | The 5-line `cube_all` model is trivial/functional; the vanilla-identical model is Mojang's file. Recreate both. | H: SHA-256 match only for `models/item/rotten_flesh.json`. |
| `en_us.json` (311,999 bytes) | Clouds 2, Yeti | 5,084 strings match 1.19.2 Mojang English; 44 changed; 27 extra; 305 missing (H). Only one gameplay-relevant custom line: `item.minecraft.rotten_flesh = "Energy"`. | **Do not copy the dump.** Ship a one-key override. | H: JSON diff against 1.19.2 jar. |
| `pack.png` icons | All three | Clouds2/Yeti: yellow chevron, same file. Space Race: green crowned character on orange, 128×128. | **No.** Replace with neutral icons. | H: images opened; H: Clouds2/Yeti byte-identical. |
| GUI sheets (widgets/icons/inventory/recipe/book/options/accessibility) | Space Race | Author redraw on Mojang's GUI layout. 5 of 8 are dead in 1.21.1. | **No.** Recreate only the 3 live ones if needed. | H: jar comparison; H: images opened. |
| `AnimPad.psd` | Clouds 2, Yeti | Author's Photoshop CS6 source, 32×32, 6 frames, includes layer names/XMP. | **No.** Do not redistribute source. | H: PSD parsed; H: preview rendered. |
| `desktop.ini` | Space Race | Windows shell stub (`oak_planks.png=@oak_planks.png,0`). | Irrelevant; delete. | H: 56 bytes. |
| Names "Ace Race", "Energy", "Restart Attempt", "Trident" | Clouds 2 / Space Race | Short names; "Ace Race" is MCC branding. | Use generic names (`Jump Pad`, `Boost`, `Checkpoint`) unless MCC branding is licensed. | H: lang/NBT reads. |
| Block/item identities and GUI layout | All | Mojang / Microsoft game design. | Facts and interoperability are fine; do not copy art. | H: vanilla jars. |

## 2. What is Mojang-derived

1. **The language file.** 311,999 bytes of Mojang English with 44 cosmetic edits and one real change (`Rotten Flesh` → `Energy`). Shipping it republishes Mojang's strings. Recreate as:

   ```json
   {
     "item.minecraft.rotten_flesh": "Energy"
   }
   ```

   That is the only line this dossier found in use (and even it is not used by the staged maps).

2. **The rotten-flesh item model.** Byte-identical to 1.16.5 vanilla (H). It is a three-line functional model; write it yourself or omit it and override only the texture.

3. **GUI layout and texture atlas conventions.** The Space Race GUI art follows Mojang's slot/button geometry; the art is the author's, the layout is Mojang's. Recreate with the current 1.21.1 sprite names if a HUD theme is wanted.

4. **Blockstate→model names.** `minecraft:block/<colour>_glazed_terracotta` is a Mojang identifier; using it is interoperability, not copying.

No texture in any pack is pixel-identical to vanilla (H: 59 textures had a same-size 1.16.5–1.21.1 counterpart and none matched; 12 animated/item strips have no same-size vanilla counterpart; the 3 icons have none either; the only exact file match is the rotten-flesh model JSON). So the packs are not "vanilla with a tint"; they are original art that must be recreated.

## 3. What can be copied safely

- **Facts:** `pack_format` numbers, sound-event names, file paths, JSON schema, block IDs, command syntax.
- **Directory layout:** `assets/minecraft/textures/block/...`, `sounds.json`, `models/...`.
- **Technical recipes:** animation strip dimensions (16×112 = 7 frames; 16×256 = 16; 16×80 = 5; 32×16 = 2 side-by-side), `frametime` values, alpha behaviour.
- **The 5-line `cube_all` model pattern** (functional, not creative).
- **Internal forensic copies in this folder**, for review only, not distribution.

## 4. What must be recreated

### 4.1 Pad art

| Effect | Geometry | Animation | Recreate without copying |
|---|---|---|---|
| Jump pad (red/magenta/pink) | 16×16 tile, square spiral / concentric square | 7 frames, `frametime` 2 | Draw a 2-colour concentric square with a 1-px highlight; 7-step rotation. |
| Speed pad (yellow) | 16×16 chevron | 16 frames, `frametime` 1 | Draw two chevrons; offset vertically one pixel per frame. |
| Mega-jump pad | Vanilla orange shulker box (no custom texture) | n/a | Use vanilla block; no art needed. |
| Slow/hazard pad | Vanilla green glazed terracotta (not overridden) | n/a | Use vanilla block. |
| Light | 16×16 white rounded square | 5 frames, `frametime` 5 | Simple white square with 1-px cyan edge. |
| Team colours | 16 flat colours | none | Use vanilla terracotta or concrete; no need for 16 custom files. |

### 4.2 Sounds

| Cue | Length to aim for | Recipe |
|---|---|---|
| Jump | 0.3–0.5 s | short rising sine + noise transient, mono/stereo Vorbis |
| Mega jump | 1.5–2.5 s | pitch-down whoosh + low boom |
| Speed | 0.5–1.0 s loop | filtered noise sweep |
| Checkpoint | 0.3 s | two-note bell (e.g. C6→E6) |
| Start | 1–3 s | three-note countdown or crowd-free riser |
| Water boost | 1.0–1.5 s | bubbly noise + upward filter |
| Fall/void | 1.5–2.0 s | wind noise, low-pass closing |

Use **Ogg Vorbis**, 44.1 kHz stereo, mono for positional sounds; keep files under ~100 KB. Do **not** reuse the 5:10 track or the unnamed SFX.

### 4.3 Energy item

Keep the name if desired; draw a new 16×16 two-frame icon (e.g. a battery or lightning bolt alternating red/blue). The original is a red/orange sun-like flower vs blue version; that specific art is the author's.

### 4.4 HUD theme

Only `container/inventory.png`, `recipe_book.png`, `book.png` are live in 1.21.1 (H). If a theme is wanted, target the 1.21.1 sprite atlas (`textures/gui/sprites/**`) instead of the 1.16 sheets; otherwise 5/8 files do nothing.

### 4.5 Music

Do not use "Ace Race (From \"MC Championship Season 2\")". Options:

1. license the track from MCC/Noxcrew;
2. commission an original 3–5 minute loop;
3. use a CC0/CC-BY track with attribution;
4. no music; use ambient SFX.

The current pack's 310 s track is 20,795,556 of 22,642,170 bytes (92%) across two codecs and is the single biggest legal and size risk.

## 5. Risk table

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Shipping the MCC track | high if pack is copied wholesale | high (DMCA / takedown) | delete `records/far.ogg` and `block/cherrywood_trapdoor/toggle*.ogg` |
| Shipping Mojang `en_us.json` | high if pack is copied | medium | one-key override only |
| Shipping author textures without permission | high | medium | recreate from descriptions; do not copy PNG/PSD |
| Shipping unknown SFX | high | medium | recreate or license |
| Using "Ace Race" branding | medium | medium | generic naming unless licensed |
| Redistributing the PSD/source | low if zips are not published | low | exclude from any release |

## 6. Clean-room checklist for a replacement pack

- [ ] New `pack.mcmeta` with `pack_format: 34` (1.21.1) and a neutral description.
- [ ] New 16×16/128×128 `pack.png`, original art.
- [ ] 5 glazed-terracotta `cube_all` models only if the animated-pad effect is wanted; otherwise use vanilla blocks.
- [ ] New 7/16-frame animation strips and `.mcmeta`; no copied pixels.
- [ ] New short Ogg Vorbis SFX; no 5:10 track.
- [ ] A one-key `en_us.json` for any renamed item.
- [ ] No PSD, no `desktop.ini`, no Mojang language dump.
- [ ] Test on a singleplayer copy, not on the review server.

## 7. Confidence

- H: no licence file; file sizes/hashes; language diff; model byte-match; texture non-match; OGG metadata.
- M: provenance of the short SFX (unknown, so "assume restricted"); whether the two 5:10 files are the same master.
- L: author identity and whether permission could be obtained.
