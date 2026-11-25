# data/ — what each file is

Authoritative deliverables are the CSVs. The JSON/plain files are the raw extraction
they were generated from, kept so every CSV row can be re-derived without re-reading
the 86 MB region set.

| File | Rows / size | What it is |
|---|---|---|
| `command_blocks.csv` | 322 | **Authoritative** command table: x/y/z, block variant, auto, conditional, powered, bank, exact command, trigger, effect, reset, multiplayer caveat, 1.21.1 risk. |
| `command_failures.csv` | 31 | The subset that fails on 1.21.1 (30 item-NBT boots + 1 malformed setblock), with native-1.20.1 status. |
| `signs.csv` | 285 | **Authoritative** sign table: x/y/z, block, front/back 4 lines, colour, glowing, waxed, click event. |
| `containers.csv` | 225 | **Authoritative** container table: every shulker, variant, `Items` tag presence, contents, role. |
| `blocks.csv` | 171 | **Authoritative** block table: type, count, share, use. |
| `checkpoints.csv` | 111 | Curated ordered route points (start/finish, checkpoints, pads, elytra, gates). Not every raw pad component. |
| `route_legs.csv` | 14 | The route as legs L0–L13 with from/to, length, theme, mechanics. |
| `pack_mapping.csv` | 33 | Map element → pack asset → notes. Pack inventory is A10's (`../resource_packs/data/`). |
| `entities.csv` | 1 | The single marker entity. |
| `command_blocks.json` | 322 | Raw extraction incl. `LastExecution`, `SuccessCount`, `TrackOutput`, `UpdateLastExecution`, region/chunk. |
| `block_entities.json` | 832 | All raw block entities (322 command blocks, 285 signs, 225 shulkers) as parsed from the chunk NBT. |
| `signs.json` | 285 | Raw sign components (JSON text strings before decoding). |
| `pads.json` | 396 | Connected components of yellow/red/magenta/pink/purple/green glazed, lime/orange shulkers, spruce/andesite elytra, calcite. |
| `block_counts.json` | 171 | Raw `[name, count]` pairs from the numpy palette scan. |
| `scan_summary.json` | 1 | Chunk count and chunk-aligned bounds. |
| `entities.json` | 1 | Raw entity record (type, Pos, tags). |
| `clouds2_full.mcmdump.gz` | 150,414 blocks | Full block dump in `MCMDUMP 1` format, origin x −258 / y −64 / z −316. Input to `tools/render_preview.py` and my crop/section scripts. |
| `cave_air.txt.gz` | 309 | Every `minecraft:cave_air` coordinate (the elytra-clear volumes). |
| `cb_plain.txt` | 322 | Human-readable command dump: `x y z a=? c=? command`. |
| `signs_plain.txt` | 285 | Human-readable sign dump: `x y z colour | text`. |
| `crops/*.mcmdump.gz` | 13 | Cropped dumps used for the section renders. |

## Reproduce a CSV

```
python scripts/scan_be.py          # block_entities.json
python scripts/dump_cb_signs.py    # command_blocks.json + signs.json
python scripts/scan_blocks.py      # block_counts.json
python scripts/dump_mcmdump.py     # clouds2_full.mcmdump.gz
python scripts/join_cb_type.py     # adds the block variant to command_blocks.json
python scripts/make_cb_csv.py      # command_blocks.csv
python scripts/make_data_csvs.py   # signs / containers / entities / failures / pack / blocks
python scripts/make_route_csvs.py  # checkpoints / route_legs
python scripts/fill_sign_blocks.py # adds sign block variants
python scripts/find_pads.py        # pads.json (render overlay, not the CSVs)
python scripts/qa.py               # counts and integrity checks
```
