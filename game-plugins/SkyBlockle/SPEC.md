# Sky Blockle — spec (classic 18-minute version; owner-approved)

Game id `skyblockle`, display name "Sky Blockle", plugin `McmSkyBlockle`, world `mcc_skyblockle` (created at plugin enable). Research: `..\research\skyblockle_rules.md`, `..\research\skyblockle_maps.md` (an "Image-search pass" section is being appended), `..\research\skyblockle_video.md` (being written: loot seen in chests, spawn inventory, border, HUD). **Re-read all three before finalising your part; they override the PENDING values below.** Follow `..\LESSONS.md` — especially "No guessing".

## Confirmed
- One round, **18 minutes**. One life per player; dead players spectate (spectator mode, free flight within the map). Last side with a living player wins.
- Skyblock play: no infinite blocks, no fixed combat kit. Players mine, craft, loot chests, use cobblestone generators, brew and enchant. Normal survival rules inside the map (break/place/craft allowed) — this is the one game where the world is fully editable.
- Scoring (real MCC coins, classic 70 / 10 / 150; `../COINS.md`): **70 per kill**, **+10 to every living player each time another player dies**, **150 to each player alive at the end**. Team kills score nothing; friendly fire off.
- Border closes toward the centre and forces the endgame into the centre castle.
- Map structure: identical team wedges (one per side, up to 10; build only as many wedges as there are sides, evenly spaced) around a centre castle; tiny islands in rings on each team's line to the centre and on the lines between teams; no pre-built bridges; bridging gaps 13–30 blocks; loot improves toward the centre. Approximate ring radii from the research: castle ~20–25, enchant decks ~48, brew decks ~70, forts ~103, pillars ~119, starts ~135, generator and spike islands ~150, rock islands ~165. Island sizes: start ~6x6, generator ~8x8, forts ~11x11, castle ~45–50 across with 4 towers and a flat ~25-wide arena floor. Natural skyblock palette; red/yellow accents on the castle.
- Void = death. Credit the last attacker within 10 s.

## PENDING_RESEARCH (implement as config/data with these provisional values; do not hard-code; list them in the README)
- `start-inventory` (provisional: empty) and `start-island` contents (provisional: 1 oak tree, 1 chest, dirt).
- `loot-tables` by island type (`start`, `generator`, `mid`, `fort`, `brewing`, `enchanting`, `castle`) in `loot.yml`: provisional tiers — start: wooden tools, lava bucket, ice, food, saplings; mid: stone/iron tools, bow+arrows, blocks, food; fort: iron armour, shield, ender pearls; castle: diamond gear, golden apples, enchanted books. Loot placed in random slots unless research shows otherwise.
- `border` schedule: provisional — static until 6:00, shrinks linearly to the castle radius by 15:00, then to the arena floor by 18:00; damage outside rising from 1 to 4 per second. Config list of (time, radius) points.
- `grace-seconds` (provisional 60, PvP off).
- `timeout-tiebreak`: provisional — more players alive, then more kills.
- Generator, brewing and enchanting details (provisional: working vanilla cobble generator; brewing stands with a few ingredients; enchanting table with lapis and bookshelves).

## Map (our own designs on the confirmed structure)
- Pure-Java generator per `..\MAP_BRIEF.md` offline approach; islands are small, tapered underneath, natural palette, staggered heights; a few sculpted showpiece islands of our own invention between wedges for character; the castle is the hero build. Team colour marker on each start island (banner/concrete trim) so sides can find home.
- Everything players can reach must be legit skyblock terrain: real ores only where a real skyblock would have them; chests registered with an island type for the loot system.
- Validators: bridging-gap report (min/max gap along each team's path to the centre), every chest reachable, connection post-pass, staircase helper for any stairs, offline renders (whole map top-down + 2 angles, one wedge close-up, castle cut-away) in `SkyBlockle\previews\`.

## HUD
Boss-bar timer; sidebar: sides alive, players alive, border radius/next shrink, your kills; action bar warning when near/outside the border; `McmFx` title card, countdown, eliminations, team eliminated, winner celebration.

## Acceptance checklist
1. Game starts; each side is placed on its own start island; solo/1-player works.
2. Players can break, place and craft; blocks drop normally; chests open.
3. Each chest contains items from its island type's loot table; castle chests are the best tier.
4. Cobblestone generator islands produce cobblestone.
5. PvP is off during the grace period and on afterwards; friendly fire never works.
6. Dying (any cause, incl. void) eliminates the player: spectator mode, no respawn, items handled per research (provisional: drop at death point; void deaths drop nothing).
7. Kill gives 14 to the killer; every death gives 2 to each other living player; team kills give 0.
8. Border follows the configured schedule, is visible, and damages players outside.
9. Game ends when one side remains (30 to each survivor) or at 18:00 (tiebreak rule); `finish` once; players back in the MCC lobby.
10. `/mcm stop` cleans up; the map is fully regenerated next game (no leftover player builds).
11. Bridging gaps on each team's path are within 13–30 blocks; all wedges identical.
12. Quitters count as eliminated; last-side-standing logic still works.
