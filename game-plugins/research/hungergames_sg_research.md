# Survival Games / Hunger Games — Modern MCC Design Research (`McmSurvivalGames`)

## Sources
- MC Championship Wiki (mcchampionship.fandom.com) — *Survival Games*, *City Living*, *World of Cars*
- Community analysis of modern MCC Survival Games scoring, infinite XP enchanting, airdrops, and border pacing

---

## 1. Modern MCC Survival Games Mechanics (1.21)
- **Equal-Footing Start (No Kits):** Unlike legacy kit-based SG servers, modern MCC Survival Games starts every player on equal footing so map knowledge, looting, routing, and PvP skill decide fights.
  - Starter utility: Player-Tracking Compass (points to nearest enemy with action-bar distance), light starter food, team-coloured leather boots, and an **Oak Boat** on water/coastal maps (`harbour_citadel`, `woodland_basin`) as a fast traversal tool and nod to Minecraft Monday.
- **Infinite XP Levels & Enchanting Landmarks:** Players receive level 30 experience (`player.setLevel(30)`) at the start and regain level 30 immediately after enchanting. Each map includes landmark Enchanting Tables surrounded by bookshelves + Lapis Lazuli in chests, encouraging mid-game gear upgrades.
- **Tiered Chest Loot:**
  - *Tier 1 (Outer POIs, houses, ships, upper floors):* stone/iron melee weapons, bows, crossbows, arrows, shields, leather/chain/iron armor pieces, steak/bread/golden carrots, lapis lazuli, cobwebs, oak planks/cobblestone for quick bridging/cover.
  - *Tier 2 (Central Cornucopia & major landmarks):* iron/diamond gear, golden apples, splash healing/swiftness potions, enchanted books/items, ender pearls.
  - *Tier 3 (Supply Airdrops):* diamond weapons & armor, multiple golden apples, splash instant health II, power/sharpness gear.
- **Shrinking WorldBorder & Border Damage:** The border starts at the arena perimeter and smoothly shrinks toward the Cornucopia over the round, dealing border damage outside to eliminate camping and drive fights together, culminating in a Cornucopia Deathmatch in the final 60s.
- **Glowing Supply Airdrops:** Announced 15 seconds ahead with exact `(x, z)` coordinates, particle beacons, and fireworks; spawning a glowing Supply Crate (`BARREL` on a sea-lantern/gold marker) containing Tier-3 gear plus a shared team point bonus (`+24` split across the capturing side).
- **Modern 1.21 Combat:** `mcc_hungergames: [ "vanilla" ]` is configured in `plugins/OldCombatMechanics/config.yml`, disabling 1.8 spam-combat overrides cleanly.

## 2. MCC-Scaled Scoring (50–150 pt MCM Target)
- **Elimination:** `+10` points to the killer.
- **Passive Survival:** `+1` point to every surviving player whenever another player is eliminated.
- **Supply Airdrop Capture:** `+24` points shared across the capturing side.
- **Team Placement Bonus (shared across side):**
  - 1st (Last Side Standing / Top surviving team): `+40`
  - 2nd: `+30`, 3rd: `+22`, 4th: `+16`, 5th: `+12`

## 3. Map Archetypes (Original Builds, ~125-block radius)
1. **`city_district`** *(MCC City Living inspired)*:
   - Urban road grid (`GRAY_CONCRETE` roads, yellow lane dashes, stone-slab sidewalks, streetlamps) around a central Civic Plaza Cornucopia (tiered stone/quartz monument, 12 Cornucopia chests, 24 spawn pedestals).
   - 16+ varied multi-storey buildings (brick, terracotta, sandstone, deepslate, concrete palettes) with doorways, glass windows, interior floors, real staircases to upper floors/rooftops, side alleys, a central park with custom trees, and an Enchanting Library.
2. **`woodland_basin`** *(MCC World of Cars - Forest/Farm inspired)*:
   - Multi-octave value-noise heightmapped rolling terrain, shallow central valley, winding river with arched timber bridges, dirt trails, custom oak/spruce trees, and stone boulders.
   - Central Stone Ruins Cornucopia (24 pedestals + 12 chests) surrounded by distinct landmarks: Red Barn & Farmhouse, Lumber Mill, Stone Watchtower (walkable spiral/switchback stairs), Ruined Chapel (Enchanting Table + bookshelves), and Riverside Docks.
3. **`harbour_citadel`** *(Coastal/Waterways SG)*:
   - Terraced stone harbour Cornucopia, navigable canal/harbour channels with bridges (making Oak Boats tactical), 3 multi-deck docked ships (hulls, masts, sails, cabins, gangplanks), Lighthouse Tower, Dockside Warehouses & Market Stalls, and Arcane Study (Enchanting Table).
