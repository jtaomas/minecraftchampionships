# STATUS — A10 resource-pack forensics

**State:** COMPLETE for the three packs found. No blockers for the asset inventory.
**Last update:** 2026-11-25.

## Done

- [x] Located every pack among nine staged worlds; found a third pack (Space Race) not named in the brief.
- [x] Hashed and extracted all packs into `packs/` with original paths preserved.
- [x] Inventoried all 168 files (including the Yeti duplicate) with size, SHA-256, kind, PNG dimensions/mode and CRC32.
- [x] Confirmed Yeti is a byte-identical subset of Clouds 2 except `pack.mcmeta`; duplicate `zipped-map` is byte-identical.
- [x] Parsed `pack.mcmeta`, `sounds.json`, all 12 models, all `.mcmeta` animations, and both `en_us.json` files.
- [x] Confirmed no blockstates, fonts, shaders or CIT/OptiFine files exist in any pack.
- [x] Checked all 71 pack textures against 1.16.5/1.19.2/1.20.1/1.21.1 vanilla jars; 59 same-size comparisons, none identical; the 12 animated/item strips have no same-size counterpart. The 3 icons have no vanilla counterpart.
- [x] Diffed the 311,999-byte `en_us.json` key-by-key against 1.19.2; found the single gameplay rename `rotten_flesh = Energy`.
- [x] Analysed every OGG: codec, duration, sample rate, bitrate, title tags, duplicates and vanilla counterparts.
- [x] Found `sounds.json` is invalid JSON and confirmed the `acerace.start`/`acerace.checkpoint` files are absent.
- [x] Mapped all 44 `playsound` commands to pad types and custom OGGs.
- [x] Decoded all nine worlds' region files for asset-relevant blocks: 11,152 Clouds 2 / 584 Yeti / 28,037 Space Race placements.
- [x] Scanned all 455 command blocks, 387 containers, playerdata and entity files for item NBT, names, lore and `CustomModelData`; found none of the last two.
- [x] Built `data/asset_map.csv` (108 rows) with own-map vs other-map usage separated.
- [x] Cross-checked A12's findings: formats 15/9/6 vs 34, Space Race wrapper root, and the missing `acerace.start`/`checkpoint` files; exact paths in `data/a12_crosscheck.md`.
- [x] Built `data/missing_refs.csv` (19 rows), `data/sounds.csv` (15 rows), `data/item_mapping.csv` (5 rows).
- [x] Wrote `data/compatibility.md` and `data/licensing.md`.
- [x] Produced and opened 13 evidence images (sheets, icons, PSD preview, waveforms, charts).
- [x] Confirmed no server-wide pack is active (`resource-pack=` empty, `require-resource-pack=false`).
- [x] No writes outside `resource_packs/`; shared tools untouched.

## Blocked / not attempted

| Item | Why | Who owns it |
|---|---|---|
| In-game screenshots of the packs | Packs are not enabled; enabling a server-wide pack is forbidden; no client session was launched | A1/A4 (map visuals), A11 (video) |
| Listening test of the Opus/custom SFX | No audio playback device in this environment; identification is by codec/path/metadata | A11 if needed |
| Server-side activation decision and `resource-pack=` config | A12 owns server import and resource-pack policy | A12 |
| Map route / command logic | Out of scope by plan; only conditions needed for asset mapping were read | A1/A2/A3/A4 |
| Author identity / licence negotiation | No metadata, no URL, no licence file in any pack | coordinator / owner |

## Open questions

1. **Provenance of the short SFX.** No title/artist metadata. Were they hand-made, purchased, or copied from a library? This determines whether a replacement pack can reuse them (currently: assume no).
2. **The 5:10 track.** Title says "Ace Race (From \"MC Championship Season 2\")". Is there a licence from MCC/Noxcrew, or is it lifted? Assume not licensed until proven.
3. **Why is `sounds.json` malformed?** The `{ "path" }` syntax looks like a hand edit or a bad generator. Was the pack ever tested after that edit?
4. **Intended mapping of `azalea.break` and `brewing_stand.brew`.** Both are custom, both are unused/used-once. Are they the lost `checkpoint`/`start` sounds?
5. **"Energy" item.** Renamed in lang but never distributed in these maps. Is it a server-plugin item (stamina) rather than a map item?
6. **Space Race "Restart Attempt".** Saved in a placeholder UUID's hotbar with no command/datapack trigger. Is it a hub item whose logic lives in a plugin?
7. **Purple glazed.** Fully modelled and animated but zero placed in Clouds 2 or Yeti. Was a fourth pad type cut?
8. **Space Race archive.** Was `resources.zip.zip` an upload mistake, or is the inner folder intentional for a launcher that expects it?
9. **1.21.1 HUD.** Should a replacement pack target the new `gui/sprites/**` atlas, or skip GUI theming because 5/8 current files are dead?

## Hand-off notes

- **A1 (Clouds 2):** use `data/asset_map.csv` and `data/map_asset_matrix.md` for the pack section; do not re-inventory the pack. The map's audio works through vanilla event replacement, not `acerace.*`. The 5:10 track is copyrighted.
- **A4 (Yeti):** Yeti is a subset of Clouds 2; do not treat the pack as a separate art set. It has no sounds. Purple/glowstone/Energy are unused.
- **A12 (server import):** `server.properties` has no active pack. Space Race's archive is not a valid pack as delivered. If a pack is enabled, the 1.21.1 GUI and Opus issues in `data/compatibility.md` apply.
- **A13 (synthesis):** the reusable design lessons are the pad colour grammar and the 7/16-frame animation recipes, not the files. Licensing forbids copying the art/music/lang dump.

## Confidence

- **H:** every file count/hash/size, PNG dimensions/alpha, JSON validity, OGG codec/duration, block counts, command strings, playerdata names, archive roots, server version/properties.
- **M:** client warning behaviour for old formats; exact Opus decode failure; whether malformed `sounds.json` is skipped while vanilla stays loaded; whether the two 5:10 encodes are the same master.
- **L:** author intent behind unused assets and the malformed index.
