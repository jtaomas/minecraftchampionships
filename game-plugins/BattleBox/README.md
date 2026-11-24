# Battle Box v2 (McmBattleBox)

Game id: `battlebox` (`/mcm play battlebox`). Display name: **Battle Box**. Core API 3.2.0, Paper 1.21.1.

## How it plays
- **Sides:** each tournament team is a side. Solo players are grouped into squads of about 4 (at least two squads if nobody is on a team). If everyone is on one team, it is split in two. One player alone plays 3 practice rounds against an empty side (no points).
- **Schedule:** round robin, up to 9 rounds (`max-rounds`), at least 3 (with 2-3 sides the pairings repeat). Odd number of sides: one side has a bye each round (+4 each).
- **Map rotation:** every round picks a new random arena theme (never the same twice in a row); all arena copies that round use it. `rotate-maps-each-round: false` uses one random theme for the whole game; `force-theme` pins one theme.
- **Each round:**
  1. **Kit select (20 s in round 1, then 15 s):** players wait in their side's spawn room behind team-coloured glass gates. The kit menu (chest GUI) opens by itself and can be reopened with the star in the hotbar. Picking a kit puts its items in your inventory straight away (they can't be used yet). Each kit can be taken once per side (twice for 6-10 players, ...). Teammates' picks show in the action bar and the menu. Anyone without a pick gets a random free kit.
  2. **Fight (no time limit by default; `fight-seconds` > 0 sets one):** the gates drop (the glass vanishes with block particles, glass-break + piston + bell + horn sting). One life; when you die you become a spectator: you are put high above your own half looking down at the centre and can fly around freely, but you are teleported back if you leave your arena's box. No friendly fire, no dropping, picking up or sharing items, arrows can't be picked up.
  3. **Results (5 s):** win/lose/draw titles, fireworks in the winner's colour over their arena, a results line per matchup in chat. Then everyone goes to the holding platform while the next arenas are built (under a second).
- **Centre:** a 3x3 pad set *into* the top of a two-step dais (walkable stairs on all four sides): the 9 cells sit
  flush with the yellow/black hazard-stripe ring around them (y 2 above the floor), with sea lanterns under the cells
  and in the four ring corners. The 9 cells **start filled with neutral blocks** (white concrete, or white wool in wool mode; light grey if a side is white). You must break a neutral or enemy block first and can only place your own team's concrete (default, iron pickaxe) or wool (`centre-block: wool`, shears) into an empty centre cell - nowhere else. You can't break your own colour. Centre blocks mine at normal vanilla speed with the plain centre tool (white concrete with an iron pickaxe about 0.45 s, wool with shears about 0.25 s) and drop nothing.
- **Winning a round:**
  - All 9 cells in your colour: win.
  - Wiping out the other side does **not** end the round: the survivors still have to fill the centre. If both sides are out (dead or offline), the round ends at once on centre count (equal = draw). Practice side out = round over.
  - **No time limit (default, `fight-seconds: 0`):** nothing else ends a round, so a round lasts until one of the above happens or an admin uses `/mcm skip`. A round *can* stall (both sides hiding, or survivors who will not or cannot fill the centre - each player has 16 centre blocks and broken blocks drop nothing).
  - With a time limit (`fight-seconds` > 0, min 15; the old default was 60): at time-out more centre cells wins; equal cells -> if exactly one side still has players alive it wins ("last side standing"), otherwise draw.

## Kits (loadout per theme)
Every kit also gets team-coloured leather armour, 16 centre blocks and the centre tool.

| Kit | Greenhouse Conservatory | Toy Workshop | Ski Lodge |
|---|---|---|---|
| Medic (sustain) | stone sword, bow, 8 arrows, 3 Bloom Bursts | stone sword, bow, 6 arrows, 2 Mending Charges, 1 Bloom Burst | stone sword, bow, 8 arrows, 2 Bloom Bursts, 1 Healing II splash (team only) |
| Marksman (ranged) | Power I bow, 16 arrows, 1 Updraft Charge | Quick Charge crossbow, 10 arrows, 1 Quickstep Charge | Quick Charge crossbow, 12 arrows, 1 Slip Charge |
| Tinkerer (utility) | Punch I bow, 6 arrows, 1 Sting Fuse Burst, 1 Spring Charge | Punch I bow, 4 arrows, 1 Smoke Fuse Burst, 1 Lift Burst | Punch I bow, 5 arrows, 1 Flare Burst, 1 Sting Fuse Burst |
| Runner (mobility) | Knockback I stone axe, 3 Quickstep Charges | stone axe, 2 Quickstep Charges, 1 Popper Crate (thrown TNT) | Knockback I stone axe, 2 Quickstep Charges, 1 Vanish Charge |
| Brawler (frontline) | iron sword, 1 golden apple | crossbow, 3 rockets, wooden sword | bow, 4 arrows, 2 Harming splash (enemies only), wooden sword |

Custom items:
- **Charges** (right-click = you, melee hit = that enemy): Quickstep (Speed II 8 s), Mending (Regeneration II 5 s), Updraft (Levitation 1.5 s), Vanish (Invisibility 6 s), Slip (dash + Speed III 3 s), Spring (Jump Boost III 8 s).
- **Bursts** (thrown, 3.5-block area where they land): Bloom (Regeneration II to teammates), Lift (enemies float up), Flare (enemies glow 10 s).
- **Fuse Bursts** (thrown, go off 1.5 s after landing with a shrinking particle ring): Sting (3 hearts to enemies), Smoke (blindness 4 s to enemies).
- **Popper Crate:** thrown primed TNT, 1.5 s fuse, never breaks blocks.
- `custom_model_data` hooks are in `Gear.Custom.model` (0 for now, TODO to link to the resource pack).

## Scoring
| Event | Points |
|---|---|
| Elimination (or last hit within 10 s) | 3 to the killer |
| Round win | 8 to each member of the winning side |
| Draw | 3 to each member of both sides |
| Bye | 4 to each member |

"+N" feedback appears in chat with a sound. With 10 teams (9 rounds), a strong player ends up with about 80-110 points.

## HUD
- Boss bar: countdown for the current phase; during an untimed fight it counts **up** (time since the gates opened, full bar).
- Sidebar: round x/9 and theme, phase (an untimed fight also shows the elapsed m:ss), the top matchups with their centre score, top fighter.
- Action bar: during kit select, your kit and your teammates' picks. During the fight: opponent, your eliminations, and the centre count as coloured squares (yours vs theirs).

## Arenas
Three themes, generated in pure Java (`arena/` package, no Bukkit), rebuilt for every match each round. Every
arena is a 35x35 floor (fixed size; the layouts are hand-placed) inside painted walls 13 high with a frieze. The two
teams' halves are a 180-degree rotation of each other (fair); inside one half, left and right are deliberately
different. Each theme has its own structural idea (research: `..\research\battlebox_layouts.md`, incl. the round-3
section on fan-made arenas). "Left" and "right" below are as seen by a player walking out of their own spawn.

| Theme | Archetype | Spawn building and exits | Left flank | Right flank | Levels |
|---|---|---|---|---|---|
| **Greenhouse Conservatory** (`greenhouse`) | **Terraced garden** (round 3 redesign): no building round the centre. An open court holds the dais, a fountain, low hedges and an orange tree; each half rises from it in a planting terrace with flower beds, an orange tree and an urn as cover | Potting Shed: right door, left door, indoor stair to the roof; from the roof a potting bridge drops you onto the terrace | **pergola walk + gazebo** (deck 5 up): back stair along the side wall, a short stair from the terrace, or a ladder from the court; the gazebo overlooks the dais under a leaf canopy; underneath is a shaded lane | **sunken fern walk** (2 deep, with a rill): hidden from the floor, three stair exits (back yard, rim path / terrace side stair, court) | trench (stand -1), floor (1), terrace (3), shed roof (5), gazebo (6) |
| **Toy Workshop** (`toyworkshop`) | **Open middle + bridge level**: a toy-train viaduct (deck 7 up, wall to wall) crosses over the open centre; a **skylight hole** above the dais (drop straight onto the pad; people crossing are exposed from below); arched piers at x +-8 / +-15 | Packing Room, no front door: a long low play tunnel, a wide open porch and a **roof hatch** (ladder) | open foam play-mat with the main viaduct stair and the **toy fort** (slit windows toward the centre, crenellated roof 4 up via an inside ladder - covered mid position under the bridge) | **two tiers**: the tight roofed shelf corridor on the floor, and the **shelf-top gallery** (5 up) on its roof - reached by its own stair or over the tunnel roof from the spawn roof - with a stair from the gallery onto the viaduct | floor (1), fort roof (4), spawn roof (5), gallery (6), viaduct (8); 3 ways up per side (main stair, pier ladder, gallery stair) |
| **Harbour Canal** (`canalships`) | **Canal split**: shallow canal (`\|z\| <= 6`) with central footbridge to dais island, moored cargo barges with gangplanks, dockside crane platform | Boathouse porch (left/right side arches + side dock door) | moored cargo barge + dockside crane platform (`y = 5`) | cargo yard + pier into canal | canal (0), floor (1), barge/crane (5) |
| **Grand Terminus** (`trainstation`) | **Rail concourse**: parallel passenger train carriages on flanks, central ticket-booth divider, open concourse around dais | Terminus porch (left/right platform doors + side alley door) | walk-through passenger carriage + roof (`y = 5`) | luggage carts + platform benches | floor (1), carriage roof (5) |
| **Sunlit Villa** (`villacourtyard`) | **Villa courtyard**: no front door — side-tucked loggia exits + garden arch, cypress-hedge 3-lane courtyard, raised colonnade balcony | Villa Atrium (left loggia + right loggia + garden arch) | raised colonnade balcony (`y = 5`) + shaded underpass | cypress garden + fountain cover | floor (1), colonnade balcony (5) |

How you reach the centre: Greenhouse - across the terrace and down its front stair / drop gaps, along the shaded lane
under the gazebo, or up out of the fern walk; Toy Workshop - straight across the open floor, under/over the viaduct, or
through the skylight; Ski Lodge - over your bridge or down onto the ice and up the island stair.

Offline checks (`tools/render.sh`, numbers from the current build):

| Theme | spawn exit -> enemy spawn exit | spawn exit -> far half hidden | own half -> enemy spawn exit blind | back third -> enemy back third blocked | pad top visible from mid-range ground / high spots | stair lanes | route check (strict) |
|---|---|---|---|---|---|---|---|
| Greenhouse | 100% of 3600 rays blocked | 100% of 611 spots | 100% of 611 | 72.5% | 0% / 86% | 32 OK | 3/3 exits, 13/13 waypoints, sealed |
| Toy Workshop | 100% | 100% of 749 | 100% of 749 | 86.9% | 0% / 71% | 14 OK | 3/3 exits, 16/16 waypoints, sealed |
| Ski Lodge | 100% | 100% of 518 | 100% of 518 | 78.9% | 0% / 67% | 14 OK | 3/3 exits, 11/11 waypoints, sealed |
| Harbour Canal | 100% | 100% of 452 | 100% of 452 | 87.4% | 0% / 100% | 6 OK | 3/3 exits, 5/5 waypoints, sealed |
| Grand Terminus | 100% | 100% of 572 | 100% of 572 | 91.9% | 0% / 100% | 6 OK | 3/3 exits, 4/4 waypoints, sealed |
| Sunlit Villa | 100% | 100% of 556 | 100% of 556 | 84.9% | 0% / 100% | 6 OK | 3/3 exits, 4/4 waypoints, sealed |

"Pad top visible" now tests the top face of the cells; since round 3 the cells are flush with the dais top (y 3),
which is above eye height from the floor, so from the ground you see the pad only from the dais steps or higher.

- **Sightlines** (`tools/Sightlines.java`, eye height 1.62): glass, panes, bars, fences, walls, leaves and small props
  count as see-through, the centre cells and gates as empty; only spots a player can actually reach (from the route
  check) are tested.
- **Route check** (`tools/Routes.java`, strict player-box model since round 3): flood-fill of where a 0.6 x 1.8 player
  can walk from each spawn, for both sides (walk; step up stairs and slabs; jump up 1 block; drop any height; climb
  ladders; gate glass open). Every step up needs full headroom in **both** columns - the one you leave and the one you
  enter - up to the new head height, because the body rises while it is still over the lower block. Each theme
  registers its spawn building's intended exits and a waypoint in every lane. It fails the render if the centre or any
  waypoint is unreachable, if any exit does not lead to the centre on its own (all the other exits closed), or if the
  centre can still be reached with every exit closed. The old (round-2) model is still run and printed for comparison.
- **Staircases:** every flight is placed by one helper (`ArenaTheme.staircase`), which clears 3 blocks above every
  step except the last (2) and above the approach, and is walked bottom to top with a player-box sweep: rise <= 1,
  flush landing, and no block anywhere the head passes while stepping up (a block 2 *or 3* above a step fails).
  A self-test plants head-height blocks in a test flight and fails the run if any is not caught.
  **Connections:** `Grid.postProcess` recomputes fence / pane / bars / wall / gate states and drops carpets that are not
  on a full block.

World `mcc_battlebox`: arena copies at x = slot * 100, y 64, z 0; holding platform at (-200, 110, 0); admin preview arena at (0, 64, -400).

## Admin commands (`battlebox.admin`, op by default)
- `/bb arena <greenhouse|toyworkshop|skilodge>` builds one arena copy at the preview spot and teleports you in.
- `/bb kits [theme]` opens the kit menu outside a game. Click a kit to see its real items.
- `/bb theme <id|off>` forces a theme for every round (saved to `force-theme`, applies from the next game).
- `/bb dump` dumps the preview arena to `plugins/McmBattleBox/dumps/` for `render_preview.py`.
- `/bb themes` lists the themes and the current settings.

## Config (`config.yml`)
`centre-block` (concrete|wool), `centre-tool-efficiency`, `rotate-maps-each-round`, `force-theme`, `first-kit-seconds`, `kit-seconds`, `fight-seconds`, `end-seconds`, `max-rounds`.

- `fight-seconds`: round time limit. **0 = no limit (default since round 3, owner request).** > 0 = limit in seconds
  (minimum 15; the old default was 60) with the time-out tiebreak. A server that already has a
  `plugins/McmBattleBox/config.yml` keeps its saved value (60) until it is changed to 0 there.

## Offline previews
`sh tools/render.sh [themeId...]` compiles the generators with plain `javac`, validates every staircase, both gate
passages and the routes (exit code 2 = broken, no PNGs), runs the sightline check (report in `target/offline/sightlines.txt`), writes
dumps and renders `previews/<theme>_iso_a0.png` (full), `_iso_a2.png` (other angle, roof off), `_top.png`,
`_cutaway.png` (roof off, near walls lowered) and `_floorplan.png` (top-down, everything above y 3 removed).
`NORENDER=1` skips the PNGs.

Defaults, not researched (adjust after playtesting): `centre-tool-efficiency: 0` - plain vanilla centre tool, so centre
blocks mine at normal vanilla speed; set 1-5 to make them faster.

## Known limitations
- The sidebar is global, so the per-player matchup is shown in the action bar.
- With more than 10 sides, the 9-round cap means not every pair meets.
- Arenas are rebuilt at the start of every round, not cleared when the game ends.
- Late joiners use the core's default spectator mode.

## Admin testing
- `/mcm skipwait` ends the intro, the kit selection (players without a pick get a kit as usual) or the round results now.
- `/mcm skip` ends the current round now with no winner and no round/draw points (eliminations already scored stay) and goes to the next round; after the last round the game ends normally.
- `/mcm map <id>` (`greenhouse`, `toyworkshop`, `skilodge`) sets the next round's arena theme (one-shot; round 1 is already built during the intro).
- `/mcm fast on`: intro, kit timer and round-results wait are at most 1 s. Building the arenas and the fight are not shortened (the fight has no limit by default; use `/mcm skip`).
