# TGTTOS / TGTTOSAWAF rules research (sonnet agent)

Confidence: H = stated by wiki text, M = inferred/partial/one source, L = guess or not found.
Main sources:
- [W] https://mcchampionship.wiki.gg/wiki/TGTTOSAWAF (main page; History table read from raw wikitext, `index.php?title=TGTTOSAWAF&action=raw`)
- [M] map pages `https://mcchampionship.wiki.gg/wiki/<Map>_(TGTTOS)` (intro paragraphs of ~33 maps)
- [U] https://mcchampionship.wiki.gg/wiki/Games_Updates_(Season_1) (per-event update notes)
- [I] https://mccisland.wiki/0.14.1 (via search snippet only: karma tweaks, MCC Island patch, not the event)
- [V1] YouTube tB8y6pw9PUo "The Fastest Completion of every MCC TGTTOSAWAF Map" (MC Events Stuff), frame at 0:40 (MCC 22 Skydive POV)
- [V2] YouTube QAjSy5K8TPg unofficial rules video (36 s map flyover only; no captions fetched, no rule info gained)

Limits: fandom returned 402 to WebFetch (wiki.gg mirror used). Map pages say almost nothing about kits/items. Videos gave only a HUD frame; no start/finish/void footage was captured. Kits per map are mostly NOT researched.

## 1. Facts

### Structure
- 6 rounds, one map per round, six different maps per event. [W] H
- Round length: 1:30 from MCC 1; raised to 1:40 in MCC HT (infobox says 1:40 now); MCC MU only: 2:00. [W History] H. (Intro text still says 1:30, outdated.)
- Name changes: TGTTOS (MCC 1-5) -> TGTTOSAWAF (MCC 6+), joke variants in special events. [W] H
- There are ~40 maps in the pool (35 current + event exclusives); not all appear. Which map in which round is chosen by Noxcrew per event; any selection rule (random/shuffle) is not documented. [W] M/L
- Players "start on one end of the map". Everyone is given "various tools". [W] H
- Gate/countdown before round start: not documented by wiki; video not captured. L (do not claim)

### Goal / finish
- Goal: reach the far side and punch ("whack") an NPC "fan". [W] H
- Until MCC 3: the NPC was a chicken ("To Get To The Other Side" joke). From MCC 4: NPCs with player model using skins of fans submitted to Noxcrew (April Fools). [W, U] H
- From MCC 6: the fan's name is shown in chat when a player hits them. [W, U] H
- Fans can be roster members/streamers/viewer teams depending on event (MCC 10 viewer teams, MCC P22 non-participants); MCC PARTY 2 has a hidden "Mega Chicken" bonus objective. [W] H
- Multiple fans at the end (per-map; MCC 13 Ranboo "in every map as a fan"). Whether each player has their own fan or fans are shared is not documented. L
- HUD (frame V1 0:40, MCC 22 Skydive): right-side scoreboard with "Game 0/8: TGTTOSAWAF", "Map: <name>", "Round: 5/6", timer (01:16), "Game Coins (x30)" with top-4 team scores, "Your Coins", and "Players Finished: 0/40". Hotbar present, health bar shown. [V1] M (low-res frame; the x30 multiplier and exact line text are uncertain)

### Items (what players are given)
- Original (MCC 1-25): wool blocks + shears. From MCC 26: concrete blocks + pickaxes instead. [W History MCC 26] H
- Basins: a Riptide trident is "given at the start as the only means of traversing". [M Basins] H
- Golf: heavily trident-based. [M Golf] H (exact item count unknown)
- Glide: building verticality + elytra techniques (elytra implied; MCC 22 era). [M Glide] H for elytra use; given items unknown.
- Boats / Slippery Slope: movement in boats, ice surface. Boats given: implied. [M] M
- Passing: all players get Potion of Leaping II before the round (to bridge upward faster). [M Passing] H
- Skydive (Remix): rocket launchers instead of blocks; platforms larger. [M Skydive] H
- Launcher: rocket launchers (from Rocket Spleef). [M Launcher] H
- Terra Swoop Force tunnels 1-3: explosion mechanics kept from the original TSF map; Frozen Tunnel: no explosions, players given fireworks and updrafts. [M TSF] H
- Chili Con Carnage: speed boost + ice slides. Water Park: swimming + bubble elevators. Beehive: slime and honey blocks (honey pits stick you). [M] H
- Some maps give "items to help sabotage opponents" (unspecified). [W] H/vague
- LEGO Chicken remix (MCC PG): ride a chicken, slow falling, cannot jump. Stack remix (Pride events): right-click another player to stack them in a tall tower; stacked players cannot place blocks. [W] H
- Reversed remix (MCC R3): travel from the old finish to the old start on 5 of 6 maps. [W] H

### PvP / knockback
- "Players can punch off other players as they wish." Sabotage is part of the game; punches knock back. Whether punches deal damage is NOT stated (our README claims zero damage; plausible, but unverified). [W] H for knockback, L for no damage.
- Karma system (MCC 31+): the more you get punched, the less knockback you take; the more you punch, the more knockback you take; low-karma players show red particles; heavy punchers can have punching temporarily disabled; karma recovers slowly over time. [W] H. MCC Island variant (patch 0.14.1): faster karma gain on repeated same-target punching, no karma gain for punching high-karma ("naughty") players, red glow. [I] M (different product, same lineage)

### Blocks
- Players place wool (concrete after MCC 26) and cut/break with shears (pickaxes after MCC 26). Whether you can break only player-placed blocks, and per-map limits/refills, are not documented. Industry: players can also break concrete blocks on the walls; Breakdown adds "breaking" elements. [M] M
- Scoring/placement limits (block count, refill): not found. L

### Falling / void / death
- Not documented on any page read. Maps mention lava pits (Shallow Lava: "one-deep lava pits"), cobwebs, guardians, water (Walls), piston traps and skeletons (Siege), mobs (Zoo). Respawn mechanic unknown: likely return to start, not confirmed. L

### Collision, finished players, round end
- Not documented. Cliff note: "punched constantly throughout the round". Skydive and Skyscraper flow downwards. No evidence for player collision rules, spectating of finishers, or end-of-round conditions beyond the timer. L

### Scoring (current, since MCC R) [W] H
- 40 coins to 1st finisher, minus 1 per lower placement (so 41-N per placement; wiki wording).
- Placement bonus top 10: 80, 75, 70, 65, 60, 55, 50, 45, 40, 35.
- Team bonus for first 4 teams to fully finish, shared among members: 100, 75, 50, 25.
- Wiki's History text says the MCC R change "bonuses for the top 10 players instead of the top 4, and total team bonuses were reduced". Note: team bonus now 100/75/50/25 vs 250/200/140/80 in MCC 17.
- Open: whether coin values on screen are multiplied (HUD showed "(x30)"?); see open questions.
- History of scoring: MCC 1-5: 80 for 1st, -2 per place. MCC 6: same plus 100 to 1st team. MCC 17: 40 base (-1 per place) + bonuses 60/45/30/15 for top 4; team bonus 250/200/140/80. MCC R: current. [W History] H

### Map types (categories; wiki tags per map)
- Speed-bridging: Pits, Walls (cobwebs, guardians, water), Industry (side bridging), Badlands, Shallow Lava (1-deep lava pits), Doors/Breakdown (doors + bridging/towering/breaking).
- Elytra/vertical: Glide (build verticality + elytra), Skydive (aim for a specific platform; bridge and tower downward), Skyscraper (dropper-like descent).
- Trident: Basins (Riptide only), Golf.
- Boats/ice: Boats (ice surface), Slippery Slope.
- Rocket launchers: Launcher, Skydive Remix, Terra Swoop Force (explosion mechanics).
- Parkour/obstacle run: Cliff (constant punching), Domino, Blankorp, Frozen Sprint, Train, Foot Race, Lunchtime, Zoo (mobs), Las Vegas, Spiral Climb / Ice Mountain (spiral up), Siege (piston traps + skeletons), To The Dome!, Scarland, Water Park, Beehive, Chili Con Carnage.
- [M pages] H for the one-line focus of each; specific kits unknown.

### Version changes (full list) [W History] H
MCC 1 debut (maps Pits, Boats, Doors, Cliff, Walls, Glide); MCC 4 chicken->fans + Basins, Skydive; MCC 6 fan names in chat + team bonus; MCC 10 Launcher; MCC 12 TSF tunnel 1; MCC 14 Industry, Siege, Breakdown, Skydive Remix, TSF tunnel 2; MCC 17 Shallow Lava + scoring; MCC R scoring; MCC AS To The Dome!; MCC 19 Cliff?; MCC 20 Beehive; MCC 22 Badlands; MCC P22 Stack remix; MCC 26 wool/shears -> concrete/pickaxes; MCC 28 Passing, Train; MCC 29 Spiral Climb, Skyscraper; MCC 31 karma; MCC TR Las Vegas, Chilli Con Carnage, Water Park; MCC 4KO Golf; MCC EC six maps; MCC TRSD Zoo; MCC BC Blankorp, Domino; MCC MU TSF3, Foot Race, Lunchtime, Skyblockle (2:00 timer once); MCC HT Scarland, 1:40 timer; MCC PG six maps + LEGO Chicken.

## 2. Open questions (not found, do not invent)
1. Void/fall handling and respawn; 2. player-to-player collision; 3. what finishers do (spectate, free move); 4. round-start countdown and gate; 5. exact kit per map and block counts/refills; 6. whether punches damage; 7. rules for breaking blocks; 8. fan count per finish area; 9. map selection rule; 10. real in-game coin multiplier. Best next source: a first-person full round video with captions (YouTube search page was slow/blank in the shared browser; only title-card frames of a compilation were captured).

## 3. Gap list: our README vs real game
1. Wrong maps: real game uses ~35 maps; ours are 6 invented types. Real map categories (bridging, elytra, tridents, rocket launchers, boats, parkour) are only partly covered: no elytra, trident, rocket-launcher, parkour-run maps. (Fix is design-level.)
2. Goal object: "punch (or right-click) chicken" -> since MCC 4 it is a fan NPC (player-skinned) and since MCC 6 its name appears in chat on hit; chicken only for pre-MCC 4 style. Punch only (right-click is not documented).
3. Round time: README 90 s; real is 100 s (current) since MCC HT (90 s before). Decide target era.
4. Items: README uses wool + shears; real since MCC 26 is concrete + pickaxes (wool/shears were the pre-26 kit). Snowballs/eggs/zombies are not in researched sources (unverified inventions); Basins needs a Riptide trident, Passing gives Leaping II, Skydive/Launcher give rocket launchers, boats exist on Boats.
5. Scoring wrong: README 12/11/.../3, later finishers 2, team bonus 8/5/3 per member. Real: 40 for 1st minus 1/place, +80/75/.../35 for top 10, team bonuses 100/75/50/25 (first four teams to complete) shared among members. (Scale may be display-multiplied; see open question 10.)
6. Team bonus triggers: README gives bonus only to "TEAM:" sides with online members all finished; real is first 4 teams where all members finished; four ranks, not three.
7. Karma anti-spam: README lists "no karma" as a limitation. Real game has karma (MCC 31+). Should be implemented (less knockback taken for victims, more for spammers, temporary punch disable).
8. PvP: README says punches deal no damage; wiki only states players can punch others off and the knockback is a core mechanic. Keep, but mark "unverified".
9. Falling: README sends you back to spawn with fresh kit. Real behavior not documented; mark unverified.
10. Finished players spectate in spectator mode; real behavior not documented.
11. Missing mechanics: remix modes (Stack, Reversed, LEGO Chicken), special blocks (slime/honey, cobwebs), mobs in a few maps (Zoo, Siege), per-map timers.
12. HUD: README documents none. Real HUD: scoreboard with game number, map name, round n/6, timer, team coins, your coins, players finished x/N.
13. Map selection: README shuffles 6 generated maps; real picks 6 from a large pool, a different map each round.
14. "Defaults not researched" section is missing in README (LESSONS rule): gate length, countdown, void respawn, block limits.

## 4. Draft acceptance checklist (testable; # = confirmed by research, ? = default/unverified)
1. A game consists of 6 rounds, each on a different map. #
2. Round timer is 100 s (config; 90 s legacy option). #
3. HUD scoreboard shows game number, map name, "Round n/6", countdown timer, top team scores, own coins, "Players Finished x/N". # (layout M)
4. The finish object is an NPC fan with a player skin at the finish area; punching it completes the map for the puncher. #
5. On hit, the fan's name is displayed in chat to the puncher (or all). #
6. A finished player's finish place is recorded; placement points = 41 - place (min 0) plus top-10 bonus 80,75,...,35. #
7. When all members of a team have finished, that team gets a team bonus 100/75/50/25 for first four, shared among members. #
8. Teams beyond fourth get no team bonus. #
9. The round ends at timer zero; unfinished players score nothing for that map. ? (round end condition not documented)
10. Kit on block maps is concrete + pickaxe (config toggle for wool + shears legacy). #
11. A trident map gives a Riptide trident as the only crossing tool. #
12. A "leaping" map gives Leaping II potion at start. #
13. Elytra map: players have elytra/fireworks or build upward then glide. # (given items M)
14. A rocket-launcher map gives launchers and explosions throw players. #
15. Boat map gives boats and ice surface. M
16. Punching another player applies knockback. #
17. Karma: a player who is punched repeatedly takes less knockback; a repeated puncher takes more knockback, shows red particles, and can have punching disabled temporarily; karma decays over time. #
18. Players can place blocks only where the map allows; placed blocks are removed at round end. ? (limits not researched)
19. Falling into the void/lava returns player to a start position (not elimination). ? (unverified)
20. Finished players can no longer score again on that map and may watch. ?
21. Map picked per round differs from earlier rounds in the same game. M
22. A "Remix" option exists as a config for later (stack/reversed). #
23. Config exposes all defaults (gate/countdown, block limits, respawn), listed in README as "defaults, not researched". process rule
