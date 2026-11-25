# A12 cross-check — pack formats and archive roots

Verified against the copied archives in `packs/*/resources.zip` (byte-identical to the staged sources; source names are noted below). Format, root and file-existence rows are **H**; statements about how a client treats a repacked/wrapped pack are **M**.

| Pack | `pack_format` | Required for 1.21.1 | Root entry | Root `pack.mcmeta`? | Inner `.zip`? | Verdict |
|---|---|---|---|---|---|---|
| Clouds 2 | 15 | 34 | `assets` | yes | no | valid pack root |
| Yeti Set Go | 9 | 34 | `assets` | yes | no | valid pack root |
| Space Race | 6 | 34 | `Space Race Resource Pack` | no | no | not a pack as shipped |

## Exact archive roots

### Clouds 2

- Archive copy: `C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs\packs\clouds2\resources.zip`
- Source filename: `resources.zip`
- Zip root entries: `assets`, `pack.mcmeta`, `pack.png`
- `pack.mcmeta` at zip root: **yes**
- Inner zip/jar files: **none**
- Shallowest `pack.mcmeta`: `pack.mcmeta`
- `pack_format`: **15**
- `description`: '§c Ace Race Resource Pack'

### Yeti Set Go

- Archive copy: `C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs\packs\yeti_set_go\resources.zip`
- Source filename: `resources.zip`
- Zip root entries: `assets`, `pack.mcmeta`, `pack.png`
- `pack.mcmeta` at zip root: **yes**
- Inner zip/jar files: **none**
- Shallowest `pack.mcmeta`: `pack.mcmeta`
- `pack_format`: **9**
- `description`: '§c Ace Race Animation Example'

### Space Race

- Archive copy: `C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs\packs\space_race\resources.zip`
- Source filename: `resources.zip` (source was `resources.zip.zip`)
- Zip root entries: `Space Race Resource Pack`
- `pack.mcmeta` at zip root: **no**
- Inner zip/jar files: **none**
- Shallowest `pack.mcmeta`: `Space Race Resource Pack/pack.mcmeta`
- `pack_format`: **6**
- `description`: 'Space Race Textures.'

**Space Race is a wrapper, not a nested zip.** Its zip root contains one directory, `Space Race Resource Pack/`, and every asset lives below that directory. There is no inner `.zip`; the double `.zip.zip` in the source filename is a naming error plus the wrapper. Minecraft looks for `pack.mcmeta` at the zip root, so this archive is not a pack as shipped. It also is not named `resources.zip`, which is the filename a world folder expects.

## A12 statement check

| A12 statement | Result | Evidence |
|---|---|---|
| Clouds 2 is format 15 | **confirmed** | `packs/clouds2/extracted/pack.mcmeta` |
| Yeti is format 9 | **confirmed** | `packs/yeti_set_go/extracted/pack.mcmeta` |
| 1.21.1 requires 34 | **confirmed** | 1.21.1 client `version.json`: `resource: 34` |
| Space Race is format 6 | **confirmed** | `packs/space_race/extracted/Space Race Resource Pack/pack.mcmeta` |
| Space Race is a 'nested/double zip' | **partly**: it is a wrapper folder inside a `.zip`, not a zip-inside-zip; filename is `resources.zip.zip` | zip entry list: one root folder, no inner `.zip` |
| Space Race is invalid as shipped | **confirmed** | no root `pack.mcmeta`; not named `resources.zip` |
| Clouds 2 references missing `acerace/start.ogg` and `acerace/checkpoint.ogg` | **confirmed, plus worse**: `sounds.json` is invalid JSON, so all six `acerace.*` events fail to register | `json.load` error line 5; file listing |

## Format requirement table

| Pack | Format | Correct version range | Map version | On 1.21.1 (format 34) |
|---|---|---|---|---|
| Clouds 2 | 15 | 1.20–1.20.1 | 1.20.1 (matches) | loads with older-version warning; content paths valid |
| Yeti Set Go | 9 | 1.19–1.19.2 | 1.19.2 (matches) | loads with older-version warning |
| Space Race | 6 | 1.16.2–1.16.5 | 1.18.1 (already old: 1.18 uses 8; M) | archive invalid before format even matters; if repacked, loads with warning |

No pack is enabled. `server.properties` has `resource-pack=` empty and `require-resource-pack=false`; no imported server world contains `resources.zip`. Nothing was ported or enabled for this check.
