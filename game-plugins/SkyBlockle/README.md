# Sky Blockle (McmSkyBlockle)

Game id `skyblockle`, display name "Sky Blockle". Start it with `/mcm play skyblockle`. World: `mcc_skyblockle` (void world, created at plugin enable; the old folder is deleted once per server start).

This is the classic 18-minute version (see `SPEC.md`). The mechanics (this README) and the map generator (`com.mcm.games.skyblockle.map`, built by a separate agent) live in the same plugin.

## Rules
- There is one round of 18:00 (`game-seconds`). Each side gets its own wedge of the map. The map has as many wedges as there are sides, up to 10. With more than 10 sides, some sides share a wedge's start island.
- Before the game: the map is generated (async, pure Java), the old map is wiped and the new one is placed. Then players are teleported to their start island and see a title card and a frozen countdown.
- Normal survival skyblock: break, place, craft, mine, brew and enchant. No infinite blocks and no fixed kit. Blocks drop normally and chests open normally. Cobblestone generators are plain vanilla lava and water. Fluids are re-placed with physics after the build so they work. The build height is capped at the highest map block + `map.build-height-above-map`.
- **One life.** Any death (melee, bow, fall, lava, border, void...) eliminates the player. The player becomes a spectator, with free flight kept within the map's radius and height range. Their items go into a lootable grave at the death spot (void deaths: none). There are no respawns. Quitters count as eliminated. Late joiners and rejoiners spectate.
- Falling more than `death.void-depth` blocks below the lowest map block is a void death.
- **Grace:** none by default (`grace-seconds: 0`, per video). When set, PvP is off for that long after GO.
- **Friendly fire is always off.** This covers melee, projectiles, TNT the player lit, and harmful splash or lingering potions.
- **Kill credit:** the last enemy who damaged the victim within `kill-credit-seconds` (10 s) gets the kill, whatever the cause of death (including the void and the border).
- **Border:** a vanilla world border (visible, square) is centred on the castle. It follows `border.points`: [seconds after GO, half-size], linear in between. Our own damage applies outside it (`border.damage`, damage per second over time, armour-bypassing `outside_border` damage). The vanilla border damage is set to 0.
- **End:** the game ends when only one side has living players (or when nobody does), or at 18:00. At a time-out, surviving sides are ranked by `timeout-tiebreak`. Sides equal on every criterion are a draw. After `end-delay-seconds` of celebration, `ctx.finish` is called exactly once and the core returns everyone to the lobby.
- **Cleanup:** every chunk the map or a player touched is tracked. This includes placements, fluids, cobble forming, trees, falling blocks, explosions, pistons, dispensers and players' own chunks. Those chunks are wiped after `finish`/`stop` and again before the next build. Each game therefore places a completely fresh map, with a new seed unless `map.seed` is set. Non-player entities (dropped items, arrows, XP) are removed.

## Scoring (classic 70 / 10 / 150 scaled)
| Event | Points |
|---|---|
| Kill (enemy) | 14 to the killer (`points.kill`) |
| Any player dies | +2 to every other living player (`points.survive-death`) |
| Alive at the end (last side or time-out) | +30 each (`points.alive-at-end`) |
| Team kill | 0 (`points.team-kill`) |

## HUD
- Boss-bar timer (`ctx.setTimer`).
- Sidebar: Next Shrink m:ss (or LAST STAND m:ss), Players Alive x/n, Teams Alive x/n, border half-size, top killer (plus a PvP line if a grace is set).
- Action bar: your kills, the grace countdown, a border warning within `border.warn-distance`, and a red "outside the border" warning with the damage rate.
- `McmFx`: title card, countdown, elimination and team-eliminated feedback, a game-over title and the winners' celebration.

## Loot
`loot.yml` (copied to the plugin folder) has one table per island type: `start`, `generator`, `mid`, `fort`, `brewing`, `enchanting`, `castle`. Each table has `rolls`, `fixed` items and a weighted `items` pool. Items support amounts, enchantments (stored on enchanted books), potion types and names. Every chest registered in the map's `MapModel.chests` is filled from its island type's table, into random empty slots. Every brewing stand block in the map is filled from the `brewing_stand` table (slot-based). `/sbl loot <type>` opens a sample roll.

## Map interface
`MapModel` (owned by the mechanics side) is the pure-Java contract: a sparse block store, spawns per wedge, chest spots with island types, team markers that are recoloured at runtime, castle centre, arena/castle/map radii and ring radii. The generator entry point is `com.mcm.games.skyblockle.map.SkyMap.generate(int wedges, long seed)`. `map.generator: auto` uses SkyMap if the class is in the jar, otherwise the built-in stub (`stub.StubMap`, plain islands, for testing only). `skymap` and `stub` force one or the other. The runtime re-measures minY/maxY/mapRadius from the blocks.

## Admin commands (`/sbl`, op)
`build [wedges] [seed]` previews a map outside a game. `tp` goes to the castle. `info` shows the map, chest counts and resolved border schedule. `loot <type>` rolls a chest. `clear` wipes the map. `reload` reloads the config and loot (both apply from the next game).

## Research status (video research `research\skyblockle_video.md` applied 2026-11-24)
**Updated from the video (H unless noted):**
| Item | Now | Where |
|---|---|---|
| Start inventory | empty | config `start-inventory` |
| Chests | **fixed, hand-arranged layouts** (`slots`, keys `r<row>c<col>`), not random. Observed: start, generator hut, generator hidden chest (M), pillar/tower top, spike/red islet (M), showpiece/ship (M), stone mid structure (generic `mid`, L/M), enchanting deck. A few item IDs are unconfirmed: terracotta, wheat seeds, red mushroom, flint, melon for the "green block", fire charge for the "orange orb" | loot.yml |
| MID chest variants | `mid_pillar` / `mid_spike` / `mid_grass` are inferred from the block under the chest. `generator_hidden` and `mid_showpiece` apply only when the map sets `ChestSpot.variant` | loot.yml + `MapWorld.inferVariant` |
| Generator ores | coal, iron and redstone ore embedded in the generator island's rock (video #5 and §3 line 113: seen while mining; iron ore is smelted in a furnace) | `map.Islands.genOres` |
| Hidden generator chest | one chest **buried inside the rock of every generator island** (video #5, M: found while mining the generator rock), registered as GENERATOR + variant `hidden` -> `generator_hidden` (potato, book, XP bottle; slots as seen, M). Not visible from outside; players dig to it | `map.Islands.generator`, loot.yml |
| Border timing | first shrink 7:00, then every 2 min (9, 11, 13, 15), last at 16:30 (1:30 left), then a 30 s **LAST STAND** in the sidebar. Red "Border shrinking" subtitle, red chat warning 30 s before | config `border.points`, `last-stand-*` |
| Border look | cyan particle wall drawn at the border near each player. The vanilla world border still enforces | config `border.particles-distance` |
| Deaths | the whole inventory goes into a lootable **"<name>'s Grave"** (5-row, right-click; breaking it spills it). Void deaths: nothing | config `death.grave` |
| Grace | none (M) | config `grace-seconds: 0` |
| End / HUD / chat | ends as soon as one side is left; "Game Over!" title. Sidebar: Next Shrink / LAST STAND, Players Alive, Teams Alive, border, top kills. Chat: "[+2] X fell out of the world", "[+14] You killed X!", "Only N players remain!", "<team> eliminated", "[+30] You and your teammates were the last players standing!", per-player place / eliminations / points summary | code |

**Still PENDING_RESEARCH (provisional):**
| Item | Provisional value | Where |
|---|---|---|
| Border step radii, step duration, final size | map+8 → start+10 → fort+10 → brewing+8 → enchanting+6 → castle+4 → arena; each step moves for 30 s | config `border.points` |
| Border damage | 1/s rising to 4/s | config `border.damage` |
| LAST STAND at 0 | nothing extra; the game runs to 18:00 | config |
| Time-out with several sides | players-alive, then kills | config `timeout-tiebreak` |
| Graves despawn | never | — |
| Countdown freeze | movement frozen (video: players may have walked, M) | code |
| "Only N remain" thresholds | 10, 5, 3, 2 | config `announce-remaining` |
| Chest refills | none (L) | config `loot.refill-at` |
| Kill credit for void knock-offs | last enemy hit within 10 s | config `kill-credit-seconds` |

**Defaults, not researched (never opened on camera):** `fort`, `brewing`, `mid_grass` (between-team grass island), `castle`, `brewing_stand`. These keep weighted provisional tiers in loot.yml.

**Hidden generator chest: defaults, not researched.** The research only says the chest is inside the generator island's rock. Its exact spot is our choice: generator-local (-1, -3, +2) = 1 block to the side and 2 blocks behind the basin centre (away from the start), 3 blocks below the floor surface (4 below standing height), not under a lava/water cell (so digging to it does not drain the generator) and well inside the tapered underside (the rock there is 4-5+ deep, so the silhouette is unchanged; any missing neighbour is filled with the island's own rock). It is the same template cell on every wedge. It is a map-template constant in `Islands.generator`, not a config key (the pure-Java generator has no config access). The book is a plain `book` because the enchantment (if any) was not readable (loot.yml `generator_hidden`, editable). Opening needs the block above the chest removed (vanilla chest rule); digging down from the top does that.

**Generator ores: defaults, not researched.** The research names the ore types only (coal, iron, redstone), not counts or positions. Counts are named constants in `Islands`: `GEN_COAL_ORE = 4`, `GEN_IRON_ORE = 4`, `GEN_REDSTONE_ORE = 2` per generator island. Each ore replaces a plain rock block (stone/andesite/cobblestone/tuff/mossy cobblestone) at generator-local y -5..-2 that is fully enclosed (all 6 neighbours are blocks, so nothing on the outside or the silhouette changes), not in a column under the lava/cobble/drop/water cells, not the hidden chest or one of its 6 neighbours, and at least 2 blocks from other ores (relaxed to 1 only if the rock is too small). Which eligible cells are used follows a seeded hash of the map seed, so positions differ per game but are identical on every wedge. The validator checks the counts, the enclosure and the basin rule on every copy. Preview: `previews/generator_ores_xray.png` (rock below the basin removed; ores drawn as coal/raw-iron/redstone blocks because the shared renderer draws every ore as plain stone).

## Acceptance checklist mapping
1. **Start:** `start()` maps sides to wedges (`i % wedges`) and teleports each side to `MapModel.spawnsFor(wedge)`. With one side the game does not end on "last side" (`totalSides >= 2`). It ends on death or time-out.
2. **Break/place/craft:** players stay in SURVIVAL. The listener only cancels for non-alive players, outside LIVE, or above the build cap. Drops and chests are vanilla.
3. **Loot:** `MapWorld.fillContainers` fills each `ChestSpot` from `loot.yml` `<type>`. The castle table is the diamond tier.
4. **Cobble generators:** vanilla fluids; `map.fluid-physics` re-places water and lava with physics after the build.
5. **Grace / friendly fire:** `GameListener.onDamage` cancels PvP while `!pvpOn()` and always between teammates (projectiles, TNT and potions included).
6. **One life:** `onLethal` cancels the lethal hit and calls `eliminate`, which puts the inventory into a lootable grave (`Graves`; none for void deaths), sets spectator mode and never respawns. `/kill`-style deaths are caught by `onDeath`/`onRespawn`.
7. **Scoring:** `eliminate` awards 14 to an enemy killer, 0 for a team kill, and +2 to every other living player.
8. **Border:** `updateBorder` drives the vanilla world border per segment (`setSize(size, seconds)`). `borderDamageAndHud` deals the scheduled damage outside it.
9. **End:** `checkEnd` and the time-out lead to `endGame` (+30 to survivors, tiebreak, celebration). `finishOnce` is guarded by the `finished` flag.
10. **Stop/cleanup:** `stop()` is idempotent. It cancels every task, the build queue and the clear task, resets players and the border, and wipes the dirty chunks. The next game wipes again and places a fresh map.
11. **Bridging gaps / identical wedges:** this is the map generator's job (its validator reports min/max gaps). The "every chest reachable" validator also covers the hidden generator chests: each must be fully enclosed by diggable blocks and reachable from the walkable island surface by breaking blocks only (no fluids, void air or unbreakable blocks on the way; currently 3 blocks). Preview: `previews/generator_hidden_chest.png` (wedge 0 generator cut open through the chest).
12. **Quitters:** `onQuit` calls `eliminate`, followed by the usual last-side check.

## Known limitations
- The vanilla world border is square. The map is round, so the corners of the square reach further out than the circle.
- The core sidebar is shared by everyone, so "your kills" is shown on the action bar instead.
- `SkyMap` requires 2-10 wedges, so `map.min-wedges` is 2: a solo test game gets a 2-wedge map with one wedge unused. If SkyMap throws, the game falls back to the plain stub map.
- SkyMap places no team markers (research: no team colours on the map). The `TeamMarker` hook stays unused.
- SkyMap has 4 spawns per start island. Larger sides cycle onto the same spots.

## Admin testing
- `/mcm skipwait` ends the pre-game freeze/countdown (the game starts now), the PvP grace period (PvP on now) or the end celebration.
- `/mcm skip` does nothing: Sky Blockle is a single round.
- `/mcm fast on`: the intro before the countdown, the countdown and the end celebration are at most 1 s. The grace period and the border schedule are NOT shortened.
