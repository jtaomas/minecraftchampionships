# V4-A — GAPS (angles and details not visible in the reference)

Only things that are genuinely unseen, ambiguous, or deliberately invented are listed. Every
entry says what was built and why, so a reviewer can overrule it. No claim of 1:1 is made where
the reference does not show the answer.

## 1. The balustrade across the axis is continuous in the reference

`MCCHub-SD.png` and `MCCHub-S6.png` both show a stone balustrade spanning the full width between
the two giant columns with no visible gap, although the red carpet stairs, the yellow dome-island
floor and the dome are directly behind it on the same axis. A continuous 4-block rail cannot be
walked through.

Built: the rail runs along the whole dome-island rim, but there is a **9-wide gate opening on
the axis** between two chiselled gate piers with lanterns. From the south and from iso angles it
reads close to the reference; up close it is an opening. Alternative if the owner prefers the
literal photo: close the gap and treat the bridge as a dead-end viewing balcony (players would
have to be teleported). **Needs an owner decision.**

## 2. Bridge deck level vs plaza level

`MCCHub-S4.png` shows three brick steps rising from the red-brick plaza to the terrace that
carries the balustrade, i.e. the bridge terrace may sit ~3 blocks above the plaza. `MCCHub-S6.png`
is shot from on the bridge and shows no step, and `MCCHub-SD.png` is flat at the columns' feet.

Built: **one flat level at y=120** for plaza, bridge and dome-island floor. Reason: keeps the
axis walkable and the plinth stairs as the only rise. A 2–3 block raise of the whole entrance
terrace is a contained change if the owner wants S4's steps.

## 3. Water: the reference hub sits in a sea

`MCCHub-S4/S6/SD` show water immediately behind the balustrade and along the east coast; the
aerials (`MCCHubSeason1.png`, `MCCNewHub.jpg`) show the hub as an island group in open sea.
The build target is a void world, and the brief says "water at the edges" without defining the
edge.

Built: a **closed 14-wide moat** `r 38..52` around the dome island (the LAYOUT_V2 reading of the
hub), plus shore ice. No outer sea. The island floats over void with a rock keel. The moat is
what the front views can actually see. **Invented/interpreted:** the island's outer coast shape
(7 lobes, 2 coves) and the keel are not traced from any photo; no reference gives the island's
plan outline at 1:1.

## 4. Column banner lettering and the festive variant

`MCCHub-S6.png` shows **white** 3D letters on a red banner with no shelves. `MCCHub-SD.png`
(festive, primary) shows **gold/orange** letters with three pale **ice shelves** between them,
plus a red bow with a gold bell at the top.

Built: white `white_concrete` letters (as S6 and as the brief's "white MCC letters"), the red bow
(SD), and **no ice shelves** (they would have required a 37-block banner and fought the white
letters). Lettering is a hand-drawn 7×7 font, not traced from the photo; the exact glyph shapes
and the banner's aspect (13 wide × 33 tall) are estimated from S6.

## 5. Column crown profile

The gold crown is seen from below in S6/SD; its plan depth and the number/width of merlons are
not countable. Built as a 13×5 solid sill + full ring + 2-on/1-off crenellated band (4 tall),
with gold side lamps on stone arms at capital height (these are visible in SD but their exact
mount is not).

## 6. Plinth: rainbow order and height

`MCCHubSeason1.png` (older hub) is the only clear plinth reference and shows a much taller ring
(~15–20 blocks) with ~20 vertical colour bands, a black top band and dark posts around the rim.
The v4 brief fixes the plinth at `r 28..30, y 120..128`, which is much shorter than Season 1.

Built: 20 bands (two cycles of the 10 team colours) with the warm end red at due south and cool
running east, blackstone band + dark cap, 24 dark rim posts at y=129..131. **The posts exceed
the brief's y=128** deliberately, to keep the Season-1 silhouette; they are the only blocks above
128 and are not dome geometry. The exact number of bands and whether red faces the entrance is
an interpretation (the photo is oblique).

## 7. The MCC floor emblem pattern

`MCCHub-S4.png` shows a circular red/yellow floor mosaic with a grey stone rim on the plaza. It
is only seen obliquely, at ~20 px/block, partly covered by players.

Built: a 25-wide circular mosaic at `0,120,86`: stone rim, 1-block yellow ring, red field, and a
yellow **crown over an M** motif. The reference's inner motif is chunky and asymmetric and could
not be traced reliably; the crown+M is the MCC logo's read, not a pixel trace.

## 8. Team-colour row at the column feet

SD shows a row of team-coloured columns at the base of the giants, apparently 5 per side, ordered
warm→cool from the outside in on the left and cool→warm outside in on the right; the outermost
colours are cropped by the frame. Snowmen stand on some in SD (props, V4-D).

Built: exactly **10 columns in team order** (red … pink) at `z 60..62`, `x ±18/±25/±32/±39/±46`,
3×3, 10 tall, with stone caps and snow. This is the **entrance decoration**; V4-C's 10 team
columns with four skin pedestals are a separate structure and are not built here. Their exact
spacing and count at the entrance are an interpretation.

## 9. Where the paths fit into the other agents' work

No reference gives the hub's path network at 1:1, and V4-B/C/D had not published footprints at
checkpoint time.

Built: three 5-wide stone-brick stubs — west to the Hall of Fame door (`-49,46`), east along the
arm to `96,18` (toward the aqua building), and a short north-east spur. These stop at plausible
door/plot edges and **must be confirmed with V4-B/D**; they are the most likely overlap risk.

## 10. Island ground levels under other agents' landmarks

The brief fixes THE CHAMPS at `-95,120,0`, the Hall of Fame at `-70,120,45`, the snow globe at
`75,125,55` and the aqua building at `80,120,0`, which forces the island top to stay at **y=120**
in those places. That is why no broad stepped land terraces were added (they would either float
those builds or need the other agents' coordinates changed). "Terraces" in this package are the
raised planter terraces, the plinth stair and the coastal sand lip.

## 11. Snow amount

SD is heavily snow-covered; S4 has none; S6 is autumn/clear. This package is the festive variant:
large snow drifts on grass with `snow_block` cores, snow on walls/copings/rails/column tops, and
clear paths/plaza. The drift pattern is noise-generated, **not** traced — no photo shows the
whole island's snow distribution.

## 12. Things deliberately absent

Dome (other bot), THE CHAMPS, Hall of Fame/biosphere, rainbow bridge, snow globe, aqua building,
training portals, 10 team columns + 10 stage plots, player heads, trees, boats, balloons,
presents, market stalls, candy canes. Also absent: interior/underside detail under the island
plate (the plate is a hollow shell with a keel; nobody stands under it), and any block below
y=96.
