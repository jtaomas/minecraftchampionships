# Skyblockle (MC Championship / MCC Island) — rules research

Prepared 2026-11-23 for a private, non-commercial fan-server plugin. Everything here is paraphrased. Each fact has a confidence:
**H** = stated directly by two or more sources, or plainly by one primary wiki page; **M** = one secondary source, or inferred from structured data; **L** = inferred or uncertain.

## Sources used

| ID | URL | Notes |
|----|-----|-------|
| S1 | https://mcchampionship.fandom.com/wiki/Skyblockle | Read with the built-in browser. Has the official description, coin values and trivia. |
| S2 | https://mcchampionship.wiki.gg/wiki/Skyblockle | Mirror of S1, plus stats (appearances, aces, eliminations record). |
| S3 | https://www.mccisland.wiki/Skyblockle | The MCC Island 2026 remix: team size, duration, random-item pool, version history. |
| S4 | https://www.mccisland.wiki/index.php?title=Map:Skyblockle_Map&action=raw | DataMaps JSON behind S3's interactive chest map: team spawns, chest types and chest contents. Read in the browser. |
| S5 | https://www.mccisland.wiki/0.32.0 | Patch note: "over 1600 items in the loot pool". |
| S6 | https://mcchampionship.wiki.gg/wiki/MC_Championship_Scuffed | MCC AF / Scuffed (2023) appearance: troll changes, x3 multiplier. |
| S7 | https://mcchampionship.wiki.gg/wiki/Sky_Battle | Sky Battle event rules, used for the comparison. |
| S8 | https://mccisland.wiki/Sky_Battle (also read raw) | MCC Island Sky Battle: starting kit, chests, XP. S3 says the remix reuses its scoring. |
| S9 | https://en.wikipedia.org/wiki/MC_Championship | One-line game summaries and the years each game ran. |
| S10 | https://minecraft.wiki/w/MC_Championship | One-line Sky Battle summary. |
| S11 | https://mcchampionship.wiki.gg/wiki/Skyblockle_(TGTTOS) | Map reuse; notes that each team has a different start point. |
| S12 | https://mcchampionship.wiki.gg/wiki/Teams | "10 teams of 4 players" (current event format). |

**Blocked or empty:** mcchampionship.com (403), tvtropes (403), sportskeeda (405), dreamteam.fandom (402) and web.archive.org (blocked). A second fandom page (MCC Scuffed) failed to load in the browser, so I used the wiki.gg mirror (S6). Searches turned up no Reddit threads or video descriptions with mechanics. Community discussion is therefore **not** represented.

There are **two versions** of the game. Keep them apart:
- **Classic** (MCC 1–8, 2019–2020, plus MCC Scuffed/AF in 2023): 4-player teams, 18 minutes. The wikis document this version only thinly.
- **MCCI Remix** (MCC Island 0.32.0, April Fools 2026; Plobbies since 0.37.4): 2-player teams, 10 minutes, random items. This version has detailed chest data (S4).

---

## 1. What the game is

- A battle royale built on Skyblock. Each team starts on its own small island, surrounded by other islands that hold useful resources. Players gather and equip, fight their way toward the centre, and try to be the last team standing. **H** (S1, S2, S9)
- Players have to mine blocks and craft their own tools from what the map provides. S3 says this outright for the remix and calls it "similar to regular Skyblockle". **M** for the classic version (S3, S9: "gather items much like in the classic map Skyblock")
- It is one of the original 8 MCC games. It debuted in MCC 1 (17 Nov 2019) and was replaced by Sky Battle from MCC 9 (Sep 2020). **H** (S1, S2, S7, S9)
- The game has one map, also called "Skyblockle". **H** (S1, S2, S3)
- Abbreviation: SBL. **H** (S1, S3)

### How it differs from Sky Battle (its replacement)

| Aspect | Skyblockle (classic) | Sky Battle (MCC event) | Conf. / sources |
|---|---|---|---|
| Building blocks | No infinite blocks. You get blocks by mining, from chests, and from the map (cobblestone generators are present, see §3). | Teams start with **infinite blocks** (concrete). | H for Sky Battle (S7, S8); M for Skyblockle (S3, S4) |
| Gear progression | Mine, craft and loot. The starting kit is basic food and materials. | Fixed starting kit (MCCI: iron chestplate, stone sword, bow, iron pickaxe, etc.) plus tiered loot chests. | M (S3, S4, S8) |
| Rounds / length | **1 round, ~18 min** | 3 rounds of ~4–5 min each, plus Overtime | H (S1, S7) |
| Border | Map borders close in over time, toward the centre. No timing is documented. | Circular border shrinks from the sides **and the top**. It deals damage outside, and is removed in Overtime. | H (S1, S7) |
| Coins | 10 per other death while you live / 70 per kill / 150 for surviving | 2 / 40 / 50 (split among survivors), plus a team placement bonus of 65 down to 5 | H (S1, S2, S7) |
| Maps | 1 | Many (12+) | H (S2, S11 nav box) |

In short, Skyblockle is the slow, survival-crafting ancestor: you earn your blocks and tools. Sky Battle is the fast arena version: blocks are infinite and the kit is fixed. **M** (synthesis of the above)

---

## 2. Teams, players, map

| Fact | Value | Conf. | Sources |
|---|---|---|---|
| Classic team size | 4 (an ace is defined as killing all four members of an enemy team) | H | S1, S2, S3 ("two instead of four"), S12 |
| Classic teams per map | 10 (MCC format is 10 teams × 4 = 40 players; the map has 10 team spawns) | M | S12, S4 |
| Remix team size | 2 | H | S3 |
| Remix players / teams | 20 players → 10 teams of 2 | H (players) / M (teams, derived) | S3, S4 (10 "Team Spawn" markers) |
| Starting position | Each team has its own spawn island; start points differ per team | H | S1, S11, S4 |
| Map layout (remix data, assumed same map) | 10 spawn islands, 10 cobblestone-generator islands, 10 grass islands, 20 sand/granite spikes, 20 concrete pillars, 10 Nether-fort structures, 10 brewing platforms, 10 enchanting platforms, and a central **Sky Castle** (4 chests) | M (counts come from marker data; I have not checked that the layout matches the classic map) | S4 |

Layout reading (**L**): there are 10 of most structure types, so each team appears to have its own ring of satellite islands (generator, grass, Nether, brewing, enchanting), with shared spikes and pillars in between and the castle at the centre.

---

## 3. Starting island, items and resources

### Starting items
- **Classic:** no starting-kit list was found. **Open question.**
- **Remix:** no inventory kit is documented. Each team's spawn-island chest holds **4× Apple (2 each), 2× Oak Log (2 each), 2× Cobblestone (8 each)**. **M** (S4; the "N× Item xM" notation is read as N slots of M items, **L**)

### Resources and how they are obtained
- **Chests** sit on fixed islands with fixed contents. Remix data (S4), all **M**:

| Chest (count) | Contents |
|---|---|
| Spawn Island (10) | 4× Apple ×2, 2× Oak Log ×2, 2× Cobblestone ×8 |
| Cobblestone Generator island (10) | 5× Iron Ingot, 5× Gold Ingot, 2× Stone Pickaxe, 2× Cooked Chicken ×2 |
| Grass Island (10) | 2× Arrow, Bow, Splash Potion of Healing |
| Sand Spike (10) / Granite Spike (10) | 2× Cobblestone ×8, Iron Sword, Golden Apple |
| Concrete Pillar (20) | 2× Wind Charge, 4× Diamond |
| Nether Fort (10) | 2× Snowball ×4, 2× Stone Sword, 2× Cooked Chicken ×3, 2× Cobblestone ×8, Water Bucket |
| Brewing Platform (10) | 2× Arrow ×2, 2× Potion of Healing, 2× Cobweb |
| Brewing Stand (10, one per platform) | 3× Awkward Potion (pre-loaded, so players brew their own potions) |
| Enchanting Platform (10) | 2× Ench. Book Protection I, 8 Snowballs, 2× Steak ×4, 2× Cobblestone ×8, 2× Ench. Book Sharpness I |
| Sky Castle, centre (4) | Wind Charge, 5 Golden Carrots, Diamond Boots |

- Wind Charges (a 1.21 item) show that the remix chest contents were modernised. The classic 2019 chests cannot have had them. Classic chest contents are **unknown** (**open question**).
- **Cobblestone generators:** each team has a "Cobblestone Generator" island (S4). This means cobblestone can be mined from a lava+water generator, as in classic Skyblock. **M** (named in S4; the mechanics are not described)
- **Mining:** players mine map blocks. **M** (S3)
- **Crafting:** players craft their own tools. Logs, iron and gold ingots and diamonds are supplied as raw materials, not as finished gear. **M** (S3, S4)
- **Brewing and enchanting:** dedicated platforms exist with brewing stands and enchanted books, which implies anvil or enchanting use. **M/L** (S4; it is not stated whether anvils, enchanting tables or XP are available)
- **Trading:** no evidence of villager or shop trading. **L** (not mentioned anywhere)
- **Remix only:** every **10 seconds**, each player receives random items. Items differ per player and in amount. The pool is any Creative-inventory item up to MC 1.21.8 (more than 1600 items) minus an exclusion list, plus MCCI custom items (Eyepatch, Gluten Free Bread, Mysterious Rift, and the potions, sparks and orbs from the Mysterious Rift). Blocks come in stacks of 8 and items in stacks of 4 where possible, otherwise 1. Ore/metal blocks, golden apples and enchanted golden apples always come as 1. **H** (S3, S5)
  - Exclusions: enchanted books, glazed terracotta, bundles, pottery sherds (Blade was accidentally left out of the exclusion list), armor trims, netherite upgrade, mob heads, brown stained glass and pane, end portal frame, bedrock, spawners, operator items and paintings. **H** (S3)

---

## 4. Building, PvP, lives, void, hazards

| Topic | Finding | Conf. | Sources |
|---|---|---|---|
| Building | Players bridge and build toward the centre using gathered blocks | H | S1 ("gather resources by building to the center") |
| Block breaking | Players can mine map blocks (needed for mining and generators) | M | S3 |
| PvP | Free PvP between teams; kills are the main source of coins | H | S1 |
| Friendly fire | Lava placed by a player killed his own teammates in MCC 3, and those kills earned coins. In later events, team kills give **no** reward. Damage between teammates (at least environmental) therefore exists; whether melee friendly fire is on is unknown | M | S1, S2 |
| Lives / respawn | **One life.** Once you die you are out, with no way back in | H | S1, S2 |
| Void | No explicit rule found. A void death counts as a death (one life). **Open question:** credit for void knock-offs | L | — |
| Border | Map borders close in over time, pushing every team to the centre. **No timings, speed, damage or vertical component documented** | H (existence) / — (details) | S1, S2 |
| Lava / water | Lava buckets existed in classic (MCC 3 incident). Remix has a water bucket in the Nether fort chest | H / M | S1, S4 |
| Scuffed troll hazards (MCC AF only, not core rules) | Organisers placed End Crystals in the middle, spawned random mobs on bridges and islands, and built a beacon | H | S6 |

---

## 5. Rounds, length, scoring, win condition

| Fact | Value | Conf. | Sources |
|---|---|---|---|
| Rounds | 1 | H (remix states 1; classic implied by an 18-min single game) | S1, S3 |
| Classic length | 18 minutes | H | S1, S2, S3 |
| Remix length | 10 minutes | H | S3 |
| Coins: another player dies while you are alive | **10** per death | H | S1, S2 |
| Coins: personal elimination | **70** (chat in MCC 2 and 3 wrongly showed 60; 70 was actually paid) | H | S1, S2 |
| Coins: survive the entire game | **150** | H | S1, S2 |
| Team placement bonus | None documented for Skyblockle (Sky Battle has one) | L | S1 vs S7 |
| Team kills | No reward (from after MCC 3) | M | S1 |
| Assists | Not documented for classic. In the remix (Sky Battle scoring), assists give no personal score | L / M | S8 |
| Multiplier | Normal by round slot; MCC AF used x3.0 as game 8 | H | S6 |
| Remix scoring | Reuses **Sky Battle's** MCCI scoring and XP table: Score for surviving and for eliminations. XP is the higher of placement XP (1st 300 … 11–12th 75) or time-survived XP, plus elimination XP | M | S3, S8 |
| Win condition | Last team with at least one living player | H | S1, S2 |
| End at time limit with several teams alive | **Unknown.** The "survived the entire game" bonus suggests the game can end with survivors from more than one team when time runs out | L | inferred from S1 |

---

## 6. Version history

| Event / version | Change | Conf. | Sources |
|---|---|---|---|
| MCC 1 (17 Nov 2019) | Introduced; round 6 | H | S1, S2 |
| MCC 2, MCC 3 | Chat showed 60 coins per kill, 70 paid. MCC 3: lava team kills still paid coins | H | S1 |
| After MCC 3 | Team kills no longer rewarded | M | S1 |
| MCC 5, MCC 6 | Not chosen / not played | M (S1 lists "Not chosen" across both rows) | S1, S2 |
| MCC 9 (Sep 2020) | Rotated out and replaced by Sky Battle | H | S1, S7 |
| MCC Scuffed / AF (2023) | Brought back as forced game 8 (x3.0). Old-version textures, deliberately worse logo, Sky Battle music, troll End Crystals, mobs and beacon | H | S6, S1 |
| MCC MU | The map was reused as a TGTTOSAWAF course ("Skyblockle"), with different start points per team | H | S11, S1 |
| MCCI 0.32.0 (1 Apr 2026) | Remix: teams of 2, 20 players, 10 min, random items every 10 s, modernised chests, Sky Battle scoring, music and podium | H | S3, S5 |
| MCCI 0.32.1 | Removed | H | S3 |
| MCCI 0.37.4 | Playable in Plobbies (private lobbies) | H | S3, S2 |

---

## 7. Open questions (not guessed)

1. **Classic starting kit:** any items in inventory at spawn, or only the spawn chest?
2. **Classic chest contents** (2019). The remix chest list includes post-2019 items (Wind Charge), so it cannot be identical.
3. **Border:** start time, shrink speed, final size, damage per second, whether it closes vertically, and whether it finishes before 18:00.
4. **What happens at time-out** with more than one team alive: tie, survivors all paid 150, overtime, or sudden death?
5. **Void kills:** who gets elimination credit for knock-offs, and is there a "last damager" window?
6. **Melee friendly fire:** on or off?
7. **Cobblestone generator mechanics:** pre-built and working, or players must build them with supplied lava/water? Is generator output normal cobblestone or modified?
8. **Enchanting and brewing:** are enchanting tables, anvils and XP available? Is blaze powder provided for the brewing stands?
9. **Is there a grace period** (no-PvP window) at the start?
10. **Remix random items:** do they apply in classic? (No: the remix added them to speed things up.) Is the per-player item and amount distribution uniform or weighted?
11. **Spectating** after death, and whether dead players' items drop.
12. Exact **map geometry**: distances between islands, height layers, and the size of the central Sky Castle.

Answering 1–9 needs VOD review (MCC 1–8 and MCC Scuffed broadcasts) or a direct Noxcrew/wiki-editor source.

---

## 8. Draft acceptance checklist

Statements marked [remix] follow the 2026 MCCI data. The owner should choose which version the plugin targets; confirm the tunables before testing.

1. A match has exactly 1 round and ends at most **18:00** after start (classic) or **10:00** (remix), configurable.
2. Classic: teams have 4 players. Remix: 2 players. The map supports **10** teams.
3. Each team spawns on its **own** island, and no two teams share a spawn island.
4. Players start with no infinite-block item and no fixed combat kit. A spawn-island chest is present.
5. [remix] Each spawn-island chest contains 8 apples, 4 oak logs and 16 cobblestone.
6. Each team has a reachable cobblestone-generator island, and mining its generator yields cobblestone repeatedly.
7. Players can break map blocks and place gathered blocks; they can craft vanilla tools at a crafting table.
8. Chest contents per island type match the configured loot table (§3). Chests do not refill during the match (to confirm).
9. The central Sky Castle holds better loot (e.g. diamond boots and golden carrots) than the outer islands.
10. A player who dies (any cause, including the void) is permanently eliminated for the rest of the match and becomes a spectator.
11. Falling into the void kills the player and counts as a death for the "+10 per other death" coin award.
12. Every living player receives **+10 coins** each time any other player dies.
13. The killer receives **+70 coins** for each personal elimination of an enemy player.
14. Killing a teammate gives **0 coins**.
15. Every player alive at the end of the match receives **+150 coins**.
16. The world border starts shrinking during the match toward the map centre, and players outside it take damage.
17. The game ends immediately when only one team has living players, and that team is the winner.
18. If time expires with more than one team alive, the configured tie rule applies and every surviving player still receives +150.
19. PvP between different teams is enabled from the start (or after a configurable grace period, pending confirmation).
20. [remix] Every 10 s, each living player receives a random item from the allowed pool. Blocks come ×8, stackable items ×4, non-stackables ×1, and ore blocks and golden apples ×1. Excluded items (bedrock, command blocks, spawners, etc.) are never given.
21. Brewing stands on the brewing platforms start with 3 Awkward Potions.
22. Dead players cannot re-enter the arena by any means (rejoin, respawn command, etc.) until the match ends.
