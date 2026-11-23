# Minecraft Monday Championship Server (Paper 1.21.1)

Paper 1.21.1 tournament server featuring the custom **MinecraftMondayCore** plugin (`plugin-src/` and pre-built `plugins/MinecraftMondayCore.jar`), supporting plugins (`Multiverse-Core`, `OldCombatMechanics`, `TAB`, `ViaVersion`, `ViaBackwards`, `ViaRewind`), and all tournament/minigame worlds (`world`, `world_nether`, `world_the_end`, `dropper_world`, `oneshot_world`, `parkour_world`, `pvp_world`, `sg_world`, `sg4_world`, `spleef_world`, `survival_world`, `tntrun_world`).

## Prerequisites

- **Java 21** (e.g., Eclipse Temurin 21 from https://adoptium.net/)

## Setup & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/jtaomas/minecraftchampionships.git
   cd minecraftchampionships
   ```
2. Start the server:
   - **Windows:** run `start-mcm-server.bat`
   - **macOS / Linux:**
     ```bash
     java -Xms2G -Xmx6G -jar paper.jar nogui
     ```

> **Note:** Paper automatically downloads and recreates `cache/`, `libraries/`, `versions/`, and `plugins/.paper-remapped/` on first launch.

## In-Game Commands

- `/mcm [start|play <game>|skip|stop|reset|teamsize <1-5>]` (aliases: `/mcc`, `/tournament`) — Open the Minecraft Monday Tournament Controller or run admin commands.
- `/team` (alias: `/teams`) — Open the Team Selector GUI.
