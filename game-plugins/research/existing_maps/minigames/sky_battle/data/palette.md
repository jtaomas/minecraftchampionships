# Palette and lighting — MG-SB-01 Sky Battle

Source: `data/blocks_mg_sb_01.csv` (47,874 non-air blocks, 59 block types); machine table `data/blocks.csv`; per-region matrix `data/blocks_by_region.csv`. **H** for every count.

## 1. Whole-arena palette

| Group | Blocks | Share | What it does |
|---|---:|---:|---|
| Barrier shell | 34,751 | 72.59% | Invisible 105x81x105 box: walls x1948/2052, z-52/52, roof y56, sky-lobby cage |
| Neutral stone / quartz | 6,330 | 13.22% | White concrete, stone bricks, smooth quartz, quartz stairs, cobblestone/stone-brick walls |
| Team concrete | 3,004 | 6.27% | Red 984, yellow 725, green 725, blue 570 — gift boxes + canopy quadrants |
| Team terracotta | 1,437 | 3.00% | 359/359/360/359 — canopy underside / seat bands |
| Balloon wool | 892 | 1.86% | Orange/cyan/lime/purple, 223 each |
| Wood / containers | 662 | 1.38% | Oak fence/leaves/trapdoors, spruce mast/fence, crafting tables, barrels, chests |
| Team glazed terracotta | 313 | 0.65% | 78/79/78/78 — gift-box lid pattern |
| Lobby gray wool | 287 | 0.60% | Lantern body |
| Lobby gold | 68 | 0.14% | Gold block 48 + raw gold 20 |
| Chain | 61 | 0.13% | Central drop, balloon hangers, mid stem |
| Utility / lighting | 28 | 0.06% | 2 command blocks, 1 repeating, 10 signs, 5 buttons, 4 light, 1 lectern, 5 campfires |
| Lobby stained glass | 24 | 0.05% | 6 each of red/yellow/blue/green |
| Iron knots | 8 | 0.02% | 2 per gift-box ribbon apex |
| Join pads | 4 | 0.01% | 1 concrete powder per team |
| Other | 5 | 0.01% | 1 green wool, 1 lectern etc. |

Visible build = 47,874 - 34,751 = **13,123 blocks**. Any palette analysis that includes barrier is dominated by an invisible block. **H**.

## 2. Per-structure palette

- **Ring (7,344 blocks):** white concrete 2,197; stone bricks 1,231; team concrete ~460 each (red 460, yellow 465, green 465, blue 461); team terracotta 359/359/360/359; oak fence 240; oak leaves 138; smooth quartz 256 + stairs 95; cobblestone wall 164. Read: a **white-stone circular arcade** with four saturated team roofs; wood/leaf colonnade outside. **H**.
- **Red gift box (1,017):** white concrete 587, red concrete 288, red glazed 78, smooth quartz 60, 2 iron, 1 white shulker, 1 crafting table. **H**.
- **Green gift box (1,018):** white 587, green concrete 260, green glazed 79, smooth quartz 60, red concrete 28 (shared ribbon), 2 iron. **H**.
- **Blue gift box (1,018):** white 587, red concrete 180 (ribbon), blue concrete 109, blue glazed 78, smooth quartz 60, 2 iron. **H**.
- **Yellow gift box (1,017):** white 587, yellow concrete 260, yellow glazed 78, smooth quartz 60, red 28, 2 iron. **H**.
- **Balloon (290 each):** coloured wool 223, oak trapdoor 20, spruce fence 16, chain 10, stripped spruce log 9, spruce trapdoor 4, fence gate 4, barrel 1, chest 1, slab 1, campfire 1. Read: a **wool envelope on a timber mast/crate**, with a chest in a trapdoor crate and a barrel hanging 12 blocks below. **H**.
- **Sky lobby (464 visible):** gray wool 287, gold 48, raw gold 20, oak trapdoor 20, spruce fence 16, 10 signs, 4 light, 4 stained-glass colours x6, 5 buttons, 2 command blocks, 1 lectern, 1 campfire. Read: a **gray/gold hanging lantern** with four team-coloured bays. **H**.
- **Mid (43 at y20-30 + 9 chain):** stone bricks 11, stone-brick stairs 8, 5 each of red/yellow/blue/green concrete, 2 white concrete, barrel, crafting table. Read: a **tiny team-coloured compass plus** under a barrel/crafting-table tower. **H**.

## 3. Palette logic

1. **Neutral shell + one saturated colour per team.** White concrete and stone bricks are the shared surfaces; each team owns one concrete + one terracotta + one glazed terracotta. The team colour is visible from above, from the rim, and from the opposite side. **H** (counts, render).
2. **Quadrant symmetry.** Each gift box and each canopy quadrant uses the same 3-material recipe (concrete body, terracotta underside, glazed lid). Only the dye changes. This makes the arena readable and cheap to rebuild. **H**.
3. **Material honesty.** The gift boxes are white concrete + team concrete with quartz ribbon; the balloons are wool; the gallery is stone/wood. There are no random accent blocks. **H**.
4. **No custom resource pack.** Every id is vanilla 1.17.1. **H**.

## 4. Lighting

- **Fixtures:** 4 `minecraft:light` blocks at y74 in the sky lobby; 4 campfires at y29 on the balloon crates; 1 campfire at y63 on the lobby stem. No torches, lanterns, glowstone or sea lanterns in the arena. **H** (block list).
- **Daylight:** `doDaylightCycle=false`; the arena is open to the sky through the barrier roof (barrier is intended to be non-occluding, but this needs an in-game check). **H** game rule, **M** light pass-through.
- **Under-canopy:** the gallery floor at y25 is covered by a roof at y27-37; at the diagonal dips the roof underside is y27, leaving 1-2 blocks of headroom. Expect **dark pockets** under the low canopy unless the barrier roof passes skylight. **M** (geometry + no lamps).
- **Render note:** the preview PNGs use a flat sky background and no in-game light calculation; do not read their brightness as evidence of in-game lighting. **H** (render tool).

## 5. Palette takeaway for a reimplementation

Keep the **white/stone neutral + 4 team triples (concrete/terracotta/glazed)** rule. It reads at distance, survives a resource pack swap, and costs 7 materials per team. The **wool balloons** are the only large non-team colour blocks; they mark neutral loot and are meant to be broken. **M** (design recommendation, not executed).

