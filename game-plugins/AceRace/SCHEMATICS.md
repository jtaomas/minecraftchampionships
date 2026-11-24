# Ace Race v1 — map schematics (for owner approval)

**Maps:** Sunset Skyway (ground/elytra, 3 laps) · Kessler Run (space, elytra/riptide, 2 laps).
Both are original layouts. The reference maps are used for structural principles only; no layout, name, palette or set piece is copied.

**Read with:** `SPEC.md`. This file is the build gate for the two maps.

---

# 1. SUNSET SKYWAY

**Theme line:** *“The last race before the lights go out.”*
**Fantasy:** a warm harbour city at sunset — a boardwalk start, a crane yard, old-town rooftops, a thermal tower, a cloud bridge over the bay, a lighthouse island, a market switchback, a cargo yard, and a final straight under a giant sun.

## 1.1 Top-down (topological, not to scale)

```
                              N
                              ^
                  [6] LIGHTHOUSE ISLAND
                     (landing, drop −25)
                              │
          [5] CLOUD BRIDGE (elytra, 3 ring gates)
             over the bay, west → east
                              │
   [4] THERMAL TOWER ─────► [7] MARKET SWITCHBACK
      (wing ring, +55)          (old-town lanes)
            ▲                         │
   [3] ROOFTOP DASH                   │
      (old-town roofs)                ▼
            ▲                  [8] CARGO ZIP
   [2] CRANE CLIMB                (launch pad)
      (jump pads)                     │
            ▲                         │
   [1] DEPARTURE BOARDWALK ◄── [9] SUNSET STRAIGHT
      START / FINISH ARCH ◄──────────┘
```

The lap is a **closed circuit**. Start and finish are the same 40-wide boardwalk arch, crossed from opposite sides. The lighthouse is visible from the start; the giant sun disc is visible from most of the second half.

## 1.2 Sections

| # | Section | Tests | Length | ΔY | CP | Expert split |
|---|---|---|---:|---:|---|---:|
| 1 | Departure Boardwalk | sprint line, speed pads | 100 b | +2 | CP1 | 9 s |
| 2 | Crane Climb | jump pads, vertical read | 80 b | +18 | CP2 | 12 s |
| 3 | Rooftop Dash | short hops, edge control | 120 b | +8 | CP3 | 15 s |
| 4 | Thermal Tower | wing ring, launch | 60 b | +27 | CP4 | 8 s |
| 5 | Cloud Bridge | glide line, ring threading | 220 b | −10 | CP5 | 22 s |
| 6 | Lighthouse Island | landing, controlled drop | 90 b | −25 | CP6 | 12 s |
| 7 | Market Switchback | slow fields, tight turns | 130 b | +4 | CP7 | 18 s |
| 8 | Cargo Zip | launch pad, committed line | 80 b | −6 | CP8 | 10 s |
| 9 | Sunset Straight | pure sprint, finish read | 110 b | 0 | Finish | 14 s |

**Totals:** ~990 blocks per lap · expert lap **2:00–2:15** · median **2:20–2:40** · slow **2:50–3:10** · 3 laps · **10:00 cap** · winning time ~6:00–6:45 · vertical range **80 blocks** · footprint ~**420 × 360** · block budget ≤ **300k**.

## 1.3 Elevation profile

```
 +55 │                    ▄▆█ S4 thermal tower
 +40 │              ▄▆▆▆▆     ▄▄▄
 +20 │      ▄▆▆▆▆                  ▄▄▄ S7
   0 │ █▄▄▄▆        ▄▄▄▄▄▄▄▄▄▄        ▄▄▄▄▄ S9 finish
 -10 │                    ▄▄▄ S5 bridge
 -25 │                         ▄▄▄ S6 lighthouse
     └──────────────────────────────────────────────▶
       1    2    3    4    5    6    7    8    9
```

Rules:
- The thermal tower is the high point; the lighthouse drop is the low point. No drop over 30 blocks without a recoverable surface within 10 blocks horizontally.
- The finish is at the start elevation, under the arch; the last straight is flat.

## 1.4 Palette

| Role | Blocks |
|---|---|
| Boardwalk / start | warm cream concrete, birch, stripped dark oak, brass trim |
| Old town | coral terracotta, brick, cream concrete, dark oak, copper roofs |
| Crane yard | iron, deepslate, copper, yellow-black hazard stripes |
| Thermal tower | copper, oxidized copper, orange stained glass, glowstone |
| Cloud bridge | white concrete, light-blue glass, quartz, sea lanterns |
| Lighthouse | white quartz, red concrete, cyan glass, copper lamp |
| Market | teal, coral, mustard, string lights, striped awnings |
| Cargo | containers in team-neutral colours (no team colours on the track), rust, dark steel |
| Ground/water | sand, prismarine, water, kelp; sunset sky |
| Light | lantern, sea lantern, froglight (warm), end rod |
| Signal | lime chevrons, white arrows, orange speed pads |

No team-coloured track surfaces; team colour belongs to the grid, HUD and fireworks only.

## 1.5 Landmarks

- **L1 — The Lighthouse** (hero, seen from the start and the bridge): a tall white-and-red tower on a rock island, its lamp visible across the bay. The finish straight points at it before turning home.
- **L2 — The Giant Sun** (backdrop): a 60-block disc of orange/yellow glass and concrete on the horizon behind the thermal tower; the map’s colour source.
- **L3 — The Crane** (mid-ground): a 45-block harbour crane with a fallen arm that forms one shortcut.

## 1.6 Shortcuts (max 6 s each, never skip a checkpoint)

1. **Fallen crane arm (S2):** run the arm instead of the stairs; saves 4 s; failing drops 8 blocks to the yard, recoverable.
2. **Rooftop gap (S3):** jump a 4-block gap between two roofs; saves 5 s; failing drops 6 blocks to a balcony.
3. **Thermal updraft (S4–S5):** enter the wing ring from the high side; saves 6 s; requires a clean launch, otherwise you start the glide low.
Every shortcut is visible before commitment and marked with one lime arrow.

## 1.7 Start / finish / spectator

- **Start:** ten team bays on the boardwalk, one row deep, behind a glass barrier; a single white line. Above the arch: the lighthouse and the sun. 10 s frozen countdown; barrier drops; no GO word.
- **Finish:** the same arch from the north, checkered black/white line, firework in the player’s team colour *inside* the arch.
- **Spectator:** a lighthouse terrace grandstand looking back down the bridge and the finish straight; overview at the bay centre with the lighthouse, bridge and finish in one frame.

## 1.8 Validation targets

- [ ] 40 spawns with solid ground and 2-block headroom; barrier real blocks.
- [ ] Every section has a floor route that does not require a rocket or an expert shortcut.
- [ ] Each shortcut saves ≤6 s and rejoins before the next checkpoint.
- [ ] Lighthouse drop lands on a recoverable surface every time.
- [ ] Finish gate is directional; no side entry.
- [ ] Block count ≤300k; paste ≤15 ms/tick.
- [ ] Offline renders from two angles; route/timing validator passes.

---

# 2. KESSLER RUN

**Theme line:** *“A dead ring. A live race.”*
**Fantasy:** a broken habitation ring and a scrapyard orbit — leave the station, cross the void, thread the ring, run the freighter spine, dive back into the intake. Copper/bone/violet under a banded gas giant.

This is the original Space Race-inspired map. It keeps the *fantasy* (a home you leave and return to, silence and exposure, scale by detail, fragility) and none of the reference layout, naming or palette.

## 2.1 Top-down (topological, not to scale)

```
                         [L1] BROKEN HABITATION RING
                              (hero, tilted 15°)
                                       │
        [S8] RING PASS ────────────────┼────────────── [S7] COMET ICE
              │                        │                      │
        [S9] FREIGHTER SPINE           │              [S6] CRYSTAL MINE
              │                        │                 (riptide)
        [S10] RETURN / INTAKE          │                      │
              │                        │              [S5] SOLAR SAIL
        [S11] DEBRIS SLALOM            │                 (glide rings)
              │                        │                      │
        [S1] DEPARTURE HALL ◄── [S2] DISH FARM ◄── [S3] GANTRY ◄── [S4] LAUNCH
              ▲                                                      │
              └────────────────── FINISH ◄───────────────────────────┘
```

The lap is a loop that leaves the station, orbits the ring, and returns through the intake. Start and finish are the same hangar line.

## 2.2 Sections

| # | Section | Verb | Length | ΔY | CP | Expert split |
|---|---|---|---:|---:|---|---:|
| S1 | Departure Hall | sprint | 90 b | 0 | CP1 | 7 s |
| S2 | Dish Farm | sprint + 2 hops | 110 b | +2 | CP2 | 10 s |
| S3 | Gantry Run | tight catwalk | 80 b | +8 | CP3 | 10 s |
| S4 | Launch Spine | elytra launch | 60 b | +18 | CP4 | 5 s |
| S5 | Solar Sail | glide, 3 rings | 220 b | −8 | CP5 | 20 s |
| S6 | Crystal Mine | glide→land→riptide | 160 b | −35 | CP6 | 16 s |
| S7 | Comet Ice | ice + glide hops | 130 b | +8 | CP7 | 14 s |
| S8 | Ring Pass | glide, thread | 200 b | +22 | CP8 | 18 s |
| S9 | Freighter Spine | sprint + 30-block drop | 150 b | −30 | CP9 | 14 s |
| S10 | Return | glide → intake finish | 180 b | −10 | Finish | 16 s |
| S11 | Debris Slalom *(optional branch)* | glide | 90 b | −6 | — | 8 s saved max |

**Totals:** ~1,470 blocks per lap · expert lap **1:55–2:05** · casual **2:30–2:50** · 2 laps · **8:00 cap** · winning time ~4:00–4:30 · vertical range **85 blocks** · footprint ~**600 × 480** · block budget ≤ **250k**.

**No rockets in v1.** The map is built so every glide has a ring line and a floor route; rockets are an `[ORIGINAL]` config option deferred.

## 2.3 Elevation profile

```
 +40 │                         ▄▆█ S8 ring
 +30 │              ▄▆█ S4    ▄▆   ▄▆
 +20 │   ▄▆ S1–S3 ▄▆   ▄▆___▆      ▄▆___
   0 │  ▄                              ▄▆___ S9 spine
 -20 │ ▄                                    ▄▆___
 -35 │        ▄▆ S6 flooded shaft               ▄▆ S10 intake ▄▄ finish
 -45 │        █ (lowest point is water)
     └──────────────────────────────────────────────▶
       S1  S2  S3  S4  S5  S6  S7  S8  S9  S10
```

Rules:
- The lowest point is water (S6), never void.
- No vertical drop over 45 blocks without an intermediate surface within 12 blocks horizontally.
- Start/finish at Y 0; every glide descends from a higher point, so a bad launch has a fallback.

## 2.4 Palette

| Role | Blocks |
|---|---|
| Structure | smooth quartz, calcite, diorite, light grey concrete, polished diorite |
| Metal/trim | oxidized copper, weathered copper, cut copper stairs, copper grate |
| Glass | light blue/cyan stained glass, tinted glass; teal only where light passes |
| Rock | end stone, smooth basalt, tuff, grey terracotta, packed mud (pale grey-gold, not orange) |
| Crystal | amethyst, budding amethyst, purple stained glass, magenta concrete |
| Signal | lime concrete, lime glazed terracotta, white concrete |
| Danger | coral/red/black concrete — the only red in the map |
| Water | water, blue ice, packed ice |
| Light | sea lantern, teal froglight, end rod, amber shroomlight |
| Sky | `#07070F` base, banded gas giant 400–600 blocks out, two small moons, three star depths |

## 2.5 Landmarks

- **L1 — The Ring:** a broken habitation ring ~90 blocks across, tilted 15°, half shattered with a debris arc. The route passes through it once and around it twice; it is the map’s clock.
- **L2 — The Derelict Freighter:** a ~60-block cargo hauler snapped in half, spine exposed, containers spilling. The route runs the spine then drops off the broken end.
- **L3 — The Dish Farm:** twelve radio dishes on a bone-white platform, one fallen; the postcard behind the start and the last thing before the final dive.

## 2.6 Shortcuts (max 8 s each, never skip a checkpoint)

1. **Fallen dish (S2):** hop the fallen dish instead of running around; saves 4 s; failure drops 12 blocks to the deck.
2. **Solar sail thread (S5):** aim between the arrays instead of following the rings; saves 6 s; 3-block gap; rings still work.
3. **Freighter drop (S9):** leave the spine early onto a drifting container, then glide to the intake; saves 7 s; failure drops 30 blocks to a recovery platform.
4. **Debris slalom (S11):** an alternative final approach through moving containers; saves 8 s; rejoins before the final checkpoint, so it cannot skip progression.
All are visible before commitment and marked with a lime arrow.

## 2.7 Start / finish / spectator

- **Start:** ten team bays in a dark copper hangar, one white line, open end framing the Ring and the gas giant. 10 s frozen countdown; front gate drops; runway chevrons light in sequence; no GO word.
- **Finish:** a 9-block lit intake mouth in the station’s underbelly, copper rim and pulsing lime ring; inside, a short hangar and the same white line, now checkered. Firework in team colour inside the hangar.
- **Spectator:** grandstand on the outer hull above/behind the start, looking through the open roof at the Ring; overview at the Ring centre offset 40 blocks; finisher balcony inside the intake.

## 2.8 Wing-ring / riptide technical notes

- Wing ring: a light-blue ring; on pass, `setGliding(true)` and tag `kessler.gliding`; on landing, remove elytra. If a client drops gliding mid-air and velocity > 0.35, allow one silent re-glide.
- Ring detection: plane crossing + radius band (`r_inner < dist < r_outer`), not a box; non-solid ring blocks so they cannot be landed on.
- Riptide: trident + Dolphin’s Grace in S6; water/rain only; cancel trident throws; two-lane minimum in every riptide gate.
- Fail plane Y −60 relative to map origin; recovery to last checkpoint within 3 s; also recover after >3 s falling with no elytra and no ground within 6 blocks.
- Weak glider: every glide has a floor route; ring radii 4 blocks (3.2 clear); no glide requires a rocket.

## 2.9 Validation targets

- [ ] 40 spawns, barrier, start strip.
- [ ] Every elytra section has a ring line and a floor route; zero-rocket completion proven.
- [ ] Every shortcut ≤8 s and rejoins before a checkpoint.
- [ ] Ring detection passes a grazing test and fails a reverse crossing.
- [ ] Riptide section works in water; no dry riptide.
- [ ] Fail plane and fall recovery ≤3 s.
- [ ] Block count ≤250k; hero ring, freighter and dish farm visible from the start.
- [ ] Offline glide/route validator + real-client elytra test.

---

# 3. Build order

1. **Test Grid** (small, 1 lap, 3 checkpoints) — proves the contract, world, paste, start, checkpoint, pads, finish, HUD, spectator.
2. **Sunset Skyway** — ground/elytra, no riptide; the safer first full map.
3. **Kessler Run** — space, wing rings, riptide; the harder map.
4. **Verify both** — contract checks, route/jump/glide validators, two-angle renders, bot lifecycle, real-client elytra/riptide.
5. **Owner playtest** — feedback file, fixes, v1 sign-off.

No map block is placed before this file and `SPEC.md` are approved.
