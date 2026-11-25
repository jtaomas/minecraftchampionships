# City Marathon — server verification cross-reference (A12)

Read-only cross-check between A2's NBT table and A12's live-server command audit.
Tags: **H** observed, **M** inferred from two sources, **L** untested.

## 1. A12 result

A12 reports **7 command-block failures** on `mcc_citymarathon`, all of them no-target `/gamemode adventure` blocks, failing with `permissions.requires.player` — **H** (A12 message). No other command failure was reported — **M** (A12's list is complete for the 7).

## 2. The seven blocks

| A2 ID | Coords (x,y,z) | Block / mode | Exact command | Intended effect | Actual effect |
|---|---|---|---|---|---|
| CB03 | 11,−2,−6 | repeating | `gamemode adventure` | force adventure | none; fails (no player target) |
| CB05 | 11,−2,−4 | repeating | `/gamemode adventure` | force adventure | none; same failure |
| CB11 | 7,−3,3 | chain | `gamemode adventure` | force adventure at start | none; chain also never fired |
| CB30 | −387,−8,45 | repeating | `gamemode adventure` | force adventure at CP3 | none; same failure |
| CB31 | −387,−8,47 | repeating | `/gamemode adventure` | force adventure at CP3 | none; same failure |
| CB52 | −208,−2,86 | repeating | `/gamemode adventure` | force adventure at CP2 | none; same failure |
| CB53 | −208,−2,87 | repeating | `gamemode adventure` | force adventure at CP2 | none; same failure |

**Independent NBT evidence:** all seven have `SuccessCount=0` in the saved block-entity NBT — **H** (`data/block_entities.json`). The two chain blocks also have no `LastExecution` — **H**. The other 60 commands are not in A12's failure list — **M**.

## 3. Why it fails

`/gamemode <mode> [targets]` needs at least one player target. Without a selector, the default target is the **command source**. A command block is not a player, so the command fails with `permissions.requires.player` — **H** (A12 live result; same behaviour in 1.19.2 and 1.21).

The leading slash is **not** the problem: CB05/CB31/CB52 use `/gamemode` and fail the same way as CB03/CB30/CB53. In this world the leading slash was accepted by command blocks (the start `/fill` and `/title` commands have `SuccessCount=1`) — **H**.

## 4. Multiverse / survival implication

- Multiverse applies a **per-world gamemode** when a player joins/changes world. The imported world's `level.dat` `GameType=1` (adventure) is the world default, not a per-join override — **M**.
- The seven blocks are the map's only attempt to force adventure. If Multiverse's per-world gamemode is `survival`, players will be in **survival**: they can break/place blocks, edit the course, pick up items, craft, and use survival inventory rules. `keepInventory=true` and `fallDamage=false` reduce death loss but do not prevent editing — **M**.
- The visible `plugins/Multiverse-Core/worlds.yml` on this checkout does **not** contain a `mcc_citymarathon` entry (read-only grep; **H**). Either the world is configured in another file/server instance, or it is not currently imported there. The live per-world gamemode therefore needs A12/coordinator confirmation — **open**.
- If the world is run in survival, the **release gate** (18 barrier blocks at z10) is also breakable, and the **train chassis** (1,035 barrier blocks at y4) is breakable. That defeats the map's physical bounding — **M**.
- Do not fix this in the research pass. Record the setting; the coordinator decides whether to change Multiverse's gamemode or add a player-target command — **plan rule**.

## 5. What to check next (read-only)

1. Query the live world's gamemode (Multiverse `/mv info mcc_citymarathon` or equivalent read-only command) — **H**.
2. Join and confirm whether the player is adventure or survival before pressing the start button — **H**.
3. Press the button at 8,0,8; confirm the release gate opens and the trident/titles fire — **H**.
4. Confirm the 7 blocks still fail and no new failures appear — **H**.
5. Do not change the world, the gamemode or the command blocks — **plan rule**.

## 6. Verdict

- **Command failure:** confirmed, exact, and already present in 1.19.2 — **H**.
- **Map consequence:** no in-map adventure enforcement; survival is the default failure mode — **H** for the command, **M** for the live setting.
- **1.21 consequence:** unchanged; no syntax upgrade fixes a missing player target — **H**.
