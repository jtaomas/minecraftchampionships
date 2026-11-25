# Space Race — route, leg by leg

Source world: `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Space_Race\Space Race`
Version 1.18.1 (DataVersion 2865) · 14,198 chunks · 296,699 non-air blocks
Live world: `mcc_spacerace` · 29 command blocks · 22 signs · 0 containers · 0 entities

Confidence tags: **H** = read directly from NBT/block dump/render, **M** = inferred from two
independent observations, **L** = guess. Coordinates are block coordinates; `y` is the block
the item sits in, so a player standing on it has feet at `y+1`.

This is a **lap map**, not point-to-point. The same 23-block gate is start and finish.
The world's own scoreboard (`LapCheck1`, `Minutes`, `Seconds`, `milliseconds`, `Restart`) shows
the intended loop, but **no command block in the world writes any scoreboard** (H: all 29
commands in `data/command_blocks.csv`). The loop below is reconstructed from geometry plus the
author's own gallery captions ("StartFinish Line", "Trident Rings", "Pool Island", "Water
Features", "Luminous Cavern", "Elytra Islands", "Main Building Entrance").

---

## Leg 0 — Start / finish line (pod on the south face of the main building)

| Item | Coordinate | Evidence |
|---|---|---|
| World spawn | `17.45, 115.0, 27.58`, yaw `-179.99` (north), pitch `-0.77` | H `level.dat` Player, `playerdata/9ed6…dat` |
| Spawn floor | gray concrete `y=113`, gray concrete powder `y=114` | H block dump |
| Start kit | Trident **Riptide II** (slot 0) + Carrot on a Stick named **"Restart Attempt"** (slot 4) in the test player; the survival racer carries only the trident | H `playerdata/00000000-…dat`, `playerdata/9ed6…dat` |
| Start gate | `z=24`, `x=6..28`, `y=115..117` — alternating black/white stained-glass panes | H block dump |
| Gate opening | `/fill 28 115 24 6 117 24 air` at command block `10,111,19` | H |
| Start button | stone button `6,116,28`, wall-mounted facing east | H |
| Countdown | impulse `10,111,28/25/22` + chain `11,111,28/25/22` | H |
| Countdown text | title `Starting in` (dark_aqua) + subtitle `>3<` red → `>2<` yellow → `>1<` green | H |
| Countdown cadence | `LastExecution` 2425233 → 2425249 → 2425265 → 2425281 = **16 ticks apart** | H |
| Speed pads | magenta glazed terracotta `9..11,114,18..22` and `17,109,55` | H |
| Runway | white/quartz platform `x≈10..40, z=32..60`, top `y=109..110`, blue arrow pointing **south** | H render `renders/pod_iso.png` |

**What happens:** button → repeater loop at `x=8, z=19..28` (six repeaters, delay 4) pulses the
three title pairs bottom-to-top, then the fill 16 ticks later. That is a **3-2-1-GO** in 2.4 s,
not the 3 s the text implies (M: 4 commands × 16 ticks).

The player faces **north** into the building (H). The runway arrow behind them points **south**
(H). Those two cues disagree; see `README.md` §6.

**Reset gap:** no command ever restores the glass panes at `z=24` (H). Lap 2 starts with the
gate already open.

---

## Leg 1 — Main building entrance → Luminous Cavern

| Item | Coordinate | Evidence |
|---|---|---|
| Pod floor | `x=7..39, z=18..31`, top `y=114` | H |
| Gate room | 3 magenta pillars `x=9..11, z=18..22`, carpet `z=23`, lime-concrete stripe `z=24` | H |
| Door into hall | pod meets hall at `z≈0`, floor steps down to `y=107` | M (block dump + render) |
| Hall shell | `x=-44..163`, `z=-116..60`, `y=83..218`; roof gray concrete `y=217..218` | H |
| Lethal floor | lime stained glass `y=89..107`; command `if block ~ ~-0.35 ~ lime_stained_glass → kill @a` at `23,112,16` | H |
| Light grid | 5,756 `minecraft:light[level=15]` at `y=108..130` | H |
| West catwalk | springs from `z≈0` at `x≈-30`, rises north to `y≈150` at `z≈-60` | H render `renders/hall_catwalk_plan.png` |
| East catwalk | mirrors it at `x≈65`, rises to `y≈159` at `z≈-73` | H |

**The floor is a kill plane.** The entire cavern floor is lime glass; touching it runs `kill @a`
(H: block type + command). The only safe route is the two sandstone-coloured catwalk arches that
spring from the south wall and climb north over the floor (H render).

### Leg 1a — Cavern ascent pads (red concrete = levitation)

| # | Coordinate | Height | Measured lift needed to next |
|---|---|---|---|
| 1 | `-26,128,-30` | 128 | +6 |
| 2 | `-14,134,-58` | 134 | +6 |
| 3 | `10,140,-67` | 140 | +9 |
| 4 | `26,149,-47` | 149 | +10 |
| 5 | `32,159,-73` | 159 | +6 to ring C |
| — | `40,134,-68` | 134 | branch pad |
| — | `60,128,-31` | 128 | east arch base |

All are `smooth_stone_slab(top)` + yellow concrete + **red concrete cap** (H). Red concrete runs
`effect give @p levitation 2 23` and `speed 2 30` (H) = ~48 blocks of lift + Speed 30 for 2 s
(M: 0.05 × 24 × 40 ticks ≈ 48).

**Intended reading:** walk up the arch, hop pad to pad, launch at the top pad through the nearest
elytra ring. The pad heights match the ring heights (134 + 48 ≈ 182 vs ring A 165–175; 159 + 48 ≈
207 vs ring C 205–215), so the tuning is deliberate (M).

---

## Leg 2 — Elytra Ring A (west wall) → West Platform

| Item | Coordinate | Evidence |
|---|---|---|
| Ring A | plane `x=-44`, `z=-48..-40`, `y=165..175`; frame 9 wide × 11 high, opening 7 × 9, 1 thick | H |
| Exit side | west face of the main building | H |
| Landing | West Platform `x=-128..-98`, `z=-60..-13`, top `y=149..157` | H |
| Platform build | gray concrete 661, smooth-stone slabs 307, sea lanterns 56, magenta terracotta 120 | H |
| Magenta pads | `-122..-121,157,-19..-17` (6 blocks) | H |
| **Side effect** | any magenta block runs `setworldspawn` (command `11,112,16`) | H |

**Mechanic:** stand on a diamond block → elytra. But see §7: all eight elytra commands test
`~ ~ ~-1 … ~ ~ ~-7`, a **horizontal** offset, not the block below. The grant is unreliable (H
command text; M consequence). If it fires, glide west/down onto the platform.

**Spawn trap:** the magenta pads here (and 723 magenta blocks map-wide) call `setworldspawn`
(H). Stepping on one moves the world spawn to that pad. This is almost certainly unintended.

---

## Leg 3 — Elytra Ring B → Pool Island (A)

| Item | Coordinate | Evidence |
|---|---|---|
| Launch pad | red concrete `-122..-121,157,-19..-17` | H |
| Lift | +48 blocks → apex ≈ `y=205` | M |
| Ring B | plane `z=3`, `x=-128..-120`, `y=200..208`; frame 9 × 9, opening 7 × 7 | H |
| Glide | ≈ 80 blocks south, 45 down, to Pool Island | M |
| Pool Island A | `x=-159..-126`, `z=82..130`, top `y=148..176` | H |
| Pool | water surface `y=160`, `x≈-148..-134`, `z≈90..110`, 16 deep | H |
| Rim | red sandstone top `y=176` | H |
| Black portal | blackstone/black-concrete/lime-concrete ring at the slide mouth `x≈-160..-120, z≈100..119` | H render `renders/islA_iso.png` |

Land on the **rim** (176), not the pool. The pool is 16 deep with vertical sides and no ladder
(H); a missed landing costs a riptide climb or a death.

---

## Leg 4 — Water Features: slide A → B

| Item | Coordinate | Evidence |
|---|---|---|
| Channel | `x=-144..-138`, `y=177..178`, `z=112..176`; 3 wide, all source water (`level=0`) | H |
| Sides | light-blue stained glass; red-concrete ribs at `z=114, 121–129, 137, 145–153, 161, 169–175` | H |
| Drop | at `z=112`, 17-block waterfall into Pool Island A | H section `renders/section_x-141.png` |
| Fall into B | at `z=176`, water drops into the basin below | H |
| Sign wall | `x=-143..-139`, `z=176`, `y=174..178` — **21 blank acacia wall signs** set in light-blue glass | H |

Water **clears the chestplate** (`if block ~ ~-0.35 ~ water → item replace … air`, command
`12,112,14`), so the slide is an elytra-off section by design (H).

The 21 signs are the map's only scoreboard surface: black, non-glowing, all four lines empty
(H). They face east into the basin, i.e. they are read by players *after* the drop (M).

---

## Leg 5 — Pool Island B → bridge → Platform C

| Item | Coordinate | Evidence |
|---|---|---|
| Basin B | `x=-160..-85`, `z=150..249`, top `y=166..188` | H |
| Pool B | water surface `y=176` | H |
| Portal | blackstone/black-concrete/lime ring on the south rim, `x≈-140..-120, z≈220..239` | H render `renders/islB_iso.png` |
| Bridge | water channel `z=224..233`, `x=-100..-30`, surface `y=176`, all `level=0` | H |
| Platform C | `x=-60..-17`, `z=200..245`, top `y=176..190`; red sandstone + gray landing strip | H |
| C arch | small gray arch at the east end `x≈-25` | H render `renders/rings_iso.png` |

Swim east; the channel is source water, so dolphins grace (always on) gives full speed (H
command `20,112,14`; H effect id 30 in playerdata).

---

## Leg 6 — Trident Rings

| Item | Coordinate | Evidence |
|---|---|---|
| Ring 1 platform | centre `-36,180,270`, radius ≈ 8, gray concrete + magenta pads | H |
| Ring 1 tube | glass cylinder `x=-40..-32`, `z=266..274`, water `y=180..198` | H |
| Ring 1 top | yellow/black hazard ring at `y≈198` | H render |
| Ring 2 platform | centre `-31,180,300` | H |
| Ring 2 tube | `x=-35..-27`, `z=296..304`, water to `y≈198` | H |
| Walkway | from Platform C east end south to ring 1, then ring 2 | H render `renders/rings_iso.png` |

The tubes are **water columns**: with permanent dolphins grace you swim up; with Riptide II you
can launch out of the top. The author's gallery calls these "Trident Rings" (H external caption;
M that this is the intended use).

---

## Leg 7 — Launch pad → Elytra Islands (F)

| Item | Coordinate | Evidence |
|---|---|---|
| Exit platform | `-33..-31, 184, 339..341`, red concrete pad | H |
| Lift | +48 blocks (levitation 23) + Speed 30 | M |
| Elytra Islands F | `x=-60..46`, `z=358..430`, top `y=174..201` | H |
| Pools | two water pools at `y≈180` and `y≈190` | H render `renders/islF_iso.png` |
| Red pad on F | `-37..-35, 189, 395..397` | H |
| Portal/ring | white/pink frame at `x≈40, z≈410` | H render `renders/islF_iso.png` |
| Orange mega pad | `40..42, 192, 418..420` — orange concrete, at the east tip | H |

Landing anywhere on red sandstone clears the chestplate (H command `12,112,12`), so F is an
on-foot section. Run east along the tail to the orange pad.

---

## Leg 8 — Orange mega pad → right-hand chain (turn)

| Item | Coordinate | Evidence |
|---|---|---|
| Orange pad | `40..42, 192, 418..420` | H |
| Effect | `levitation 1 70` + `speed 1 255` = ~71 blocks of lift | H command; M math |
| Apex | ≈ `y=263` | M |
| Landing | right-chain bottom island `x=108..127`, `z=416..439`, top `y=217..249` | H |

This is the only **mega** pad in the map (153 orange blocks total, but only this 3×3 group is a
deliberate pad; the rest is trim — H coordinate clusters).

---

## Leg 9 — Right-hand chain northbound (return climb)

| Step | Coordinate | Top y | Pad |
|---|---|---|---|
| Bottom island | `108..127, z=416..439` | 217–249 | red `113..115,244,418..420` |
| Middle island | `130..145, z=384..399` | 218–238 | red `134..136,228,392..394` |
| Upper island | `118..132, z=356..368` | 210–228 | red `119..120,218,362..364` |
| Space station | `98..148, z=259..335` | 187–214 | red `113..115,200,261..263`; orange trim `99..129,193..204,263..281` |
| Elytra Ring D | plane `z=246`, `x=108..117`, `y=239..251`; frame 10 × 13, opening 8 × 11 | — | diamond frame |
| Return glide | ≈ 186 blocks north, 135 down, to the start runway | — | — |

The chain **ascends** toward the station (217 → 249 at the bottom, 187–214 at the station), so
the red pads are climbing aids (M: pad height vs island height). Ring D sits 48 blocks above the
station's red pad (`200 + 48 ≈ 248` vs ring 239–251), exactly the levitation lift (M).
The glide home is a 1.4:1 descent — comfortable for elytra (M).

---

## Finish / lap

There is **no finish trigger** in the world (H: no command references a finish region, no
scoreboard write, no title on completion). The only player-facing text is the unconditional
actionbar `You are in 1st place` on a repeating command at `13,112,16` (H) — every player sees it
from spawn to logout, including in last place.

The lap is therefore closed **only by the player crossing the start gate at `z=24` again** (M from
the "StartFinish Line" caption + `LapCheck1` objective + the gate geometry). Nothing detects it.

## Zone summary

| Zone | Blocks that grant / clear | Coordinates |
|---|---|---|
| Elytra ON | diamond block within 7 blocks north at foot height (intended: ring) | 4 rings: `x=-44 z=-48..-40 y=165..175`; `z=3 x=-128..-120 y=200..208`; `x=88 z=-6..2 y=205..215`; `z=246 x=108..117 y=239..251` |
| Elytra OFF | red sandstone, water, red concrete, gray concrete under feet | map-wide |
| Lift (small) | red concrete → levitation 23 (2 s) + Speed 30 | 477 red concrete blocks |
| Lift (mega) | orange concrete → levitation 70 (1 s) + Speed 255 | 153 orange concrete blocks |
| Speed | magenta glazed terracotta → Speed II (4 s) | 723 blocks |
| Kill | bedrock, lime stained glass under feet | 1,900 bedrock / 65,821 lime glass — **`kill @a`: one player's trigger kills every player on the server** |
| Spawn move | any magenta → `setworldspawn` | 723 blocks |
| Water speed | permanent dolphins grace (all players, every tick) | command `20,112,14` |

Full command inventory: `data/command_blocks.csv`. Gate list: `data/checkpoints.csv`.
