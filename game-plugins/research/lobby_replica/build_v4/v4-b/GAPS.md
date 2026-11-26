# V4-B — GAPS

Two kinds of entry: **(A)** something the references do not show, so I did not invent a
detail and instead chose a documented, plain default; **(B)** a deliberate departure from
the brief or a reference where the two disagree. Nothing here is a silent guess.

---

## A. Not visible in any reference — flagged, not invented

| # | Element | What the references show | What I did |
|---|---|---|---|
| A1 | Mountain, north and west flanks | Never visible: every aerial is from the south or south-east. | Built as a symmetric taper (west and north edges fall to y = 120 over 8–9 blocks). The shape is a plausible ridge, **not** measured. |
| A2 | Mountain, depth (z profile) | Only the silhouette is visible, never a section. | `Z_PROF` is a hand-fit curve: full height z -16..+4, dropping to 0.66 at the sign plane. Not measured. |
| A3 | Ridge azimuth | The S1 aerial reads the ridge as **NW–SE** (diagonal); `MCCHubSeason1` and `MCCNewHub` both suggest a long ridge receding from the camera. | Built **axis-aligned north–south**. A diagonal ridge is fine for terrain, but the marquee on it would then be 45°, and a 3×5 block font aliases into bars on a 45° plane (same finding as `build/lbr-4` §2). Axis-aligned keeps the sign crisp. |
| A4 | Biosphere — interior | `MCCHubSeason1` shows something red/white *inside* the glass with a rainbow band near the floor; `MCCHub-13` shows red tiered seating. Details are unreadable through the glass. | Built as a hall: red carpet, 6-arc rainbow floor inlay, 8 quartz columns, white/gold dais, gold championship coin. The coin is the object visible in `TheChamps.png`; its placement at the centre of the hall is my reading. |
| A5 | Biosphere — north and west exterior | Never shown. | Plain drum: quartz field, white pilasters every 30°, gold band at the top. The pilaster spacing is inferred from the S1 rib spacing. |
| A6 | Biosphere — roof/apex | `MCCHub-13` has a giant red bow on the apex; `MCCHubSeason1` and `MCCNewHub` have nothing (or an unclear finial). The bow belongs to the Christmas build only. | **No bow, no finial.** A quartz apex cap. If the owner wants the festive version, the bow is a separate ~8×8 detail that can be added above y 150 without touching anything else. |
| A7 | Snow globe — far side and base | Seen from one angle only (`MCCHub-13`). The base's back and the globe's north face are never shown. | Base is a full ring, symmetric; glass is a full sphere. Both are safe symmetrically. |
| A8 | Snow globe — sphere shell material | Reads as saturated cyan-blue glass. Cannot tell stained glass from tinted glass from a blue-lit interior. | `light_blue_stained_glass` with `blue_stained_glass` accents low down (where the reference is darkest). |
| A9 | Rainbow bridge — underside and deck surface | Every reference view is from the side or above the deck's edge; the deck surface and the underside are never seen. | Deck = `red_concrete` with a snow crust on the two edge rows (the references show snow lying on the top edge). Underside = red fascia with rib courses every 6 blocks. |
| A10 | Rainbow bridge — east termination | `MCCNewHub` shows the rainbow ending at the dome's upper structure (a red canopy), i.e. it lands **on** the dome. The dome is reserved and not built by anyone. | Bridge stops at x = -31 with a small gold-trimmed balcony, **1 block clear of the reserved plinth radius r 30 and 24 blocks above it**. V4-E must decide how it meets the dome. |
| A11 | Belvedere (bridge head) | `MCCNewHub` shows a red/white structure where the rainbow leaves the mountain, but it is small and partly behind rock. | Built a stone belvedere on the ridge shoulder: masonry body, stone deck, white parapet, gold finials, lantern posts. Its exact form is inferred. |

---

## B. Deliberate departures (brief vs reference, or reference vs itself)

### B1. The brief's biosphere site is south-**east** of the mountain; the reference reads south-**west**
`BRIEF.md` puts the biosphere at `-70,120,45`. In `MCCHubSeason1` the biosphere's centre is
roughly 30 blocks **west** of the marquee's centre, and the marquee is clear of it to the
right. At `-70` the dome sits directly in front of the marquee: from the south-east it hides
~74 % of the sign's width, and from the entrance it hides everything below y = 136.

**Built to the brief** (`-70,45`), because V4-A/V4-C/V4-D all build to the same coordinate
set. The marquee was then raised to y 130..161 so its two letter rows clear the dome's
silhouette. If the owner prefers the reference relationship, move the whole biosphere to
about `-85,120,48` (plinth r 21.5 still clears the mountain apron at z = 20) and the
occlusion drops to a sliver — no other change needed, both are self-contained.

### B2. The brief conflates two different structures
`HallOfFame.png` is a **building** (red facade, white `HALL OF FAME` letters, teal glass,
yellow crenellated parapet). The biosphere is a **glass dome** (`MCCHubSeason1`,
`MCCNewHub`, `MCCHub-13`). The brief asks for "glass dome r~20 … `HALL OF FAME` sign".

I built the dome and mapped the *sign design* from the building onto it: quartz frame, red
panel, white `HALL` / gold `OF` / white `FAME` stacked, letters mounted straight on the red
(no black board — the reference has none for this sign). The building itself is not built.

### B3. The sign's typo
`HallOfFame.png` reads `HALL / OF / FAHE` — the original build has a typo. I built **FAME**.

### B4. Sign faces
The brief says the mountain's sign faces "east/southeast". `MCCHub-SD` (the primary
composition reference, camera at the south entrance) shows `THE CHAMPS` almost face-on, so
the board's normal is within ~20° of south. **Built south-facing** (`+z`). An east-facing
board would be invisible from the entrance and would alias the font. This is within 45° of
"southeast", and it matches SD, S1 and MCCHub-13.

### B5. The marquee is mounted high, not on the ground
`MCCHubSeason1` puts the board's bottom ≈ 5 blocks above the mountain's base. That is only
possible if the rock behind it is low there. My cliff is ~30 tall across the sign's width,
so a ground-mounted 32-tall board would be swallowed by rock from the south. The board
therefore stands at y 130..161 on a 40-tall rock face, with the spires (y 164..177) above it.
The proportions (board ≈ 2/3 of the visible rock face, crown above, spires above that) match
the reference; the absolute height is +9 against S1's reading.

### B6. Champions stage not built
`TheChamps.png` shows the champions' stage: black cave, gold `MCC CHAMPIONSHIP WINNERS`
coin, white `MCC` letters, red carpet, gold plinths, checkered kerbs, spotlights, an orange
canopy truss on chains, red banners. That sits at the mountain's toe, **outside V4-B's
scope** (mountain, biosphere, bridge, globe). It is not built and nothing pretends to be it.
Suggested owner: whoever owns the plaza (V4-A) or a new pass.

The "CC" arch under the sign in `MCCHubSeason1`'s crop is that stage's backdrop, not a
separate door — I therefore did **not** invent a gate arch in the mountain face.

### B7. Footprint axes
The brief gives the mountain as "55×40". Read as a north–south ridge, mine is **44 (x) ×
51 (z)** — same area, axes swapped, because the ridge runs away from every camera in the
references. The east toe is at x = -72, the west edge at x = -116, the apron ends at z = 20.

### B8. The mountain's height
Brief: "height ~50" (y ≈ 170). Mine peaks at y = **177** (57 blocks) because the tallest
spire (`SPIRES` (-99,-7,56)) plus the ridge plus jitter overshoots. Within 12 % of the brief.

### B9. Interior of the snow globe is a bowl, not a flat disc
A flat floor inside a 14-radius sphere can only be ~7 wide. The reference's interior clearly
shows trees standing on rising ground near the rim. I therefore used a **concave bowl**
following the shell at 1.2 blocks clearance, which is both geometrically valid and what the
reference looks like. Presents and trees sit on the bowl.

---

## C. Not verified at all

* **No in-game check.** Nothing was pasted, no function was run, no world was touched.
  Lighting, collision, glass shard behaviour and whether the lanterns read at night are all
  unknown. `minecraft:glass` panels on a curved shell can leave 1-block diagonal gaps —
  they are covered by ribs at every 45°, but this has not been looked at in game.
* **No front-elevation render.** `tools/render_preview.py` only does isometric and
  top-down, so the brief's "side-by-side and overlay against `MCCHub-SD.png` / `-S4` / `-S6`"
  has **not** been done. That is the single biggest outstanding acceptance item.
* **Overlap check is mis-scoped.** `mountain_in_hof_plinth = 3,633` counts the biosphere's
  own plinth, not rock (see STATUS §4.3). The true value is expected to be 0 but is unproven.
* The preview's snow disc at y = 120 is **preview-only context**. It is not in the emitted
  functions.
