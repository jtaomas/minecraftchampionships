# Find the Button (`McmFindTheButton`)

Game id **`findthebutton`** — start with `/mcm play findthebutton`. Display name "Find the Button".
Paper 1.21.1, core API `com.mcm:MinecraftMondayCore:3.2.0`. World `mcc_findthebutton` (void world, created at plugin enable).

## How it plays
- 5 rounds (config `rounds`), each on a different map (shuffled, no repeats), 90 s each (`round-seconds`).
- Each round one **stone button** is hidden at a random spot out of that map's ~10 hand-placed hiding spots.
- Round flow: teleport to the start pad + map title card (2.5 s) → 5 s countdown, players **frozen** (no walking or
  jumping, looking around is fine) → GO → search → right-click the button → you become a **spectator** (overview
  teleport, kept inside the map's box) → round ends when everyone has found it or time runs out → 4 s results
  (where the button was, who found it) → next round. After the last round: top finders + fireworks, then `finish`.
- **Hints:** from 45 s a particle pulse + soft chime at the button every 3 s, only for players within 12 blocks;
  from 70 s within 24 blocks.
- Players are in ADVENTURE. Right-clicking the real button is never cancelled (vanilla press + click sound);
  every other block right-click is denied (no chests, doors, trapdoors, beds, pots...). No damage, hunger, block
  breaking/placing, item drops/pickups. Rooms are fully enclosed; falling out of the map returns you to the start.

## Scoring (MCC-style coins, OURS - MCC never had this game; `coins.*`, see `../COINS.md`)
- 1st finder **80** coins, each later finder 4 less, minimum **20** (80, 76, 72, ... 20). Non-finders 0.
- **Team bonus +15** to each member of a side when every online member of that side found it (sides with 2+ online
  players only, once per side per round).
- The core multiplies every award by the event multiplier. All values in `config.yml` under `coins:`.

## HUD
Boss-bar timer (`ctx.setTimer`), sidebar (round x/5, map, found n/m, top 3 finders this round), action bar
("Find the button!" / your placement), `McmFx` title card, countdown, find sound + firework per finder, results titles.

## Maps (pure-Java generators, `src/main/java/.../map/`)
Interior 20x20, 6–7 high, floor at local y = 0, start pad on the south side. Spots are defined with `spot(...)`
right next to the build code they belong to.

| id | Map | Rooms | Spots |
|---|---|---|---|
| `library` | Library | book walls, reading gallery with staircase, cold fireplace, desks | 11 |
| `kitchen` | Kitchen | kitchen + walk-in pantry, bread oven, island, dining table | 10 |
| `shed` | Garden Shed | shed + hedged yard (barrier ceiling), doghouse, pergola, flower bed | 10 |
| `mineshaft` | Mineshaft | timbered gallery + back chamber with a ledge, joined by a tunnel | 10 |
| `toyroom` | Toy Room | play fort with tower, dollhouse, train tunnel, wardrobe, teddy | 10 |
| `shipcabin` | Ship Cabin | crew quarters with hatch ledge + captain's cabin | 10 |

Lessons applied: staircase helper + walk validation (`Grid.validateStairs`), connection post-pass for fences/panes/
walls (`Grid.postProcess`), carpets only on full blocks, no decoy buttons (validation fails on any stray button),
`randomTickSpeed 0` in the world (no leaf decay/grass spread), lighting via lanterns + invisible `light` blocks.

## Reachability validation (acceptance item 4)
`map/Reach.java` (pure Java) flood-fills standing positions from the start pad (walk, step up stairs/slabs, jump
+1.25 with head room, drop down) and accepts a spot only if the button cell is empty, its support is a full block,
and some reachable eye position is within 4.0 blocks (vanilla reach 4.5) with a clear line (only air / invisible
light) to the button. It runs **at plugin enable** (spots that fail are logged and never used) and offline:

```
sh tools/render.sh            # compile generators, validate all spots + staircases, dump, render previews
NORENDER=1 sh tools/render.sh # validation only (exit code 1 on any problem)
```
The last output is in `target/offline/validation.txt`: all 61 spots on 6 maps pass.

## Previews
`previews/<id>_cutaway.png` (ceiling and the two camera-side walls removed; shed roof and ship deck beams left out),
plus `previews/<id>_spots_top.png` (top-down, every hiding spot marked as a red block).

## Admin command (`findthebutton.admin`, op)
- `/ftbmap list` — maps and how many spots passed validation
- `/ftbmap build <map>` — build a map without a game; `/ftbmap tp <map>` — go to its start pad
- `/ftbmap spots <map>` — list spot coordinates; `/ftbmap dump <map>` — `McmBuild.dumpRegion` of a built map

## Build
```
JAVA_HOME=C:\Users\LeonS\Desktop\java  C:\Users\LeonS\Desktop\java-tools\maven\bin\mvn.cmd -B -q package
```
Jar: `target/McmFindTheButton.jar` (copied to `game-plugins\_built\`).

## Known limitations
- Not run on a live server yet (bot test pending). Block ids were checked against Paper's `Material` list; block
  *state* strings fall back to the plain block with a logged warning if one is wrong.
- The reach model is conservative (4-direction moves, cell-centre eyes, no crouching), so it may reject spots that
  are actually fine, but should not accept impossible ones.
- Many players share the 18-cell start pad (overlap is harmless while frozen).
- Late joiners use the core default (spectator).

## Admin testing
- `/mcm skipwait` ends the title card + countdown (search starts now), the results screen or the final screen.
- `/mcm skip` ends the current round now (nobody else can score in it; finds already made keep their points) and moves to the next map; after the last round the game ends normally.
- `/mcm map <id>` makes the next round use that map (one of the maps with valid spots). A map this game had not built is built into its own slot first; the next round waits for it.
- `/mcm fast on`: title card, countdown, results screen and final screen are at most 1 s. The 90 s search is not shortened.
