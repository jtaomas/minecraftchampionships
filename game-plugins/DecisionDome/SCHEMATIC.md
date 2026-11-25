# Nocturne Orrery — Decision Dome schematic

**Theme:** a basalt-and-copper celestial observatory. The dome is a planetarium; the dial is an orrery floor; the reveal is a brass armillary sphere/planet. Night sky, teal and gold light, team jewel glass. Original build; MCC layout principles only.

**Site:** existing hub bowl at `mcc_lobby`. Dome centre `(0, −62)`, dial floor `y 64`, terrace/entrance `y 76`, inner radius `45`, drum outer `48.5`. Reuse the plaza, gate, carpet, drum, glass dome, tunnel and stands. Rebuild the pit.

---

## 1. Top-down — 8-game dial

```
                              N (−Z)
        ┌──────────────────────────────────────────────────────┐
        │   stands r 24–35, three tiers, y 67–79                │
        │   ╭──────────────────────────────────────────────╮   │
        │   │            concourse r 20–23 (y 64)          │   │
        │   │   ┌────┐   ┌────┐   ┌────┐   ┌────┐          │   │
        │   │   │P3  │   │P2  │   │P1  │   │P10 │          │   │
        │   │   └────┘   └────┘   └────┘   └────┘          │   │
        │   │      ╭──────────────────────────╮            │   │
        │   │  P4  │  W8    W1    W2    W3    │  P9        │   │
        │   │      │      ╲   │   ╱          │            │   │
        │   │      │  W7   ╭─┴─╮   W4        │            │   │
        │   │      │       │ ◎ │             │            │   │
        │   │      │  W6   ╰─┬─╯   W5        │            │   │
        │   │  P5  │      ╱   │   ╲          │  P8        │   │
        │   │      ╰──────────────────────────╯            │   │
        │   │   ┌────┐   ┌────┐   ┌────┐   ┌────┐          │   │
        │   │   │P6  │   │P7  │   │stair│  │P?? │          │   │
        │   │   └────┘   └────┘   └────┘   └────┘          │   │
        │   ╰──────────────────────┴───────────────────────╯   │
        │                    entrance tunnel (90°)              │
        └──────────────────────────────────────────────────────┘
                              S (+Z)
```

- **Dial:** r ≈ 12, floor y 64, one block below the pod floor. 8 wedges at 45°, boundaries at 22.5° + k·45°. Wedge 1 is centred on the 90° entrance axis so the first view through the doors is a game label, not a divider.
- **Podium:** the core ceremony podium (r ≤ 7.5) is the dial's centre island, not replaced. Wedges run r 7.5 → 12; votes only count at r > 7.5. The orrery reveal hangs above it.
- **Pod ring:** 10 pods at r 13–19 (6 deep), 36° spacing, **gap at 90°** for the stair. Pod centres: 18°, 54°, 72°, 108°, 126°, 162°, 198°, 234°, 270°, 306°. The two hero pods flank the stair at 72° and 108°.
- **Concourse:** r 20–23 at y 64, 3–4 wide; rail at r 20 with ten 3-wide gate openings and two stair openings.
- **Stair:** 6–7 wide on 90°, y 76 (r 41) → y 64 (r 24). Arrival view is over the whole dial.
- **Stands:** r 24–35, three tiers; keep the existing 120 stand spots; tier 1 at y 67 is 3 above the dial floor. Pod roofs at y ≤ 66.
- **Reveal:** a 6×6×6 brass armillary/planet suspended at y ≈ 76 over the centre, with four faces; secondary screens above the stair (90°) and opposite (270°). `revealSurface(0..3)`.
- **Labels:** one game totem at the outer end of each wedge, r ≈ 10, top y ≈ 70–71, readable from every pod above the 3-block divider.
- **Dividers:** black crenellated/comb dividers, 3 blocks high when raised, radial from r ≈ 2 to r ≈ 11.5; flush floor seam when down.
- **6-game variant:** same room/pods; dial becomes 6 wedges at 60°, boundaries at 30° + k·60°, wedge 1 on 90°. Do not shrink the pod ring. Recommended: one 8-wedge dial with two neutral/dark wedges for 6-game events.

## 2. Pod detail

```
        rear gate 3 wide          concourse r 19–23
        ┌──────┬──────┐
        │      │      │  team-colour back wall + name board
        │  ┌───┴───┐  │
        │  │ 4     │  │  floor 5×5, y 65, team carpet
        │  │players│  │  one step at the window
        │  └───┬───┘  │
        └──────┴──────┘
          throw window 3 × 2, sill y 66
             ↓ eggs arc 5–30 blocks
        ───────────────────── dial rim r 12
```

- Interior 5×5 minimum, 4-high ceiling; glass/copper frame every 3–5 blocks; 4 seats plus standing room.
- Window sill 1 above pod floor, 2 headroom; rear gate 3×3; pod gate/curse states supported by `setPodGate`/`setPodState`.
- Team colour on floor, ceiling, gate frame, window frame, name board; a large colour block on the concourse side.
- No chests/armour stands; inventory is eggs/power-ups only.

## 3. Elevation (x = 0)

```
 y
170  ▲ orrery topper / armillary
146  ╱ ╲  glass apex / oculus
126  ╱   ╲ marquee
107  │   │ truss ring; reveal sphere hangs below
102  │   │ drum rim / flags
 96  │   │ upper wall / LED band
 86  │   │ tunnel ceiling
 79  │   │ tier 3
 76  ═╡   ╞═ terrace / entrance
 72  │   │ tier 2
 67  │   │ tier 1
 65  │   │ pod floor
 64  ═╧═══╧═ dial floor (r ≤ 12)
```

Entrance 76 → dial 64 is 12 blocks; the 90° stair is the ceremonial link.

## 4. Nocturne Orrery palette

| Role | Blocks |
|---|---|
| Structure | polished basalt, deepslate tiles, blackstone, smooth basalt, tuff |
| Trim / metal | oxidized copper, weathered copper, cut copper stairs, gold block, brass-like waxed copper |
| Light stone | calcite, smooth quartz, bone block, cream concrete |
| Glass | tinted glass, light-blue/cyan stained glass, teal panes; team jewel glass in pods |
| Accent | gold block, ochre froglight, teal froglight, sea lantern, end rod |
| Sky | black/blue concrete, star blocks, a banded planet/planetarium dome above the glass |
| Team pods | ten jewel glass colours: red, orange, yellow, lime, green, cyan, aqua, blue, purple, pink |
| Danger/blank | black concrete, charcoal, red/orange hazard only on the reveal mechanism |

Strict hierarchy: 60% near-black/basalt, 25% cream/calcite, 15% copper/gold + team jewel glass. No flat single-material run > 6 blocks on non-gameplay surfaces.

## 5. Exterior

- Basalt drum with copper ribs and a cream cornice; glazed clerestory band so the lit interior reads from outside.
- Ribbed glass dome (12 meridians, 4 latitude rings) with an oculus; brass armillary topper (three rings + a small planet) instead of a generic trophy.
- Facade: a black marquee with cream block lettering **BLOCK SHOWDOWN**, a 5–7 block digit countdown (12–20 wide), and a copper orrery emblem.
- Entrance: 13-wide portal, 8–10 wide doors, carpeted approach from the plaza; optional side elevator doors.
- Night lighting: warm cream uplights, teal rim, searchlight beams, planet/star projection inside the dome.

## 6. Build phases

1. Clear the pit (pads, old accents) and keep the plaza/drum/tunnel/dome/truss/stands.
2. Lay the dial floor r 12, central podium island, 8 wedges + dividers + label anchors.
3. Build 10 pod shells at r 13–19, gates, windows, team glass, name boards.
4. Add the concourse r 20–23, rail, 90° stair, stand rail openings.
5. Add the orrery reveal, interior clock, planetarium sky, lighting.
6. Add the exterior topper/clerestory/marquee and facade countdown.
7. Render top + interior + exterior; validate dimensions; iterate twice.

## 7. Validation

- [ ] Footprint ≤ 100 blocks; fits the existing hub bowl.
- [ ] 10 pods, 4 players each; pod centres 36° apart with 90° gap.
- [ ] 8 wedges at 45°, 6 at 60°; wedge 1 on 90°.
- [ ] Dial r 12, podium r 7.5, divider 3 high when raised.
- [ ] Stair 6–7 wide from y 76 to y 64 on axis 90°.
- [ ] Stands r 24–35, 120 spots; sightline from every pod to the dial and reveal.
- [ ] Pod roofs ≤ 66; no pod blocks the stair/rail.
- [ ] Block budget 275k–485k; first pass target lower bound.
- [ ] Top and interior renders; no flat/empty bowl, no confetti palette, no fish-scale bubble.
