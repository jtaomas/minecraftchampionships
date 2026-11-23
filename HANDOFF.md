# Handoff — where this branch stands

This repository root **is the server folder** (Paper 1.21.1, Java 21). Clone the `mcc-1.21` branch, run `start-mcm-server.bat`, join `localhost`. Ops get an **Admin Tools feather** (or `/mcm tools`): fast-test mode, skip wait, skip round, pick map.

## Two tournaments on one server
- **Minecraft Monday** (original lobby, compass): the 7 built-in games + Find the Button.
- **Block Showdown** (MCC-style; `mcc_lobby` hub via the compass's "MCC Lobby" button): the game plugins in `game-plugins/`.

## State of each game
| Game | State |
|---|---|
| Battle Box | v2 round 2 (3 distinct arenas) — installed, awaiting owner playtest #3 |
| Find the Button | built, installed, not yet playtested |
| Sky Blockle | built with researched loot/border/graves, installed, not yet playtested |
| Parkour Warrior | rewrite built: single course, 5 zones + 3 endings, 36 obstacles — installed, loads and builds on the server, not yet playtested |
| To Get To The Other Side | rewrite built: 8 maps (bridge, climb, parkour, drop, elytra, boats, turnstiles, push+bridge) — installed, all maps load on the server, not yet playtested |
| Foot Race, Rocket Spleef, Hole in the Wall, Sands of Time, Parkour Tag, Dodgebolt, Grid Runners, Bingo/Lockout, Decision Dome | earlier builds, installed; not yet through the research → spec → build pipeline |
| Ace Race, Build Mart, Meltdown, Railroad Rush, Sky Battle | shelved (code kept, jars in `game-plugins/_built/_shelved/`) |

## How the work is organised (read these first)
- `game-plugins/PIPELINE.md` — research → spec (owner approves) → build → verify → playtest → feedback.
- `game-plugins/LESSONS.md` — mistakes not to repeat. `game-plugins/BRIEF.md` — plugin API contract and admin testing hooks.
- Per game: `game-plugins/<Game>/SPEC*.md`, `FEEDBACK_*.md`, `README.md`; research in `game-plugins/research/`.
- Core plugin source: `plugin-src/` (build with `mvn install` in `plugin-build/`; the games compile against that installed core (`com.mcm:MinecraftMondayCore:3.2.0`), so build the core first). Game plugins: `mvn package` in each folder; jars go to `game-plugins/_built/` and then `plugins/`.
- Map previews: `game-plugins/<Game>/previews/`; offline renderer `tools/render_preview.py`; bot test harness `tools/bot-harness/`.

## Known to-do
- Finish and install Parkour Warrior and To Get To The Other Side; bot-test and playtest the new builds.
- Start-of-game freezes (2.5–3.5 s) in Grid Runners, Sands of Time, Lockout.
- Sky Blockle: hidden generator chest not placed by the map yet; border is square.
- Resource pack (`resourcepack/`) is built but not wired into the core or hosted.
- Local scripts contain machine-specific paths (shown as `C:\Users\USER\...`): set your own Java/Maven paths.

## Not in the repository
Third-party plugin sources and jars that were only studied for reference (including one project with no licence) are not redistributed here. Nothing in this repository depends on them.
