# renders/ — source dumps and render settings

The eight `.mcmdump.gz` files are **A6’s** existing 1.17 dumps, copied read-only into this folder. They already carry an 8-block margin around the arena bounds. No new region scan was run.

The 16 PNGs here are A6c renders made with the shared `tools/render_preview.py` (read-only). The same 16 files are in `../frames/`; the two copies are byte-identical.

| Arena | Dump | Top scale | Iso scale | Top output | Iso output |
|---|---|---|---:|---:|---|
| `MG-HUB-01` | `lobby.mcmdump.gz` | 18 | 10 | `hub_top.png` | `hub_iso.png` |
| `MG-SC-01` | `sc.mcmdump.gz` | 20 | 10 | `sc_top.png` | `sc_iso.png` |
| `MG-SR-HUB-01` | `woolhub.mcmdump.gz` | 44 | 18 | `srhub_top.png` | `srhub_iso.png` |
| `MG-SR-RED-01` | `woolred.mcmdump.gz` | 20 | 10 | `srred_top.png` | `srred_iso.png` |
| `MG-SR-ORG-01` | `woolorg.mcmdump.gz` | 20 | 10 | `srorg_top.png` | `srorg_iso.png` |
| `MG-CMD-01` | `cmd.mcmdump.gz` | 20 | 12 | `cmd_top.png` | `cmd_iso.png` |
| `MG-REB-01` | `rebecca.mcmdump.gz` | 56 | 22 | `reb1_top.png` | `reb1_iso.png` |
| `MG-REB-02` | `sky.mcmdump.gz` | 12 | 8 | `reb2_top.png` | `reb2_iso.png` |

Command shape:

```
python tools/render_preview.py renders/<dump>.mcmdump.gz renders/<out>.png --top --scale <N>
python tools/render_preview.py renders/<dump>.mcmdump.gz renders/<out>.png --scale <N>
```

The renderer hides `barrier` and `light`; the images therefore show visible geometry, not the full copy envelope. See `../REFERENCE_IMAGES.md` for what each image proves.
