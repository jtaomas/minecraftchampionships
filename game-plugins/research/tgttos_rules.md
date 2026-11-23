# To Get To The Other Side (TGTTOS / TGTTOSAWAF) — rules research

Prepared 2026-11-24 for a private, non-commercial fan-server plugin. Everything is paraphrased.
Confidence: **H** = stated plainly by a primary wiki page or two sources / seen clearly on video; **M** = one secondary source, or inferred from video frames; **L** = inferred or uncertain (listed again under Open questions).

## Sources used

| ID | URL | Notes |
|----|-----|-------|
| S1 | https://mcchampionship.fandom.com/wiki/TGTTOSAWAF | Read with the built-in browser. Official description, rules, karma, coins, remixes, map list. |
| S2 | https://mcchampionship.wiki.gg/wiki/TGTTOSAWAF (and `?action=raw`) | Mirror of S1 plus the full **History** table (version changes), read from the raw wikitext. |
| S3 | https://mcchampionship.wiki.gg/wiki/<Map>_(TGTTOS) `?action=raw` | Map intros for about 45 maps (Pits, Walls, Glide, Basins, Skydive, Launcher, TSF, Industry, Siege, Passing, Shallow Water, Ice Walker, ...). |
| S4 | https://mccisland.wiki/TGTTOS | MCC Island version: 3 rounds × 90 s, modifiers, Time Skip, karma, plobby settings, full update history. |
| S5 | https://mcchampionship.com/pages/mcci-tgttos/ | Official Noxcrew page (MCCI): PvP on, snowballs, build walls / break bridges, void. |
| S6 | https://mccisland.wiki/Patch_0.14.1 | Official karma patch note (speed reduction, no punching, red glow). |
| V1 | https://www.youtube.com/watch?v=NRYOGgyuAdA | "TGTTOS 1st place finishes of MCC BG" (2024, 1:30 timer era). Player POV, HUD, kit, finish. |
| V2 | https://www.youtube.com/watch?v=1RU497hAgcU | "TGTTOS 1st place finishes of Absolute Zero" (MCC AZ, current 1:40 timer). Parkour, boat and elytra kits. |
| V3 | https://www.youtube.com/watch?v=A_WVt8rI1oA | MCC 18 admin-stream clip of Cliff: pre-round sidebar, adventure mode, start crowd. |

**Blocked or empty:** web searches returned no Reddit threads or guides with kit/respawn details. dotesports returned a 404. YouTube transcripts were not reachable (no transcript button). Individual map pages on the wikis do not list kits, so the kits per map come from map intros plus video frames.

Two versions exist; keep them apart:
- **MCC event version** (our target): 6 rounds, 40 players in 10 teams of 4, coins, fans (NPCs) as goal.
- **MCC Island version**: 3 rounds, 12–16 solo players (24 per the older official page), chickens as goal, random modifiers. Use it only where the event version is undocumented.

---

## 1. Format

| Fact | Value | Conf. | Source |
|---|---|---|---|
| Rounds per game | 6, each on a different map | H | S1, S2, V1 sidebar "ROUNDS [2/6]" |
| Round length | **1:40 since MCC HT** (was 1:30 from MCC 1 up to HT; 2:00 for MCC MU only). The timer counts down at the top centre. | H | S2 history; V2 shows 01:40; V1 shows 01:30 |
| Players | 40 (sidebar "PLAYERS FINISHED: 0/40") in 10 teams of 4. Everyone races at once on the same map. | H | V1, V2 |
| MCCI round count / length | 3 × 90 s (configurable from 1–25 rounds and 30 s–20 min in private lobbies) | H | S4 |
| Map choice | Each event uses 6 different maps from a pool of about 35 current maps plus event exclusives. New maps often debut in a specific event, and festive or Halloween variants appear in matching events. No source documents a random draw; the event's map list appears to be fixed in advance by Noxcrew. | M (pool), L (selection method) | S1, S2, S3 |
| Order hint | Terra Swoop Force Tunnel 1 has been round 1 in almost every event that used it. | M | S3 (TSF appearance table) |
| Pre-round | Players stand at the start with their kit **already in the inventory**. The sidebar shows "Round begins: 00:15". The action bar shows "STARTING ROUND IN: 00:0x", and a big title "Chaos begins in" counts down 3-2-1. | H (V1 0:25, V3 0:10, 0:30) | V1, V3 |
| Start crowd | All 40 players start together in one start area and visibly overlap one another. | M | V3 0:30 |

## 2. Goal and finishing

- Cross the map, then **punch an NPC at the end** ("whack a fan"). MCC 1–3 used **chickens**. From MCC 4 the targets are player-model NPCs with fan-submitted skins. MCCI still uses chickens in a "chicken pit". **H** (S1, S2, S4)
- The NPCs stand in a pen or pit at the finish, and there are many of them, so 40 players never fight over one. **H** (V1 0:24–0:25: a pen full of NPCs)
- When you punch one: floating red **"Whack!"** text appears over the NPC, a big placement title shows (e.g. "1st"), and chat says "You whacked <fan name>! … 1st Place!". Fan names have been shown in chat since MCC 6. **H** (V1 0:25, S2 history MCC 6)
- Finished players **stay in the world at the finish pen**. Their hotbar is replaced with a "Spectate Players" item and a settings item. They are not switched to spectator game mode right away. **M** (V1 0:25)
- The sidebar shows per-round placements for you ("[2nd] [7th] [3rd] [/] …") and "PLAYERS FINISHED: n/40". **H** (V1, V2)
- On MCCI, finished players are marked with a **tick** in the top-left player list (they used to get a skull). **H** (S4 0.2.0)

## 3. Kit (what players get)

Confirmed from video (V1, V2):

| Map type | Kit seen | Conf. |
|---|---|---|
| Bridging maps (Industry, Shallow Lava, Domino, Badlands) | **64 concrete in the player's team colour**, an **iron pickaxe**, **5 snowballs** | H (V1 0:02, 0:30, 1:28, 1:54) |
| Parkour maps (Ice Mountain, Frozen Sprint) | **Snowballs only** (5) | H (V2 0:03, 0:45) |
| Boat maps (Boats, Slippery Slope) | Player is **already seated in a boat** before GO ("Press Left Shift to dismount"), plus 5 snowballs | H (V2 1:25, 2:05) |
| Elytra map (TSF Frozen Tunnel) | Firework rockets, plus a second stack of 3 (likely fireworks or snowballs). The elytra is worn. | M (V2 3:20) |
| Cliff (classic) | No blocks. Players are in **Adventure** mode, and the map is pure parkour plus punching. | M (V3 0:10 chat "game mode should've been changed to Adventure") |

From map descriptions (S3), not seen on video:

- **Glide**: building upward, then flying with an elytra. **H** (S3)
- **Basins**: a **Riptide trident** is "the only means" of crossing. **Golf**: tridents across a large map. **H** (S3)
- **Launcher**, **Skydive (Remix)**: **rocket launchers** from Rocket Spleef instead of blocks. **H** (S3)
- **Passing**: everyone gets **Potion of Leaping II** before the round, to bridge upward faster. **H** (S3)
- **Ice Walker**: **Frost Walker boots** plus ice slides. **Shallow Water**: **concrete powder** that hardens when it touches water. **Spectrum**: speed boost pads, ice slides, jump boost. **Shulker**: shulker levitation. **TSF Frozen Tunnel**: fireworks and updrafts, explosions turned off. **H** (S3)
- **Version change:** wool + shears were replaced by **concrete + pickaxe in MCC 26**. **H** (S2 history)

## 4. Movement, PvP, hazards

| Fact | Value | Conf. | Source |
|---|---|---|---|
| Punching | Players may punch each other freely. Punches do knockback only; the game is about knocking people off, not killing them. | H | S1, S5 |
| Snowballs | Given for sabotage (knockback). | H | S5, V1, V2 |
| Breaking | Players can break other players' bridges (official: "build walls or break bridges"). Industry is the exception where some **map concrete on the walls is breakable**. Elsewhere map blocks are presumably protected. | H (bridges), H (Industry), M (map protection) | S5, S3 |
| Placing | Free placing of the given concrete. No documented height or area limit. | M | V1 |
| Collision | Players overlap freely, which suggests no player collision. | M | V3 0:30, V1 1:10 (players stacked on a single pillar) |
| Void | Falling into the void is not elimination: you go back and keep racing. MCCI has the "Ultimate Comeback" badge (1st after a void fall) and a "One Life" modifier that makes a void fall a DNF, which implies the normal rule is respawn. The red-light modifier also "sends you back to the start". | H (not elimination), M (respawn at start of map) | S4 |
| Death feedback | Each void death is broadcast in chat with a skull icon and a chicken pun ("chickened out!", "slipped while bridging", "got to the wrong side!", "was yeeted out of existence", "didn't stop, look and listen before crossing"). | H | V1 1:10, 1:28, 1:40 |
| Health | The health bar is visible, but no damage is seen. Fall damage is not documented. | L | V1 |
| Karma (since MCC 31) | Players who get punched a lot take **less** knockback. Players who punch a lot take **more** knockback, emit **red particles** or **glow red**, get a **speed reduction**, and eventually **cannot punch** for a while. Karma decays back to neutral over time. Punching the same player raises karma faster, and punching a high-karma player raises none. The HUD shows "KARMA:" with a face icon at the top right. | H | S1, S2, S6, V1/V2 HUD |
| Map hazards | Lava (Shallow Lava 1-deep pits, Badlands), cobwebs, guardians and water (Walls), piston traps and **skeletons** (Siege), honey and slime (Beehive), explosions (TSF), moving mobs (Zoo). | H | S3 |

## 5. Scoring (current, since MCC R; coins per player)

| Component | Value | Conf. |
|---|---|---|
| Placement coins | 1st = **40**, then **1 less per place** (2nd 39 … 40th 1) | H (S1, S2) |
| Top-10 bonus | 1st 80, 2nd 75, 3rd 70, 4th 65, 5th 60, 6th 55, 7th 50, 8th 45, 9th 40, 10th 35 (−5 per place) | H |
| Team completion bonus | The first 4 teams whose **whole team** finishes: 100 / 75 / 50 / 25, **shared among the team members** (so 25 / 18.75 / 12.5 / 6.25 each for 4 players) | H |
| Did not finish | Nothing documented, so presumably 0 | M |
| Game multiplier | The event multiplier (x1.0 … x3.0) applies to all coins | H (V1 x1.0, V2 x2.0) — event-level, out of scope |

History: MCC 1–5 paid 80 −2 per place. MCC 6 added 100 shared for the first full team. MCC 17 brought 40 −1, bonuses of 60/45/30/15 for the top 4, and team bonuses of 250/200/140/80. MCC R introduced the current table. **H** (S2)
MCCI awards XP by placement instead: 150/130/120/110/100/80/70/60, then 50. **H** (S4)

## 6. Round end

- The round ends when the timer runs out or when everyone has finished. **M** (S1 implies the timer; MCCI documents both)
- MCCI **Time Skip**: when 80% of players have finished, the timer jumps to 20 s remaining. **H** for MCCI (S4 0.3.6); **L** for the MCC event.
- After the round there is a recap with the finish times (MCCI shows milliseconds) and the next map's start. On MCCI, 0.6.1 shortened the wait between rounds by 7 s. **M** (S4)

## 7. HUD and feedback (V1/V2)

- Top centre: round countdown (01:40 → 00:00). Pre-round: "STARTING ROUND IN" bar and the "Chaos begins in 3-2-1" title.
- Top left: own team's heads "VS" the other teams' icons; heads mark who has finished.
- Top right: "KARMA:" face indicator.
- Sidebar: event/game title, MAP name, ROUNDS n/6, per-round placements, PLAYERS FINISHED n/40, game coins by team (multiplier), your coins.
- Chat: death puns; "You whacked <fan>! nth Place!"; "Whack!" text above the NPC.

## 8. Map types (categories of crossing)

| Type | Examples | Gameplay |
|---|---|---|
| Speed-bridging over void | Pits, Walls (cobwebs, guardians, water), Industry (side-bridging, breakable wall concrete), Domino, Badlands, Shallow Lava (lava pits), Passing (Leaping II) | Concrete + pickaxe + snowballs; sabotage by breaking bridges |
| Vertical build then fly | Glide | Tower up, then elytra glide |
| Elytra tunnel | Terra Swoop Force tunnels 1–3, Frozen Tunnel | Elytra flight through a tunnel; explosions on contact (fireworks/updrafts in Frozen) |
| Downward / dropper | Skydive (land on a target platform while bridging and towering), Skyscraper (dropper-style), Spiral Climb (climb, then drop into a hole) | Aim the fall |
| Trident | Basins, Golf | Riptide trident only |
| Boat | Boats, Slippery Slope | Seated in a boat on ice |
| Rocket launcher | Launcher, Skydive Remix | Launchers instead of blocks |
| Pure parkour / run | Cliff (constant punching), Train, Ice Mountain, Frozen Sprint, Foot Race, Zoo, Blankorp, Las Vegas, Lunchtime, Doors, Breakdown (plus bridging and breaking), Maze, Mansion | No blocks; snowballs and punches |
| Mechanic-gimmick | Beehive (honey/slime), Water Park (swimming, bubble elevators), Siege (pistons, skeletons), Shallow Water (concrete powder), Ice Walker (Frost Walker), Shulker (levitation), Spectrum (speed and jump pads) | |
| Per-team start | Skyblockle map: a different start point per team | |

## 9. Special mechanics and remixes

- Remixes: **To Get to the Other Stack** (Pride events: right-click a player to stack on them; stacked players cannot place blocks), **Reversed** (MCC R3: run from the finish to the start), **LEGO Chicken** (MCC PG: ride a chicken, slow falling, no jumping). **H** (S1, S2)
- MCCI modifiers, used about 40% of rounds with at least 1 per game: Crack Shot (Punch III bow, regenerating arrows), Crumbling Blocks, Double Time (fast movement, 45 s), Early Birds (chickens along the course), One Life, Red Light Green Light (2 min), Slap Stick (Knockback III stick), TNT Time. **H** (S4) — optional, MCCI-only.
- MCC PARTY 2: some fans replaced with a "Mega Chicken" hidden bonus objective. **H** (S1)
- To The Dome!: the timer reads "GL:HF" instead of numbers. Some maps have their own music. **H** (S3)

## 10. Version changes (MCC event)

MCC 1 debut: 6 maps, chickens, 80 −2 coins → MCC 4: fan NPCs, +2 maps → MCC 6: renamed TGTTOSAWAF, fan names in chat, first-team bonus → MCC 10/12/14: Launcher, TSF and 5 maps → MCC 17: new scoring, Shallow Lava → MCC R: current scoring → MCC 22: Badlands → **MCC 26: wool/shears → concrete/pickaxe** → MCC 28/29: Passing, Train, Spiral Climb, Skyscraper → **MCC 31: karma** → TR, 4KO, EC, TRSD, BC: more maps → MCC MU: 2:00 for that event only, 4 maps → **MCC HT: 1:30 → 1:40** → MCC PG: 6 maps plus LEGO Chicken → MCC AZ: Frozen Sprint, Ice Mountain, Slippery Slope, Frozen Tunnel. (S2, S3)

---

## 11. Gaps: our README vs the real game

| # | Our README | Real game | Severity |
|---|---|---|---|
| G1 | Round 90 s | **100 s** (since MCC HT); 90 s was the older value and is still the MCCI value | Low (make it config, default 100) |
| G2 | **Shears + wool** | **Pickaxe + team-coloured concrete** since MCC 26 (LESSONS.md already flags this) | **High** |
| G3 | Kits vary: 8/12/16 snowballs, eggs, 32 or 64 wool | Bridging maps: **64 team concrete + pickaxe + 5 snowballs**. Parkour maps: **snowballs only**. No eggs seen. | High |
| G4 | Kit given at GO | Kit is **in the inventory during the pre-round countdown** (players hold it before the start) | Medium |
| G5 | 5 s countdown behind a glass gate | About 15 s pre-round ("Round begins" in the sidebar) with a **"Chaos begins in 3-2-1" title**. Whether there is a physical gate is not documented. | Medium |
| G6 | Finishers → **spectator game mode** | Finishers **stay at the finish pen** with a "Spectate Players" hotbar item. They also get a "Whack!" effect, a placement title and the chat line "You whacked <name>! nth place". | Medium (LESSONS says to use spectator mode; owner decides) |
| G7 | Scoring 12 → 3 by 1, then 2; team bonus 8/5/3 per member for the first 3 teams | **40 − (place−1)**, a **top-10 bonus of 80 → 35 (−5)**, and a **team bonus of 100/75/50/25 for the first 4 full teams, split among members**. Scale it down if needed, but keep the shape (top-10 bonus dominates; team bonus is small and shared). | **High** |
| G8 | "No karma/anti-spam on punching" | A **karma system** exists (knockback scaling, red glow, slowness, punch lock-out, decay) | Medium |
| G9 | No mention of player list / heads, "PLAYERS FINISHED n/40", per-round placement list, map name, karma indicator | All present in the real HUD | Medium |
| G10 | No death messages | A **chicken-pun death message** is broadcast in chat for each void fall | Low |
| G11 | No 80% time-skip | MCCI time skip (80% finished → 20 s left). Not confirmed for the event. | Low (optional config) |
| G12 | The 6 map types are all "bridge/parkour + wool" variants. There are no elytra, trident, potion or rocket maps. | The real pool's identity comes from **varied traversal kits**: elytra (Glide/TSF), riptide trident (Basins/Golf), boats, Leaping II, Frost Walker, honey/slime, pure parkour in Adventure mode. Our "Boat Crossing" is right; "Mob Gauntlet" zombies loosely match Siege's skeletons and Zoo's mobs. | Medium–High (consider swapping 1–2 maps for an elytra map and a trident map) |
| G13 | Lava on Lava Field → back to spawn with a fresh kit | Real lava behaviour is undocumented (see open questions). "Fresh kit on respawn" is not documented either. | Unknown — keep, mark as default |
| G14 | Everyone in Survival on every map | Parkour-only maps use **Adventure** mode (V3) | Low |
| G15 | Goal: chickens | Event: fan NPCs (players). MCC 1–3 and MCCI: chickens. **Chickens are fine** and avoid using real skins. Make sure there are **many** of them in a pen. | OK |
| G16 | Can only break player-placed wool | Matches the real rule (break others' bridges). Optionally add some breakable map blocks (Industry). | OK |
| G17 | Punches, snowballs and eggs do no damage but knock back | Matches the documented intent. Drop the eggs. | OK |
| G18 | Round ends when all finish or time runs out | Matches | OK |

## 12. Draft acceptance checklist

1. A game has exactly 6 rounds, each on a different map. The sidebar shows "Round n/6" and the map name.
2. Each round lasts 100 s by default (config), shown as a top-centre countdown.
3. Before each round there is a pre-round of about 15 s (config) with a visible countdown and a "3-2-1" title. Players cannot leave the start area until GO.
4. Players already hold their kit during the pre-round.
5. On bridging maps the kit is exactly 64 concrete in the player's team colour, 1 pickaxe and 5 snowballs. There is no wool and no shears.
6. On parkour-only maps the kit is snowballs only, and players cannot place or break blocks.
7. On the boat map every player starts seated in a boat when GO happens.
8. Players can break concrete placed by any player with the pickaxe. Map blocks cannot be broken, except blocks a map marks as breakable.
9. Punching or snowballing another player deals no damage but applies knockback.
10. Players do not push each other (no collision): 40 players can stand in the start area.
11. Falling below the map's void level puts the player back at that map's start within 1 s. They are not eliminated, keep racing, and a pun death message is broadcast in chat.
12. Punching any goal chicken at the finish ends that player's round. They get their placement as a title, a "Whack!" effect and a chat line with the placement.
13. A finished player cannot punch or affect players still racing. They are moved to a spectating state.
14. Placement coins: 1st gets 40 and each later place 1 less. Top-10 bonus 80, 75 … 35 is added. Players who do not finish get 0. (Values in config.)
15. The first 4 teams whose online members have all finished get 100/75/50/25 coins, split evenly among the members.
16. The round ends when every player has finished or the timer reaches 0, whichever comes first.
17. The sidebar shows "Players finished: n/N" and updates within 1 s of each finish.
18. The sidebar shows the player's own placement for each round played so far.
19. Karma: a player who punches others repeatedly within a short window takes more knockback, glows red, and after the threshold cannot punch for N seconds. Karma decays over time. (Thresholds are defaults, not researched.)
20. A player who is punched repeatedly takes less knockback than one who is not.
21. All player-placed blocks are removed between rounds and at game end.
22. At least one map provides elytra-based crossing and one provides riptide-trident crossing (if the owner accepts G12).
23. Fall damage, fire and lava never kill a player outright. Lava contact is handled as a void fall (default, not researched).

## 13. Open questions (not answered by sources)

- **Respawn point after a void fall:** always the map start, or checkpoints on long maps (TSF)? MCCI's "mid-air spawn point" for One Life hints at special spawn points. Default: the map start.
- **Inventory on respawn:** are blocks or snowballs refilled? Default: refill to the starting kit.
- **Lava:** is touching lava a respawn, or burning damage? Default: respawn.
- **Pickaxe tier and break speed, snowball regeneration:** not documented (defaults).
- **Is there a physical gate at the start, or a freeze?** Undocumented.
- **Event time skip at 80% finished:** only documented for MCCI.
- **Exact pre-round length:** 15 s seen on one admin stream (MCC 18). It may vary.
