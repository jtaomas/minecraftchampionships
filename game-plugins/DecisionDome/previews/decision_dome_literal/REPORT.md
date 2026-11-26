# Decision Dome literal transplant — report

**Result: the downloaded build is copied block-for-block. 20,054 / 20,054 non-air source blocks and 45 / 45 tile entities verify at the translated coordinates. No source block was removed, rotated, recoloured or edited.**

## 1. Source and target

- Source world: `C:\Users\LeonS\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\review\mcc_decisiondome`
  (1.21.1 Anvil, the review copy of the upgraded live world; its volume/count match the brief exactly: 85×57×66, 20,054 non-air blocks, 45 tile entities).
- Target world: a full copy of the live `mcc_lobby` world, made at
  `C:\Users\LeonS\AppData\Local\Temp\opencode\dome_literal\mcc_lobby`.
- Volume read: `x -45..39, y 26..82, z -21..44` (85×57×66).
- Rigid translation: **x +3, y +8, z −78**.
  - source centre `(-3, 16)` → target centre `(0, -62)`;
  - source terrace `y68` → target `y76` (same block layer as the Nocturne terrace);
  - source dial `y58` → target `y66`;
  - source crown `y69..82` → target `y77..90`.
- Target volume after translation: `x -42..42, y 34..90, z -99..-34`.

The source is a display sculpture, not a playable arena: 2 command blocks, 2 beehives, 24 banners, 17 skulls (45 tile entities) and 140 barriers. All are copied.

## 2. What was removed / what remains

- Cleared in the copy only: the Nocturne v5 interior inside **r ≤ 45.0, y30..102** — pods, cage towers, dial, crown, stands, concourse, inner wall, aisles, old stage. This is the volume the downloaded build needs.
- Kept untouched: the Nocturne drum shell (`r45.5..48.5`), its inner decorated face, the glass dome, the facade/marquee/clock, the outer terrace and approach, the grand stair's exterior context, the island.
- No downloaded block is inside the cleared zone: the build is re-placed from the source after the clear. The target chunks are rebuilt from their own palettes; only the interior cells are replaced.

## 3. New blocks (all outside the downloaded build)

1. **Outer gallery**: `r 42.0..45.0`, `y76` (walk `y77`), `white_concrete`, strictly outside the build's outer r42 pavilions. This is the walkable outer rim next to the Nocturne shell.
2. **Gallery rails**: `y77` at `r 41.6..42.4` and `r 44.6..45.4`, `calcite`, with a 7.6-wide gap at the entry bridge. The inner rail separates the gallery from the void around the sculpture; the outer rail runs against the shell.
3. **Entry bridge** on the downloaded **door side**: the source's real doors are the iron doors at source `(19-20, 69-70, 37-38)`, translated to `(22-23, 77-78, -41..-40)` — radius 31 at **45° (southeast)**. A 5.2-wide `white_concrete` bridge at `y76` runs from `r45` (Nocturne drum) to `r29`, ending just outside the build's own r28 railing, with `calcite` side rails at `y77`. The bridge crosses only a 5-wide strip of the outer facade; it does not cover the build.
4. **Nocturne entrance cut on the same 45° side**: a 5-wide, 4-high opening through the drum at `r45.15..49.0`, `y77..81`, with `cut_copper` jambs/lintel on both drum faces and a `white_concrete` threshold at `y76`. This is the reworked entrance; the old south facade remains as a blind porch.
5. **Old south tunnel**: the inner mouth at the drum face (`r44.8..46.2`, `z -18..-14`, `y76..86`) is sealed with `polished_blackstone` and a `cut_copper` lintel at `y87`, so the south facade cannot drop a player into the void. The exterior facade/approach is untouched.
6. **Hub marker**: `mcm_hub_built.txt` set to `v5 built ...` so the current `HubGenerator.VERSION = 5` does not clear and regenerate the merged world. The live copy said `v2`; it was replaced in the copy only.

New block count in the copy: **1,877**. None of them occupies a coordinate that holds a source block.

## 4. Verification (block level, read back from the written world)

| Check | Result |
|---|---|
| Source non-air blocks in the volume | **20,054** |
| Present at `x+3, y+8, z-78` with the same block state | **20,054 / 20,054** |
| Missing source blocks | **0** |
| Wrong block state | **0** |
| Source tile entities | **45** |
| Present with identical NBT (all fields except translated `x/y/z`) | **45 / 45** |
| Missing tile entities | **0** |
| Wrong tile-entity data | **0** |
| Old v5 interior blocks left in the cleared cylinder (excluding the new gallery/bridge/rails/door) | **0** |
| Region files re-parsed after writing | 2 files, 1,024 chunks each, all parse |

Verification method: the merge reads the source `block_states` palette entry (Name + exact `Properties`) and writes that same NBT entry into the target section palette; the read-back decodes every target section and compares canonical `Name[Properties]` at every translated coordinate. Tile entities are compared as NBT compounds with `x/y/z` excluded (they are translated by the same offset).

Scripts (not committed):
`C:\Users\LeonS\AppData\Local\Temp\opencode\dome_literal\merge_world.py`, `verify_merge.py`, `dome_lib.py`, `source_model.py`.

## 5. Entry / pit safety

- The player enters through the new **45° southeast** drum door, steps onto the `y76` gallery/bridge, and lands on the outer rim — never in the pit.
- The bridge ends at `r29`, outside the downloaded build's own **r28** railing. The downloaded build's **inner barrier at r24-25** is untouched and verified present (140 barrier blocks).
- The gallery is outside the build's r42 outer pavilions; the only new floor inside that radius is the 5-wide entry bridge.
- Rails at `r42` and `r45` plus the bridge side rails contain the walkway. The void between the sculpture and the gallery is not walkable.
- The source's closed iron doors remain exactly as downloaded; they are not powered by the merge. The entry bridge lands at the outer rim beside them. Opening them (button/lever/plugin) is a separate gameplay action and would not alter any downloaded block.

## 6. Deployable world

`C:\Users\LeonS\AppData\Local\Temp\opencode\dome_literal\mcc_lobby` (18.5 MB)

- Full copy of `mcc_lobby`: `level.dat`, `region`, `entities`, `poi`, `data`, `paper-world.yml`.
- `session.lock` removed; `mcm_hub_built.txt` = `v5 built 2026-11-25 ...`.
- No live-world write, no 25580 restart, no commit.

## 7. Renders

`game-plugins/DecisionDome/previews/decision_dome_literal/`:

| File | View |
|---|---|
| `top.png` | top-down merged bowl (y30–115): downloaded pit/dial/towers, gallery ring, bridge |
| `rim_entry.png` | eye-level on the 45° bridge: the landing on the outer rim, facing the downloaded gatehouse/doors, dome above |
| `entrance_outside.png` | optional: eye-level outside the new 45° drum door, looking through it at the downloaded build |
| `interior_from_rim.png` | raised view from the outer rim at 45°, over the downloaded towers to the crown and surrounding Nocturne drum |
| `interior_iso.png` | isometric of the downloaded build inside the Nocturne shell |
| `exterior.png` | full venue exterior; drum/dome/facade/terrace/approach intact |
| `side_by_side.png` | downloaded `iso_interior_labelled.png` beside the transplanted build |

The perspective views were rendered locally from the merged MCMDUMP with a raycast camera (`perspective_render.py`); no server was involved.

## 8. Risks / integration notes

1. **Hub generator marker is critical.** The copy is marked `v5` to match `HubGenerator.VERSION = 5` in this working tree. If the deployed jar is a different generator version, `HubManager.enable()` will clear and rebuild the hub, destroying the merge. Verify the jar or pin the marker to the deployed generator version.
2. **Plugin layout still describes the old v5 interior.** `HubLayout`'s dial/pod anchors are unchanged; the world no longer contains those pods. `HubManager`/`HubDialView` may place runtime blocks/text at the old coordinates. The coordinator should disable the v5 interior runtime or update the anchors for the downloaded venue.
3. **Iron doors are closed.** The source doors are `open=false, powered=false`; altering them is forbidden by the literal-copy rule. The entry lands on the rim beside them. Add a button/lever or open via plugin if passage through them is wanted.
4. **South facade is a blind porch.** The old tunnel's inner mouth is sealed so the facade cannot drop anyone into the void; the facade and approach remain.
5. **Gallery voids.** The gallery is a 3-block-wide ring outside r42; the space between it and the sculpture is not floor. The rails protect it, but it is intentionally not a full disc (a full disc would cover the downloaded outer facade).
6. **Bridge strip.** The 5-wide entry bridge crosses the top of a strip of the downloaded outer facade; no source block was removed or changed, but the strip is covered at `y76`.
7. **No deploy, no restart, no commit.** The live 25580 server and the live `mcc_lobby` were only read.
