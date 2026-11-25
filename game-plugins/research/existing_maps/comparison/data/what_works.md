# What works — adopt / adapt / reject

Cross-map synthesis (A13). Read-only: this file judges the ten downloads, it does not build or port them.
Evidence: **H** = directly observed in the report's block/command/frame evidence; **M** = inference from two
observed facts; **L** = design judgement or unresolved.

**The one-line rule:** adopt the *grammar* (what the player reads and feels), adapt the *numbers* to a
40-player runtime, reject every *implementation* that binds game state to a command block, a world spawn,
or a resource pack. Original geometry only.

---

## 0. Ranked shortlist — the ten qualities worth taking

| # | Quality | Best source | Verdict | Why it is worth taking | Evidence |
|---:|---|---|---|---|---|
| 1 | **Chevron flow field** — the floor itself shows direction | Clouds 2 | **ADOPT** | One texture, one facing rule, readable at sprint speed without a HUD. The map proves direction from its own block `facing`, not from a video. | `../clouds2/README.md` §3, `../clouds2/renders/route_labeled.png`; H |
| 2 | **One landmark per act** — a saturated silhouette that is the next target | Clouds 2 + TGTTOS Badlands | **ADOPT** | Five set pieces in one lap (planet, tunnel, crater, rainbow, castle); Badlands does the same at a smaller scale (billboard → mine → rail → cave → mesa → MCC wall). | `../clouds2/README.md` §10; `../tgttos_badlands/README.md` §7.2; H/M |
| 3 | **Pad colour grammar** — block colour = effect, consistently *within a map* | Clouds 2 | **ADOPT, re-skin** | Clouds 2: yellow glazed = checkpoint/trident, red-family glazed = jump+speed, lime/orange shulker = jump/mega. Yeti: yellow = speed+spawn, pink/red/magenta = jump+speed, lodestone = elytra. The player learns one map's language in seconds — but the same block does not mean the same thing on the next map, so fix a single event-wide language. | `../clouds2/README.md` §4.4; `../yeti_set_go/README.md` §3.1; H |
| 4 | **Compact lap** — a full race in 28.5k blocks | Yeti Set Go | **ADOPT** | Proves a premium lap does not need a 600-block-long city. Two command clusters, 0.1 MB pack, one elytra command. | `../yeti_set_go/README.md` §6, §TL;DR; H |
| 5 | **Tuned vertical lift** — pad strength matched to the next ring/gate height | Space Race | **ADOPT the maths, reject the trigger** | Red levitation ≈48 blocks, orange ≈71 blocks, matched to ring heights in four independent pairs. Predictable and cheap. | `../space_race/README.md` F12; M |
| 6 | **Road grammar + night light pools** — surface hierarchy and light on the racing line | City Marathon | **ADAPT** | Gray asphalt / white dash / yellow double line / one-block curb is instantly legible; 2,182 invisible light blocks are placed on the route furniture (H placement; M perceived). Too much city for a 2-minute lap, so compress it. | `../city_marathon/README.md` §9, §10; H/M |
| 7 | **Start/finish framing** — arch, checkerboard, gate posts, countdown theatre | Clouds 2 + City + Parkour Tag | **ADAPT** | Clouds 2's hedge arch/checkerboard apron and City's four posts read as a gate; Parkour Tag's red→yellow→lime door fills are the best wordless countdown in the set. | `../clouds2/README.md` §3; `../minigames/parkour_tag/README.md` §4.4; H |
| 8 | **Layered containment** — visible edge, invisible rail, kill plane, respawn | Clouds 2 + TGTTOS pen + Sky Battle envelope | **ADAPT** | Clouds 2 uses 3,130 barriers plus a kill plane; TGTTOS uses a visible frame around an invisible gate; Sky Battle wraps the whole arena. Three different layers, one principle. | `../clouds2/README.md` §10; `../tgttos_badlands/README.md` §2; `../minigames/sky_battle/README.md` §2.7; H |
| 9 | **Finish as a place** — a wall, ledge, banner or pool, not an invisible trigger | TGTTOS Badlands | **ADOPT the composition** | The 45-long MCC wall + four towers + chicken ledge is visible from the mesa. A finish line should be a destination. | `../tgttos_badlands/README.md` §7.2; H |
| 10 | **Team identity without teams racing** — colour pods, canopies, traffic-light cages | Sky Battle | **ADAPT** | Four gift-box pods give instant side identity and a private start. For Ace Race, use team-coloured start rows and tab colours, not four separate spawn islands. | `../minigames/sky_battle/README.md` §8.3; H/M |

---

## 1. Route legibility

**What works.** Clouds 2 is the benchmark: the yellow-glazed floor arrows are a genuine flow field, sampled
at 32×32 cells and consistent around the loop (45/45 arrows at z −32, 50/50 at z −64, 30/30 at z 192)
(`../clouds2/README.md` §3; H). The player is never asked to read text or a minimap. Space Race's
chain-of-islands also works at distance: from each island the next is the only large object in view
(`../space_race/README.md` §5; H/M). Yeti's material changes with elevation and its pads are the only
saturated colours (`../yeti_set_go/README.md` §TL;DR; M).

**What fails.** City Marathon has excellent road grammar but no arrows; its paint is symmetric and most
legs have a parallel pad-lined fork, so it reads as a city and only partly as a course
(`../city_marathon/README.md` §10; H/M). Space Race reads as sculpture, not race: two cavern exits with no
marker, three contradictory start cues, and one pad as the only link between island chains
(`../space_race/README.md` F5/F10/F11; H). Clouds 2 itself has low-contrast white-on-white and grey-on-grey
patches, and the video's freecam shots overstate first-person readability
(`../video_clouds2/data/route_readability.csv`; H/M).

**Adopt:** a primary geometric channel (one-way drops, water, narrow lips) plus a secondary painted channel
(chevrons on the racing line) plus a tertiary runtime channel (HUD checkpoint/position). Never rely on text
alone.
**Adapt:** put chevrons only where a decision is possible; do not carpet every surface. Clouds 2's density is
right for a 150k-block sprawl, too noisy for a compact lap.
**Reject:** symmetric road paint as the only cue (City), decorative colour that is also a trigger (Space F7),
and text/signage as the primary channel.

---

## 2. Arrows and signage

**What works.** Painted floor arrows at decision points — Clouds 2's chevrons and Badlands' black/green
arrows at 1:38/2:56/3:23 (`../video_clouds2/data/route_readability.csv`; H). A good arrow points at the *next
pad or gate*, not at the void.
**What fails.** Sign walls, and arrows that contradict the spawn. Clouds 2 ships 285 signs, of which 278 are
six copies of the author's pad legend and only 7 are player-facing (`../clouds2/README.md` §5; H). Space Race
has 21 blank leaderboard signs, one course sign pointing the wrong way, and a runway arrow that points south
while the player spawns facing north (`../space_race/README.md` F5/F8; H). City has seven signs, mostly
"NO ENTRY"/"D0CT WH0" jokes (`../city_marathon/README.md` §8; H). TGTTOS puts the click event on an empty
sign line, not the visible "Start" (`../tgttos_badlands/README.md` §6; H; `../tgttos_industry/README.md` §3.4; H).
**Adopt:** arrows at forks, pad mouths and gate approaches; one arrow language for the whole event.
**Adapt:** make the arrow a block/texture in the map palette, not a floating entity or a sign. At 40 players
and 6+ blocks/second, only shape and colour survive.
**Reject:** legend walls, blank leaderboards, sign-click starts, and "decorative" signs that look
interactive. They add maintenance and lie to the player.

---

## 3. Landmark sequencing

**What works.** Clouds 2 gives each act one dominant silhouette (planet waterfall → glass tunnel → crater
village → elytra ring → rainbow → castle → coral → finish), and the palette changes with it
(`../clouds2/README.md` §10; H). Badlands does the same with a mine gate, black rail ridge, cave mouth, mesa
drop and MCC finish wall (`../tgttos_badlands/README.md` §7.2; H). City's station/train is a genuine hero
object, but the concourse around it is empty (`../city_marathon/README.md` §10; H).
**Adopt:** sequence landmarks at 100–150-block spacing, and make the next landmark visible from the previous
one. Space Race's chain spacing proves the range (`../space_race/README.md` §6 item 8; M).
**Adapt:** a landmark must also be a *decision*: the route should pass through or around it, not merely
beside it. Clouds 2's castle threads the route; City's station is crossed on an unmarked floor.
**Reject:** two competing exits from one room (Space F10), a landmark that hides its own exit, and decoration
noise seen in the video (giant creeper, floating text signs, checkerboard walls — video-only capture, not all
confirmed in the downloaded world) that competes with the route
(`../video_clouds2/data/route_readability.csv`; H video, M world).

---

## 4. Pad placement

**What works.** Pads sit on the racing line, use one saturated palette, and re-apply every tick while stood
on (`../clouds2/README.md` §4.4; H). City's rhythm — a pad every ~20–30 blocks — keeps a long avenue alive
(`../city_marathon/README.md` §4; H). Yeti proves the minimum viable grammar: yellow = speed, pink/red/
magenta = jump+speed, lime = jump, lodestone = elytra (`../yeti_set_go/README.md` §3.1; H).
**Adopt:** one block family per effect within a map, pads on the line, a visible landing target for every launch.
**Adapt:** make pad effects per-player and data-driven (effect id, amplifier, duration, cooldown), not a
per-player condition with an `@a` action. Put a cooldown on repeat triggers so a 40-player pile-up cannot stack 20 effects.
**Reject:** (a) pads off the race line or on unreachable roofs (City's six rooftop elytra pads; H);
(b) material-as-trigger, where the island's own stone/bedrock/glass is also the kill or clear trigger
(Space F7; H); (c) `@a` targeting (Clouds 2/City/Yeti pads; Space elytra clear and kill planes; H); (d) pads
that clear the whole inventory (Clouds 2 calcite; H).

---

## 5. Elytra and trident sections

**What works.** Space Race's *idea* is the strongest in the set: a cyan elytra ring is both the visual target
and the grant, and the levitation lifts are tuned to ring heights (48/71 blocks, four independent matches;
`../space_race/README.md` F12; M). Yeti's lodestone-on / stone-off swap is a clean physical grammar
(`../yeti_set_go/README.md` §3.3; H). Badlands/Industry's one-block offhand bridge is a different mechanic,
but it proves that a single readable resource can carry a whole section (`../tgttos_badlands/README.md` §7.2; H).
**What fails, decisively.** Space Race's grant tests `~ ~ ~-1…-7` — a horizontal offset — so it cannot fire in
flight; elytra is effectively unobtainable (`../space_race/README.md` F1; H). City's six rooftop pads are
unreachable and five have the wrong block under them, so elytra is vestigial (`../city_marathon/README.md` §5;
H). Yeti's single elytra command is 152 m from the lodge and 177 m from the start, so only the station works
at simulation distance 6 (`../yeti_set_go/README.md` §3.3; H/M). Clouds 2's elytra works but the flight legs
sit ~160 blocks from the nearest sensor bank, so the kill plane is inactive for most of the ring flight at
sim 6 (`../clouds2/README.md` §4.8; H/M).
**Adopt:** ring/gate as landmark + grant; one-way glide corridors with a visible landing; tuned lift maths;
a clear elytra-off surface *after* the landing.
**Adapt:** grant by a **volume check** or a plugin region/advancement, never by the block under the player.
Provide height (launch pad, levitation, or a drop) and a firework/boost source; validate with a real client,
because a bot cannot fly elytra (`../server_import/README.md` §1; `game-plugins/AceRace/SPEC.md` §10; H).
**Reject:** block-offset axis checks (Space), unreachable roof pads (City), one distant global command
(Yeti), elytra with no height source (City), and any section that can only be crossed in creative.

---

## 6. Start and finish framing

**What works.** Clouds 2's start pen is a garden hedge, a checkered banner, a button, a 9-second title
countdown and a trident; the finish returns to the same checkered apron and finish corridor
(`../clouds2/README.md` §3; `../video_clouds2/README.md` §3.1; H — the video is a different recreation, so
use it for the composition, not the palette). City's four yellow-concrete posts and
3×3 ground pad read as a gate from 50 blocks (`../city_marathon/README.md` §3.2; H). Parkour Tag's glass
doors fill black, then red → yellow → lime, with note-block hats and a bell — the best wordless countdown
(`../minigames/parkour_tag/README.md` §4.4; H).
**What fails.** City, Space and Yeti have a one-shot gate: Space's fill never re-places glass
(`../space_race/README.md` F4), Yeti's saved gate is closed and the countdown order is backwards
(`../yeti_set_go/README.md` §3.6), City's release gate is closed at save and only the centre is cleared
(`../city_marathon/README.md` §3.2). Clouds 2 has a manual reset button rather than per-round gate state
(`../clouds2/README.md` §4.3). None detects a finish; all four spam a static "You are in 1st place"
(City/Space/Yeti explicitly; Clouds 2 by A1's static actionbar, H).
**Adopt:** a start gate the player can see through, a countdown that uses the gate itself (colour/opening),
and a finish that is a physical place with a directional line.
**Adapt:** build the gate as runtime state (open/closed per round), not a one-shot `/fill`. For 40 players,
use a start pen with rows and a shared gate; release all at GO; reset the gate every round.
**Reject:** a shared button that starts/resets the whole server, a gate that never recloses, a finish with
no detection, and a hard-coded "1st place" actionbar. Those are the four most damaging behaviours in the set.

---

## 7. Palette and lighting

**What works.** Clouds 2's five palettes (cloud white, garden green, desert warm, water/ice blue, accent set)
and its 7-band terracotta rainbow prove that colour zoning can carry navigation
(`../clouds2/README.md` §7; H). City's warm masonry against cool grey infrastructure, with light blocks
placed on the pads and gate, is the most mature palette in the set (`../city_marathon/README.md` §9; H
placement, M perceived, M judgement). Yeti's snow/ice/
water/gray-concrete palette is coherent at 28.5k blocks (`../yeti_set_go/README.md` §6; H). Space's warm
orange asteroids vs cold grey tech vs cyan diamond is a clean three-way read (`../space_race/README.md` §5; H).
**Adopt:** a small base palette per biome/act, one saturated "game furniture" family, and light placed on
the racing line rather than everywhere.
**Adapt:** use vanilla blocks and original textures. The pack evidence is hostile to reuse: no licence file,
21 MB dominated by a copyrighted MCC track, an invalid `sounds.json`, missing `acerace.*` OGGs, and a
Space Race archive that is not a valid pack (`../resource_packs/README.md` §7; H). Build a small original
pack (or none) with `pack_format` for the target version.
**Reject:** shipping the downloaded packs, the 5:10 track, the 312 KB Mojang language dump, the author's PSD,
and full-texture overrides that change unrelated blocks on other maps.

---

## 8. Compactness

**What works.** Yeti Set Go: 28,533 non-air blocks, 0.1 MB pack, 37 command blocks, two clusters, a full lap
with elytra and riptide (`../yeti_set_go/README.md` §6; H). Badlands is the other end of "compact": 411k
blocks but 98% terrain, 0 command blocks, a 251×75×74 island (`../tgttos_badlands/README.md` §5; H).
**Adopt:** a 250–350-block footprint for a 2–3 minute lap; 30k–120k visible blocks; landmarks every
100–150 blocks; a single paste-friendly volume.
**Adapt:** compact does not mean flat. Yeti packs a 51-block vertical range and two route lines into 260×218;
that is the density to aim for.
**Reject:** Clouds 2's 325×608 sprawl, which forces six command banks and 10–12 chunks of simulation distance
(`../clouds2/README.md` §4.2/§4.8; H/M), and City's 559×241 city where most blocks are scenery, not course
(`../city_marathon/README.md` §2; H bounds, M proportion).

---

## 9. Skyline

**What works.** Space Race's chain of floating islands around one giant hangar gives a clean silhouette
sequence with no competing geometry (`../space_race/README.md` §5; H). City's station/train and tower row
give a real skyline, and the night lighting makes it legible (`../city_marathon/README.md` §9; H). Yeti's
engine → village → station → pool → snow chain reads as a journey, not a set of platforms
(`../yeti_set_go/README.md` §2; M).
**Adopt:** one hero silhouette above the finish/start, a secondary silhouette for each act, and a clear
height hierarchy (start low, signature high, finish low or framed).
**Adapt:** a skyline is navigation only if the player can see the next silhouette from the current one.
Space Race fails that at the cavern exits; City succeeds from the avenue.
**Reject:** several equal heroes visible at once. Clouds 2 risks this with castle + rainbow + planet in one
lap; it works there because they are sequenced, not simultaneous — a compact lap has less room for that
luxury (`../clouds2/README.md` §10; M), and a skyline that hides the route (Space's hangar hides its own exit).

---

## 10. Containment

**What works.** Three layers, each with a different job:
1. **Visible edge** — City's curbs/walls, TGTTOS's black-wool/quartz pen frame, Sky Battle's gift-box walls.
2. **Invisible rail** — Clouds 2's 3,130 barriers, Decision Dome's 140 inner-rim barriers, Parkour Tag's
   400-block barrier roof (`../decision_dome_recreation/README.md` §2.2; `../minigames/parkour_tag/README.md`
   §3.1; H).
3. **Kill/respawn** — Clouds 2's y=−60 plane, Space's void, TGTTOS's y≤−41 teleport
   (`../tgttos_pits/README.md` §2.3; H).
**Adopt:** all three, but make layer 2 match the visible geometry and layer 3 per-player.
**Adapt:** the runtime owns `playBounds`, `voidY`, `killY` and respawn; the map only supplies the surfaces.
**Reject:** a kill plane that targets `@a` (Space F13, Clouds 2 §4.5), an invisible wall 13 blocks outside
the build with no visual cue (Sky Battle §8.2 item 11; H), a 42-deep pit with no outer rail (Decision Dome
§2.2; H), and open edges with no respawn (Yeti north chain, City roofs).

---

## 11. Behaviours that must never be copied

Ranked by damage to a 40-player event:

1. **`@a` as the action target from a per-player condition** — one player's pad buffs or clears everyone
   (Clouds 2/City/Yeti). Space's levitation/speed pads correctly use `@p`, but its elytra clear and both kill
   planes use `@a` (`../clouds2/README.md` §4.5; `../city_marathon/README.md` §4;
   `../space_race/data/command_blocks.csv`; `../yeti_set_go/README.md` §3.1; H).
2. **Global `kill @a`** — Space Race's two planes; Clouds 2's N² kill selector
   (`../space_race/README.md` F13; `../clouds2/README.md` §4.5; H).
3. **`setworldspawn` as a per-player checkpoint** — global state; one player moves everyone's respawn
   (Clouds 2, City, Space, Yeti; H).
4. **Static "You are in 1st place"** — lies to 39 of 40 players (all four; H).
5. **One-shot start gate** — never recloses, so lap 2 starts open (Space, Yeti, City; H).
6. **Elytra grant by block offset / underfoot material / distant single command** — Space F1, City §5,
   Yeti §3.3 (H/M).
7. **Material-as-trigger** — the map's own palette fires effects, so colour cannot be trusted (Space F7; H).
8. **Command-block banks as the game runtime** — chunk-dependent, duplicated, and dead outside simulation
   distance (Clouds 2 §4.2/§4.8; Yeti §3.3; H/M).
9. **`gamemode adventure` with no target** — silently fails; players stay in survival and can break the
   course (City 7, Yeti 3; H). Clouds 2's `gamemode adventure @a` has a target and works.
10. **Shipping the downloaded resource packs** — no licence, copyrighted music, invalid indexes, stale
    formats (`../resource_packs/README.md` §7; H).
11. **Sign-click start / sign walls** — fragile hitboxes, unreadable at speed, and 278 of Clouds 2's 285
    signs are internal legend (H).
12. **XP level as the state machine** — any XP change breaks role/team (Sky Battle §8.2 item 7; Parkour Tag
    §4.1; H).
13. **Reset by `fill … destroy` or a global teleport** — destroys containers or resets non-finishers
    (Sky Battle §8.2 item 6; TGTTOS Pits §2.5; H).
14. **42-deep unrailed pit / buried dial** — presentation that is not playable (Decision Dome §0/§2.2; H).
