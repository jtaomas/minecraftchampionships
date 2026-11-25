# McmRocketSpleef: the original MCC Rocket Spleef (MCC 1–13)

This is the **old** Rocket Spleef, not Rush. Every number and its source and confidence are in `src/main/resources/config.yml` and in
`game-plugins/research/mcc_archive/rocket_spleef_old/notes.md`.

## Game

- **3 rounds** of **4:00**, one per map, in this order: **Nanotube**, **Rings**, **Easter Eggs!**. Each map is generated at 1:1 scale for about 40 players.
- **Round flow (measured from MCC 13 VODs):**
  - 15 s in a drop room above the map, with "Dropping in 10…1" titles; then the floor opens and everyone drops onto the map;
  - 2:00 of play, a 10 s chat warning, then TNT Time (night sky) for the last 2:00;
  - "Round Over!", then 10 s "Next Round".
- **Kit:**
  - an iron shovel with the purple enchant glint, the *Rocket Launcher*: right-click fires a fireball rocket, and a melee hit with it has extra knockback;
  - an **unbreakable elytra**;
  - **one firework** per round, a vanilla elytra boost that only works while gliding.
- **Clip:** 3 shots, but you **start each round at 0** and reload one rocket every ~2.1 s. Firing restarts the reload bar. The XP level shows the shots left, and the XP bar shows the reload.
- **Rockets:**
  - fly straight and fast;
  - remove a small sphere of blocks;
  - knock back **every** player nearby, teammates included.
  - A point-blank blast under your feet launches you **9 blocks** high (`launcher.rocket-jump-height`).
- **Punching:** on. A punch does no damage, gives vanilla knockback and counts as the last hit.
- **TNT Time:** from 2:00 to the end of the round. Fireworks are removed, and TNT falls onto the map's top blocks. TNT gives no kill credit.
- **Terrain:** lost only to rockets and TNT, with no scripted decay.
- **Elimination:** falling 10 blocks below the map's lowest block. The last hitter within 10 s gets the credit, but never a teammate.
- **Round end:** when one team is left, or at 4:00.
- **Scoring: real MCC coins** (old Rocket Spleef, final MCC 12-13 values; config `coins.*`, see `../COINS.md`):
  - outlive a player: 3;
  - elimination (last hit): 10;
  - surviving the round when only one team is left: 100 to each survivor.
  - The core multiplies every award by the event multiplier. Every award is logged as `[coins]`.

**Launcher colour:** the owner asked for green "unless old MCC looked different". Old MCC's launcher was a **shovel with the purple enchant glint** (promo art, `frames/wiki/RS-Website.png`), so we follow old MCC.

## Admin

| Command | What it does |
|---|---|
| `/mcm play rocketspleef` | Start a game |
| `/mcm map <nanotube\|rings\|easter_eggs>` | Choose the first map |
| `/mcm skipwait`, `/mcm skip`, `/mcm fast` | Core controls |
| `/rs show <map>` / `/rs clear` | Paste a map (no game running) and fly around it, then remove it |
| `/rs tnt` | Jump to TNT Time now |
| `/rs reload` | Reload `config.yml` |
| `/rs debug` | Print the state line |

## Maps (pure Java, no Bukkit)

- `map/NanotubeMap`, `RingsMap` and `EasterEggsMap` write into `map/Canvas` (String block ids).
- **Offline previews:** `sh tools/render.sh [mapId]` renders to `previews/`.
- **Reference images:** `research/mcc_archive/rocket_spleef_old/frames/wiki/*.png`.
- **Comparison sheets:** `research/mcc_archive/rocket_spleef_old/compare/`.

The old invented maps, the imported-arena loader, sky-pads, updrafts and red crumbling were removed.
