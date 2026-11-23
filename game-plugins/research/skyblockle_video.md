# Skyblockle (classic, MCC 1–8): video research

Researched 2026-11-23/24. View-only: frames were sampled in the built-in browser by seeking the video with JS, pausing, and screenshotting. Nothing was downloaded, and no sign-in, likes or comments were used. Everything is paraphrased. The aim is to support our own plugin and our own map, not to copy the original.

Confidence: **H** = read directly off a clear frame (tooltip, scoreboard, chat), or seen identically in two events. **M** = seen once, or partly blurred. **L** = inferred.

## Videos used

| ID | Video | What it is | Game start in video |
|---|---|---|---|
| **V1** | `wDgdTPxILe8` ("TommyInnit MCC4 Skyblockle", MCC Clips) | One player POV of the full MCC 4 game (Purple Pandas), 720p, streamer overlay | 0:46 |
| **V2** | `ez2aOz2rENg` ("MCC 4 – Purple Pandas Skyblockle … QUAD VOD", MCZombie) | 4 POVs side by side (Philza, Wilbur, Tommy, Technoblade), 1080p. Includes the full pre-game tutorial, the whole game, and the ending | 4:56 (296 s) |
| **V3** | `gCV6fJ5sVwg` (Philza MCC 1 VOD; Skyblockle chapter 1:37:55–1:59:41) | One player POV of the MCC 1 game (Yellow Chickens) | ≈1:41:33 (6093 s) |

Timestamps below are positions **in the video** (mm:ss or h:mm:ss). V2's Tommy quadrant runs about +4:10 behind V1, so a V1 time of 7:01 matches V2 11:11.

How the sampling was done: a canvas-based detector flagged frames that contain GUI-grey pixels (every 1–1.5 s across each game). Those frames were then checked by eye and zoomed. V2's Philza and V3's Philza mostly open crafting tables and furnaces, so their chests are few.

Rejected sources: `KRWlLYMoDHY` ("skyblockle", 37 min) is not MCC; it is a different server's PvP mode. Short highlight clips such as the MCC Vault ones were not needed.

---

## 1. Chest openings (priority 1)

Grid notation: r = row (1–3 for a single chest), c = column (1–9). All loot chests seen are **single 27-slot chests**.

| # | Video @ time | Island type (conf.) | Contents seen (count) and grid position | Conf. on items |
|---|---|---|---|---|
| 1 | V1 0:45.6 (MCC 4) | **Start island**, the one chest next to the tree (H) | r1c4 sugar cane ×9 · r1c6 orange-brown block ×6 (terracotta-like, ID not confirmed) · r2c2 sapling (oak/birch type) ×1 · r2c4 seeds ×3 (green, probably wheat seeds) · r2c5 apple ×6 · r2c6 snowball ×8 (tooltip "minecraft:snowball", 0:46.5) · r2c8 acacia sapling ×1 · r3c4–c6 milk bucket ×3 (one per slot) | H (layout), M (seed/block IDs) |
| 2 | V3 1:41:33.6–1:41:35 (MCC 1) | **Start island** (H) | **Identical** to #1, slot for slot. Tooltips read "Milk Bucket / minecraft:milk_bucket" and "Acacia Sapling" | H |
| 3 | V1 1:15 (MCC 4) | **Generator island** chest, inside a small stone-brick hut. A sign reads "Cobblestone Generator …"; the lava+water generator is next to it (V1 1:30–2:00) (H) | r1c2 red mushroom-like item ×1 · r1c9 flint(?) ×3 · r2c4 leather ×12 · r2c5 ice/packed-ice block ×6 · r2c6 egg ×3 · r3c2 coal ×3 · r3c4 water bucket ×1 (tooltip 1:29) · r3c6 lava bucket ×1 · r3c8 Bottle o' Enchanting ×~4–5 | H (layout), M (mushroom/flint IDs, bottle count) |
| 4 | V3 1:42:13.5 (MCC 1) | **Generator island** (H, same contents as #3) | **Identical** to #3 | H |
| 5 | V1 2:27 (MCC 4) | **Hidden chest inside the generator island's rock**, found while mining. Ores (coal, iron, redstone) are embedded in the same rock (V1 2:26, 2:45) (M) | r2c5 potato ×1 · r2c6 book (enchanted?) ×1 · r2c7 Bottle o' Enchanting ×1 (tooltip). Tight cluster in the middle row | M |
| 6 | V3 1:46:46 (MCC 1) | Probably the same hidden chest type (L). Philza may have added the diamond sword himself | potato ×1, enchanted book ×1, (diamond sword) | L |
| 7 | V2 6:07.3 (Technoblade, MCC 4) | **Dark "ship" island**: dark wood/blackstone hull, cobwebs in the rigging, mast and lantern (V2 6:05.5) (M) | r1c2 wheat ×12 · r1c4 cobweb ×2 · r1c5 Bottle o' Enchanting ×~4 · r1c6 cobweb ×2 · r1c8 melon slice ×1 · r2c4 water bucket · r2c5 diamond ×2 · r2c6 lava bucket · r3c2 sandstone-like block ×10 · r3c3 coal ×3 · r3c4 wooden door/sign ×1 · r3c5 **trident** ×1 (tooltip) · also bone ×3 and snowball ×4 (seen at 6:09) | H (trident, diamond), M (rest) |
| 8 | V1 7:01 = V2 11:11 (Tommy, MCC 4) | **Top of a tall TNT tower** (a 1×1-ish pillar of TNT blocks; he towers up it at V2 11:11) (H) | r1c5 diamond ×1 · r2c2 redstone ×3 · r2c4 snowball ×4 · r2c6 ender pearl ×1 (tooltip) · r3c5 cobweb ×3 | H |
| 9 | V1 7:29 = V2 11:39.6 (Tommy, MCC 4) | **Second TNT tower** (TNT is visible right next to it at V2 11:38) (M) | r1c5 diamond ×1 · r2c2 redstone ×3 · r2c4 snowball ×4 · r2c7 ender pearl ×2 · r2c9 bone ×1 · r3c5 cobweb ×3 | H (items) |
| 10 | V2 11:06.5–11:09.5 (Technoblade, MCC 4) | **Enchanting deck** just outside the castle: wooden deck with bookshelves, enchanting table, anvil, chest and hay (V2 11:05.3) (H) | r1c2 lapis lazuli ×6 · r1c3 green block ×8 (melon or slime, not confirmed) · r1c4 coal ×2 · r1c5 Bottle o' Enchanting ×3 · r1c7 cobweb ×5 · r1c8 orange orb ×1 (fire charge or magma cream?) · r2c6 bone ×1 · **r3 full row of enchanted books** (≈8; tooltips show Power I, Sharpness I, Protection I) | H (books, lapis, XP), M (others) |
| 11 | V2 12:45.4 (Tommy, MCC 4) | Inside a **stone structure / tunnel** (mid-map, dark brick walls; exact island not identified) (L) | r1c5 TNT ×1 · r2c5 chainmail-looking leggings ×1 · r3c3 cobweb ×3 · r3c4 sandstone/end-stone-like block ×8 · r3c5 redstone torch ×1 (tooltip) · r3c7 green block ×8 · r3c8 snowball ×2 | M |
| 12 | V3 1:45:49 (Philza, MCC 1) | **Small red terracotta/granite islet** with the chest sitting exposed on top (V3 1:45:47.5) (M) | r1c4 raw porkchop ×2 (tooltip "Raw Porkchop" on the same item later) · r1c5 Bottle o' Enchanting ×1 · r1c6 raw meat (beef?) ×2 · r2c4 lava bucket · r2c5 coal ×5 · r2c6 water bucket · r3c3 snowball ×8 · r3c4 mossy cobblestone ×8 · r3c7 diamond ×1 | M |
| 13–17 | V2 13:42, 16:34, 17:18, 18:27, 20:46 | **Player graves** (not loot chests; see §6) | Each shows the dead player's whole inventory in its original slots, as a 5-row container titled "<player>'s Grave" | H |

Observed patterns:
- **Contents are fixed, not random.** The start chest and the generator chest are identical slot for slot in MCC 1 (Nov 2019) and MCC 4 (Apr 2020). The two TNT-tower chests are almost identical. **H**
- **Arranged, not scattered.** Items sit in a loose centred cluster, mostly columns 2–8, often symmetric (three milk buckets in r3c4–6; water bucket and lava bucket in c4/c6). This looks hand-placed in a template chest, not filled by random slot selection. **H**
- **Counts are small and "odd":** 3, 6, 8, 9, 12, plus singletons. Nothing arrives as a full stack. **H**
- **Raw materials, not finished gear,** at the edge (logs come from the tree, plus leather, coal, ores to smell, saplings, seeds). Finished gear shows up only as single pieces mid-map (trident, chainmail leggings) and as enchanted books near the centre. Players craft their own swords and tools (diamond sword crafted V3 1:46:42, iron gear V3 1:48). **H**
- **Utility items recur everywhere:** cobwebs, snowballs, water/lava buckets, Bottles o' Enchanting. **H**
- Chests do **not** visibly refill: the second visits seen were all to graves or to chests already emptied. **L** (no direct test)

---

## 2. Loot table by island type (for our plugin)

"Seen" = read off the frames above. "Inferred" = our proposal to fill gaps; check it against the remix data in `skyblockle_rules.md` §3 and against our own balance.

| Island type (per team unless noted) | Chests | **Seen** (classic) | **Inferred / proposal** |
|---|---|---|---|
| **Start island** (1 per team) | 1 (H) | milk bucket ×3, apple ×6, snowball ×8, sugar cane ×9, terracotta-like block ×6, wheat(?) seeds ×3, 2 saplings (oak/birch-type + acacia) | Keep it exactly like this: food, a renewable (saplings, seeds, cane) and a few throwables. The tree on the island supplies the logs |
| **Generator island** (1 per team, next to the start) | 1 in a hut (H) + 1 hidden in the rock (M) | Hut chest: water bucket, lava bucket, coal ×3, flint(?) ×3, leather ×12, ice ×6, egg ×3, red mushroom ×1, XP bottles ×4–5. Hidden chest: potato, (enchanted) book, XP bottle. The rock contains **coal, iron and redstone ore** | A working cobblestone generator is already built. Including the buckets lets players build a second one. Ores in the rock give iron via a furnace |
| **TNT towers / tall pillars** (seen 2 near one team's path; probably 1–2 per team) | 1 on top (H) | diamond ×1, redstone ×3, snowball ×4, ender pearl ×1–2, cobweb ×3, (bone ×1) | "Risky climb = diamond + mobility". This matches the remix "pillar = diamonds + wind charge" |
| **Ship island** (dark wooden ship with cobwebs) (count unknown) | ≥1 (M) | trident ×1, diamond ×2, water + lava bucket, wheat ×12, XP bottles ×4, cobweb ×4, melon slice, bone ×3, snowball ×4, sandstone ×10, coal ×3, door ×1 | A themed "showpiece" island with one unique weapon. For our map: 1 showpiece per wedge or per two wedges |
| **Small red stone islet** (spike/rock type) | 1 on top (M) | diamond ×1, snowball ×8, mossy cobble ×8, coal ×5, water + lava bucket, raw meat ×2+2, XP bottle ×1 | Same tier as the pillars: one diamond plus blocks and food |
| **Stone mid structure** (unidentified) | ≥1 (L) | TNT ×1, chainmail leggings ×1, cobweb ×3, sandstone ×8, redstone torch ×1, green block ×8, snowball ×2 | First armour piece and blocks |
| **Enchanting deck** (just outside the castle; has enchanting table, bookshelves, anvil) | 1 (H) | ~8 level-I enchanted books (Power, Sharpness, Protection; Loyalty also seen in inventories), lapis ×6, XP bottles ×3, coal ×2, cobweb ×5, green block ×8, bone, orange orb | Lapis + XP bottles + books + anvil = the enchanting tier. **Anvil confirmed** (V2 11:17 "Repair & Name"; V2 14:53 trident + Loyalty book, "Enchantment Cost: 1") |
| **Nether-brick fort** (seen from outside only, V2 10:58) | not seen | – | Remix data: snowballs, stone swords, food, cobble, water bucket. **Not verified for classic** |
| **Brewing deck** | not seen | – | Remix data: arrows, healing potions, cobwebs, brewing stand with awkward potions. **Not verified** |
| **Between-team grass island** | not seen | – | Remix data: bow, arrows, splash healing. **Not verified** (no bow was seen in any classic frame) |
| **Central castle** | not seen opened | – | Remix data: 4 chests (diamond boots, golden carrots, mobility). **Not verified** |

Typical chest volume: **7–14 occupied slots out of 27**, 1–12 items per slot.

---

## 3. Start of the game (priority 2)

- **Spawn:** each team's four players stand together on the start island about 30 s before the start. The island is grass/dirt with **one oak tree and one chest** (V2 4:45 all four POVs; V1 0:45). **H**
- **Inventory at spawn: empty.** All hotbars are empty during the countdown (V2 4:45, V1 0:45, V3 1:41:00). There is no kit; everything comes from the chest, the tree and mining. **H**
- **Pre-game sequence** (V2 3:35–4:28, chat panels, paraphrased): "Welcome to Skyblockle, here's how you play" → be the last team standing; everyone starts on their own Skyblock island; gather resources, craft and eliminate opponents → there are plenty of resource islands around, some with chests of equipment, so get creative → over time the **map border closes in, marked by a particle border, "Red = dead"** → **one life; falling kills you** → get mining and crafting quickly and don't run out of blocks; **the border shrinks every 2 minutes once it has started**; prepare as much as you can at the start → coins (un-multiplied): **+10 each time another player dies while you are alive, +70 per personal elimination, +150 for surviving the whole game** → "Stand by for the game to begin…". **H**
- **Countdown:** sidebar "Game begins: 00:33 … 00:01", then the title "Starting in 3/2/1" (V1 0:45, V3 1:41:00). Players could look around and walk on the island during the countdown; the chest was opened within 0.6 s of the start (V1 0:45.6). Whether movement or chest access is locked before 0 is unknown. **M**
- **Grace period:** none was announced in the tutorial, and the sidebar shows no grace timer. Teams start ~80 blocks apart with no bridges, so early PvP is impossible anyway. **M**

## 4. Border (priority 3)

- **Timing (read off the sidebar "Next Shrink" in three games):** 18:00 game. First shrink at **7:00 elapsed (11:00 left)**: V1 0:50 shows "Time left 17:56 / Next Shrink 06:56"; V3 shows 17:59 / 06:59. After that **every 2 minutes**, at 9:00, 7:00, 5:00 and 3:00 left (V2 10:58 shows 11:53/0:53; V2 12:40 shows 10:16/1:15; V2 19:40 shows 3:16/0:15). The last regular shrink comes at **1:30 left** (V2 20:05 shows 2:51/1:20; V2 21:12 shows 1:48/0:17). **H**
- **Final phase:** from 1:30 left the sidebar line changes to **"LAST STAND: 00:30"** counting down (V2 21:27: "Time left 01:29 / LAST STAND 00:28"). What happens at 0 was not seen; both observed games ended earlier. **H** (existence) / unknown (effect)
- **Feedback:** at each shrink a red "Border shrinking" subtitle appears on screen (V2 11:22, all POVs). Red chat lines warn about the border before it moves (V2 21:30 chat). **H**
- **Look:** a particle wall. Large drifting **cyan particle clouds** mark it in the sky (V3 1:56:00 around the castle late game; cyan specks visible all game long, V2/V3). The tutorial says "Red = dead", so the outside is probably tinted or particle-marked red. **M**
- **Damage / speed / height limit:** not measurable from the frames. No vertical ceiling was seen; players towered high on the TNT pillars without any warning. **L**

## 5. HUD and feedback (priority 4)

- **Sidebar** (H, all three videos), top to bottom: `MC Championship` · `Game N/8: Skyblockle` · `Time left: mm:ss` (before the start: `Game begins: mm:ss`; after the end: `Back to Hub: mm:ss`) · `Next Shrink: mm:ss` (replaced by `LAST STAND: mm:ss` in the final phase) · blank · `Game Coins: (x2.5)` (MCC 4 Easter event: "Game Eggs (x1.0)") · the top 3 teams plus your own team with points · blank · `Players Alive: 40/40` · `Teams Alive: 10/10` · `Your kills: 0`.
- **Chat feed** (H, V2 10:58 and 21:30):
  - `[+10.0 coin] <player> fell out of the world` (death message with your +10 prefix)
  - `<player> was slain by <player>`, `walked into fire whilst fighting …`, `fell from a high place`
  - `[+70.0 coin] You killed <player>!`
  - `[▶] Only 3 players remain!`
  - `[skull] <Team> have all been eliminated!`
  - `[+150.0 coin] You and your teammates were the last players standing!`
  - End summary: `You finished in 1st place!`, `You eliminated 6 opponents.`, `You earned 950.0 coins this round.`, then "Top 5 players this game", "Best players in your team" and "Each team scored this game".
- Team scores in the sidebar rise in steps of 10 per living team member when anyone dies (e.g. 0 → 40 for a 4-player team, V1 4:30). **H**
- Big **"Game Over!"** title at the end (V2 21:30). **H**
- Name tags are coloured by team, with a team icon before the name (V2 4:45). **H**

## 6. Death, graves and spectating

- **Graves:** a dead player's items go into a **grave** at the death spot: a chest-like block with a grey headstone (V2 13:39). Anyone can open it; it is titled "<name>'s Grave" and holds the whole inventory as a 5-row container (V2 13:42, 16:34, 17:18, 18:27, 20:46). This is a key mechanic: kills pay out loot. **H**
- **Spectating:** dead players go into a free-flying spectator view of the live game (Wilbur's quadrant after V2 ~7:40; Philza in V3 after ~1:50). **H**

## 7. Islands, mechanics and the castle (priority 5)

- **Generator island:** a stone island with a working lava + water cobblestone generator, a small stone-brick hut holding the chest and the sign, plus a crafting table and furnace (V1 1:30–3:30). Its rock contains ore: coal, iron and redstone are visible when mining (V1 2:26). Iron ore is smelted in a furnace (V2 9:30 Philza: iron ore → iron ingot). **H**
- **Ship island:** dark hull with cobwebs, a mast and a lantern, at roughly start-island height (V2 6:05). **M**
- **TNT towers:** tall 1-block-wide-looking TNT columns with a chest on top, climbed by pillaring (V2 11:11, 11:38). TNT does not ignite on its own. **H**
- **Enchanting deck:** wood platform with bookshelves, enchanting table, anvil, chest and hay bale, connected by player bridges to the castle (V2 11:00–11:17). **H**
- **Central castle:** round stone-brick keep with red and orange/yellow striped segments, towers carrying red "MCC" banners, and a walled inner arena (V2 11:58 top view, spectator). Late game the inside is full of player-placed cobble, lava and water (V2 21:58, V3 1:57:10). **H**
- **Height:** the start islands and the ship sit at about the same height. The TNT towers rise roughly 15–25 blocks above the islands. The castle towers are the tallest objects. **L**

## 8. End of game (priority 6)

- **One team left → immediate end.** "Game Over!" title, +150 to the surviving team's living players, results in chat, and the sidebar switches to "Back to Hub: 00:45" (V2 21:30; V3 1:57:10, where 1 team / 4 players were left with about 2 min still on the clock). **H**
- MCC 4 ended at ~1:29 left during LAST STAND; MCC 1 ended at ~1:30 left. Neither game reached 0:00. **H**
- **Time-out with several teams alive:** **not observed**. Still unknown.

---

## 9. Answers to the open questions in `skyblockle_rules.md` §7

| # | Question | Answer | Conf. |
|---|---|---|---|
| 1 | Starting kit | None. Inventory is empty; one start chest per team (contents §1 #1) | H |
| 2 | Classic chest contents | Partly answered: §1 and §2 (start, generator, hidden, TNT tower, ship, red islet, enchanting deck) | H/M |
| 3 | Border | First shrink at 7:00 elapsed, then every 2 min, last at 1:30 left, followed by a 30 s "LAST STAND". Particle wall. Damage and size still unknown | H / – |
| 4 | Time-out with several teams | Not observed | – |
| 7 | Generator mechanics | Pre-built and working; a spare water and lava bucket are also supplied | H |
| 8 | Enchanting / brewing | Enchanting table, bookshelves and anvil on the deck; XP comes from Bottles o' Enchanting; books are level I. Brewing not seen | H / – |
| 9 | Grace period | None announced or shown | M |
| 11 | Spectating / item drops | Dead players spectate; items go into a lootable grave | H |

## 10. Still unknown

1. The contents of the **fort, brewing deck, between-team grass island and the four castle chests** in the classic version. None were opened on camera in these POVs.
2. Exact IDs of a few items: the orange-brown block ×6 in the start chest, the seeds ×3, the red mushroom-like item, the green ×8 blocks, the orange orb.
3. **Border** damage per second, its shape (circle or square), its final radius, how fast each step moves, any vertical limit, and what happens when LAST STAND reaches 0.
4. **Time-out rule** when two or more teams are still alive at 0:00.
5. How many chests each island type has and how many TNT towers, ships and red islets each team wedge gets (only one team's path was followed).
6. Whether chests refill (assumed no), and whether the grave has a timeout or despawns.
7. Melee friendly fire, and kill credit for void knock-offs (the death message shows "fell out of the world" with no attacker in the one example seen).
8. Whether players are frozen during the pre-game countdown.

Good next sources for item 1: CaptainSparklez full event `-ZphLShaUfc` / `pKhf8xUuzCc` (MCC 22, too late for classic), PeteZahHutt MCC 3/7 VODs `29gSvPqXg0o` / `tLTGhWBDuV0`, Quig's MCC 8 POV `bmk8c1eAyC0`, and JackSucksAtLife's MCC 1 POV `BxRcgxD9MJc`. Look for players who rush the fort and the castle.
