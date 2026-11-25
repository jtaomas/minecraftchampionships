# Palette and lighting — Decision Dome Recreation

Counts are from the block dump (`data/blocks.csv`, 20,054 blocks, 85 base types, 186 block states). Shares are of all non-air blocks. Every count is **H**; the "role" statements are **H** where the block is in one structure and **M** where a type is reused.

## 1. Hierarchy

| Group | Blocks | Share | Role |
|---|---:|---:|---|
| Concrete (all colours) | 14,924 | 74.4% | structure, terrace, 10 team pod towers, gate frames |
| Terracotta (team + light gray) | 1,894 | 9.4% | shadow bands on the pod towers; two drum stripes; crown band |
| Glass (stained glass, panes) | 1,844 | 9.2% | pod window bands, annex/platform glazing, gatehouse windows |
| Quartz (slabs/stairs) | 442 | 2.2% | dial plates, cross/truss, tunnel floors, annex roof |
| Gold block | 144 | 0.7% | crown's 8 pillars |
| Wool / carpet | 155 | 0.8% | pod accents, crown top ring, annex floor |
| Metal (chain) | 60 | 0.3% | gatehouse hanging chains + crown suspension |
| Barrier | 140 | 0.7% | invisible inner rail, y71 |
| Sea lantern | 18 | 0.09% | the only light-emitting block |
| Banners / heads / beehives | 43 | 0.2% | NW annex trophies/apiary |
| **Copper (any family)** | **0** | **0.0%** | **none — no copper, cut copper, oxidized/weathered/waxed copper** |

There is no wood, no stone brick, no deepslate, no blackstone, no calcite, no basalt and no custom resource pack in the world. The palette is a four-material model: **concrete + terracotta + stained glass + quartz**, with gold and a few wool/carpet accents. It is closer to a team-colour toy than to the Nocturne Orrery's basalt/copper/cream hierarchy.

## 2. The ten team colours

| Team colour | Concrete | Terracotta | Glass (panes + solid) | Approx. total team-family |
|---|---:|---:|---:|---:|
| Red | 563 | 138 | 96 | 797 |
| Orange | 530 | 132 | 96 | 758 |
| Yellow | 546 | 132 | 96 | 774 |
| Lime | 502 | 122 | 96 | 720 |
| Green | 502 | 132 | 96 | 730 |
| Cyan | 486 | 148 | 96 | 730 |
| Light blue | 535 | 146 | 96 | 797 |
| Blue | 553 | 130 | 94 | 777 |
| Purple | 502 | 122 | 96 | 730 |
| Pink | 502 | 132 | 96 | 730 |

The 10 team concrete colours are **26.0%** of the build; including their terracotta, glass and wool bands, the 10 pod families are **37.7%** (7,553 blocks). Red/blue/yellow/light-blue/orange carry extra blocks because they also trim the gatehouses, the north carpet or the crown; the pure pod bodies are ~486–502 concrete each. Each pod is built as a banded tower: **team concrete → team terracotta → white/team glass → team concrete top**. The terracotta bands are the only "shadow" in the palette; there is no gradient, no noise texture and no weathering except the terracotta itself.

## 3. Gray drum and white rim

- `gray_concrete` **8,953 (44.6%)** — the single dominant block: drum shell (r26, y27–68), flat base disk (y26), pit wall, terrace inner ring, crown bands. The drum has **two light-gray terracotta stripes** at y33 and y52 (160 blocks each); a third light-gray band at y72 is the neutral band on all 10 pod crowns (240 total); the crown itself is gold at y72.
- `white_concrete` **589 (2.9%)** — terrace outer rim (y68), north platform, gatehouse frames, NW annex floor. The white rim is the strongest value contrast: 2 wide (r27–28) against the dark pit.
- `black_concrete` **161 (0.8%)** — only the gatehouse frames/posts (y61–72); it is not used in the dial/dividers.
- `polished_diorite` + stairs **134** and `polished_andesite` slabs/stairs **76** — the gatehouse pilasters and the outer lip at y66; they read as light gray trim, not as a separate stone family.

## 4. Glass

- **594 white stained glass** — the pod window band at y58–60, 198 blocks per layer × 3 layers, all 10 pods.
- **718 coloured glass panes** — the pod crown window band at y65/67/68, 24 panes per pod per layer (blue has 70, not 72).
- **240 coloured stained glass** — the pod band at y69, 24 per pod.
- **254 white glass panes** — north platform and NW annex glazing.
- **38 light-gray glass panes** — gatehouse window strips.
- No tinted glass, no plain glass, no beacon glass, no coloured glass outside the pod bands. The glass is **not** a dome; it is a banding material. The build has no roof over the pit.

## 5. Banners, heads and the NW annex

- **24 banners**: 16 `white_wall_banner` with black/white geometric patterns, 8 columns × 2 rows on the annex's diagonal wall (y74–75); 4 coloured banners at x=-29 y70 (yellow, green, orange, cyan) and 4 orange at x=-29 y72. The patterned set is monochrome (black patterns on white), not team-colour. H.
- **17 player heads**: 9 chicken heads, 5 zombie/green player heads, 3 pale/white heads (texture uncertain). Mounted 3 high on two walls of the annex (x -23..-19 at z=-14; x -26..-28 at z=-11..-13). H for count/positions; M for the species labels.
- **2 beehives**, 4 oak leaves, 62 light-gray carpet, 9 gray carpet, 8 gray wool, 8 netherite blocks, 2 armor stands (gold and green leather chestplates). The annex reads as an **apiary/trophy lounge**, not a team pod. H/M.
- No signs, no chests, no item frames. H.

## 6. Lighting

- **18 sea lanterns only**: 12 on the NW annex roof at y73, 2 on the north dais at y70, 4 at the crown cross tips y73. No glowstone, lantern, torch, froglight, campfire, lava, or light block. H.
- The world is locked at **noon** (`DayTime 6000`, `doDaylightCycle=false`); the build is lit by daylight from above. The **pit and the dial are in shadow**, and the crown blocks the centre from above. There is no artificial light in the pit. H.
- Redstone blocks (8) at the crown top are decorative "jewels", not light. H.
- The gatehouses have no lights; the chains/trapdoors are structural detail. H.

## 7. What the palette does well / badly

**Does well (M):** the 10 team colours are unambiguous at distance; the gray drum + white rim + gold crown is a clean 3-value composition; terracotta bands stop the towers from looking like flat plastic; no confetti — only 10 team colours, all in one ring.

**Does badly (M):** the gray drum is 44.6% of the build and visually flat for 42 blocks of height; the pit is almost black and unlit; the dial's beige/gray is low-contrast and hidden; copper/wood/stone variety is absent, so close-up surfaces are repetitive; the crown's gold is the only warm accent and it is small relative to the drum.

## 8. One-line palette recipe (if re-used)

`gray concrete drum + white concrete rim + 10 team concrete towers with team terracotta shadow bands + white/team glass window bands + smooth quartz dial + gold crown + 18 sea lanterns + fixed noon`. No copper, no wood, no dome.
