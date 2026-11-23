# Battle Box v2 (McmBattleBox)

Game id: `battlebox` (`/mcm play battlebox`). Display name: **Battle Box**. Core API 3.2.0, Paper 1.21.1.

## How it plays
- **Sides:** each tournament team is a side. Solo players are grouped into squads of about 4 (at least two squads if nobody is on a team). If everyone is on one team, it is split in two. One player alone plays 3 practice rounds against an empty side (no points).
- **Schedule:** round robin, up to 9 rounds (`max-rounds`), at least 3 (with 2-3 sides the pairings repeat). Odd number of sides: one side has a bye each round (+4 each).
- **Map rotation:** every round picks a new random arena theme (never the same twice in a row); all arena copies that round use it. `rotate-maps-each-round: false` uses one random theme for the whole game; `force-theme` pins one theme.
- **Each round:**
  1. **Kit select (20 s in round 1, then 15 s):** players wait in their side's spawn room behind team-coloured glass gates. The kit menu (chest GUI) opens by itself and can be reopened with the star in the hotbar. Picking a kit puts its items in your inventory straight away (they can't be used yet). Each kit can be taken once per side (twice for 6-10 players, ...). Teammates' picks show in the action bar and the menu. Anyone without a pick gets a random free kit.
  2. **Fight (60 s):** the gates drop (the glass vanishes with block particles, glass-break + piston + bell + horn sting). One life; when you die you become a spectator: you are put high above your own half looking down at the centre and can fly around freely, but you are teleported back if you leave your arena's box. No friendly fire, no dropping, picking up or sharing items, arrows can't be picked up.
  3. **Results (5 s):** win/lose/draw titles, fireworks in the winner's colour over their arena, a results line per matchup in chat. Then everyone goes to the holding platform while the next arenas are built (under a second).
- **Centre:** a 3x3 pad on a two-step dais (walkable stairs on all four sides) framed with yellow/black hazard stripes. The 9 cells **start filled with neutral blocks** (white concrete, or white wool in wool mode; light grey if a side is white). You must break a neutral or enemy block first and can only place your own team's concrete (default, iron pickaxe) or wool (`centre-block: wool`, shears) into an empty centre cell - nowhere else. You can't break your own colour. Centre blocks mine at normal vanilla speed with the plain centre tool (white concrete with an iron pickaxe about 0.45 s, wool with shears about 0.25 s) and drop nothing.
- **Winning a round:**
  - All 9 cells in your colour: instant win.
  - Time-out: more centre cells wins.
  - Time-out with equal cells: if exactly one side still has players alive, that side wins ("last side standing"); otherwise draw.
  - Wiping out the other side does **not** end the round: the survivors still have to fill the centre (or win at time-out). If both sides are out, the round ends at once on centre count.

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
- Boss bar: timer for the current phase.
- Sidebar: round x/9 and theme, phase, the top matchups with their centre score, top fighter.
- Action bar: during kit select, your kit and your teammates' picks. During the fight: opponent, your eliminations, and the centre count as coloured squares (yours vs theirs).

## Arenas
Three themes, generated in pure Java (`arena/` package, no Bukkit), rebuilt for every match each round. Every
arena is a 35x35 floor (fixed size; the layouts are hand-placed) inside painted walls 13 high with a frieze. The two
teams' halves are a 180-degree rotation of each other (fair); inside one half, left and right are deliberately
different. Each theme has its own structural idea (see `..esearchattlebox_layouts.md` for the research behind it):

| Theme | Archetype | Spawn building and exits | Left flank (from your spawn) | Right flank | High ground |
|---|---|---|---|---|---|
| **Greenhouse Conservatory** (`greenhouse`) | **Enclosed middle**: the centre is inside a glass Palm Hall (17x17) with one door on each of its four sides; a solid living wall in the middle of each front wall | Potting House: west door, east door, indoor stair to a roof balcony (drop off its front or take the footbridge) | serpentine hedge maze (3-high hedges, 2-wide paths, 4 bends) ending at the hall's side door and in the enemy's lawn | open lawn, almost no cover | Palm Tower: glass mid floor via the footbridge, ladder to an exposed deck 8 up (sees into the hall through the glass roof) |
| **Toy Workshop** (`toyworkshop`) | **Open middle + bridge level**: a toy-train viaduct (deck 7 up, wall to wall) crosses straight over the open centre, glass over the dais, arched piers at x +-8 / +-15 | Packing Room, no front door: a long low play tunnel (left) and a wide open porch (right) | tight, roofed shelf corridor along the side wall (one doorway + two windows into the middle) | open foam play-mat with low toy blocks and the viaduct stair | the viaduct (stair on the right, ladder on the left pier); a parked carriage on each side of the glass is the only cover up there |
| **Ski Lodge** (`skilodge`) | **Divided halves**: a frozen ravine (3 deep) runs wall to wall; the centre sits on a mid-river island; lower ice level with water patches, floes, a frozen-in boat and frozen waterfalls at both ends | Boot Room + wide porch: a chimney screen fills the porch front, so you leave by its two corners, plus a side door into the alley by the lodge | big two-storey lodge (indoor stair, upper room, balcony over the ravine) - the two lodges sit on one diagonal | open slope with the ski-lift tower and the stair down onto the ice - the two slopes sit on the other diagonal | lodge balcony; ski-lift platform 8 up (ladder) |

How you reach the centre: Greenhouse - through one of the hall doors; Toy Workshop - straight across the open floor
or under/over the viaduct; Ski Lodge - over your bridge (right of centre) or down onto the ice and up the island stair
(left of centre).

Offline checks (`tools/render.sh`, numbers from the current build):

| Theme | spawn exit -> enemy spawn exit | spawn exit -> far half hidden | own half -> enemy spawn exit blind | back third -> enemy back third blocked | centre visible from mid-range ground / high spots | route check |
|---|---|---|---|---|---|---|
| Greenhouse | 100% of 3600 rays blocked | 100% of 587 spots | 100% of 587 | 89.7% | 36% / 23% (it is indoors) | 3/3 exits, 13/13 lane waypoints, sealed |
| Toy Workshop | 100% | 100% of 742 | 100% of 742 | 84.9% | 67% / 75% (open middle) | 2/2 exits, 11/11 waypoints, sealed |
| Ski Lodge | 100% | 100% of 519 | 100% of 519 | 78.9% | 60% / 70% (open river) | 3/3 exits, 11/11 waypoints, sealed |

- **Sightlines** (`tools/Sightlines.java`, eye height 1.62): glass, panes, bars, fences, walls, leaves and small props
  count as see-through, the centre cells and gates as empty; only spots a player can actually reach (from the route
  check) are tested.
- **Route check** (`tools/Routes.java`): flood-fill of where a player can walk from each spawn (walk; step up stairs
  and slabs; jump up 1 block with headroom; drop any height; climb ladders; gate glass open). Each theme registers its
  spawn building's intended exits and a waypoint in every lane. It fails the render if the centre or any waypoint is
  unreachable, if any exit does not lead to the centre on its own (all the other exits closed), or if the centre can
  still be reached with every exit closed (an opening nobody intended).
- **Staircases:** every flight is placed by one helper (`ArenaTheme.staircase`) and walked bottom to top
  (rise <= 1, 2 headroom, flush landing). **Connections:** `Grid.postProcess` recomputes fence / pane / bars / wall /
  gate states and drops carpets that are not on a full block.

World `mcc_battlebox`: arena copies at x = slot * 100, y 64, z 0; holding platform at (-200, 110, 0); admin preview arena at (0, 64, -400).

## Admin commands (`battlebox.admin`, op by default)
- `/bb arena <greenhouse|toyworkshop|skilodge>` builds one arena copy at the preview spot and teleports you in.
- `/bb kits [theme]` opens the kit menu outside a game. Click a kit to see its real items.
- `/bb theme <id|off>` forces a theme for every round (saved to `force-theme`, applies from the next game).
- `/bb dump` dumps the preview arena to `plugins/McmBattleBox/dumps/` for `render_preview.py`.
- `/bb themes` lists the themes and the current settings.

## Config (`config.yml`)
`centre-block` (concrete|wool), `centre-tool-efficiency`, `rotate-maps-each-round`, `force-theme`, `first-kit-seconds`, `kit-seconds`, `fight-seconds`, `end-seconds`, `max-rounds`.

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
- `/mcm fast on`: intro, kit timer and round-results wait are at most 1 s. Building the arenas and the 60 s fight are not shortened.
