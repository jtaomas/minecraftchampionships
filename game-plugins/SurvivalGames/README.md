# McmSurvivalGames (`hungergames`)

Modern 1.21 MCC-style Survival Games / Hunger Games plugin for **MinecraftMondayCore** (Paper 1.21.1, Java 21).

## Commands
- `/mcm play hungergames`
- `/mcm map city_district`, `/mcm map woodland_basin`, or `/mcm map harbour_citadel`
- `/mcm skipwait` — skips pre-game countdown or grace period.
- `/mcm fast on|off` — cuts countdowns to <= 1s.

## Maps (3 Rich Procedural Arenas, ~124-block radius)
- `city_district` *(MCC City Living inspired)*: Urban road grid, Civic Plaza Cornucopia, 16+ multi-storey buildings with staircases & rooftops, Central Park, and Enchanting Library.
- `woodland_basin` *(MCC World of Cars - Forest/Farm inspired)*: Heightmap hills, winding river & bridges, Stone Ruins Cornucopia, Red Barn, Lumber Mill, Stone Watchtower, Ruined Chapel (Enchanting Table), and Riverside Docks.
- `harbour_citadel` *(Coastal/Waterways SG)*: Terraced Harbour Citadel Cornucopia, wide navigable canals & piers, 3 multi-deck docked ships, Lighthouse Tower, Dockside Warehouses, Market Stalls, and Arcane Study (Enchanting Table).

## Modern 1.21 Mechanics
- **Equal-Footing Start:** Player-Tracking Compass (action-bar distance to nearest enemy), 4 Cooked Beef, team-coloured Leather Boots, plus an Oak Boat on water-friendly maps (`woodland_basin`, `harbour_citadel`).
- **Infinite XP Level 30:** Players receive level 30 experience and regain it after using any map's Enchanting Table.
- **Tiered Loot, Shrinking Border & Airdrops:** Outer, Cornucopia, and Supply Airdrop loot tiers (1.21 armor, weapons, splash potions, enchanted books, cobwebs, building blocks); 15s-announced glowing Supply Airdrops; shrinking `WorldBorder` with border damage; mid-game chest refill; Cornucopia Deathmatch in the final 60s.
- **Modern Combat:** `mcc_hungergames: [ "vanilla" ]` enabled in `plugins/OldCombatMechanics/config.yml`.

## Scoring (MCC-Scaled, ~50–150 pts top player)
- **Elimination:** +10 pts
- **Passive Survival:** +1 pt per elimination while alive
- **Supply Airdrop Capture:** +24 pts (shared across side)
- **Team Placement:** +40 / +30 / +22 / +16 / +12 (1st–5th, shared across side)

*(Exact timings, border radii, and point values are configurable in `config.yml` as sensible defaults.)*
