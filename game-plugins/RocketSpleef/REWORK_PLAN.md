# Rocket Spleef rework plan: old MCC Rocket Spleef (MCC 1–13)

**Target changed on 2026-11-25.** The owner wants the **original** Rocket Spleef (season 1), not Rush. So there is no
platform chain, no Rush pieces, no ghost previews, no Updraft and no elytra durability. The earlier Rush plan is
replaced by this one.

**Facts and sources:** `game-plugins/research/mcc_archive/rocket_spleef_old/notes.md` gives every number with its
source and confidence. The owner's reference images are `frames/wiki/{Nanotube,Rings,Easter_Eggs}.png` in that folder.

## Target

| Area | Old MCC (target) | Conf. |
|---|---|---|
| Rounds | 3 rounds, one map each: Nanotube, Rings, Easter Eggs! | H |
| Round | 4:00. The round ends early when one team is left | H |
| TNT Time | From 2:00 to 4:00. The firework is removed, and TNT falls onto solid blocks | H (rate L) |
| Terrain loss | Only from rockets and TNT, with no scripted decay | M |
| Launcher | A shovel with the purple enchant glint that shoots fireballs. Clip of 3, refilled one at a time | H (shovel, clip) / L (timings) |
| Blast and knockback | Removes a small sphere of blocks and pushes **everyone**, teammates included. No kill credit for a teammate | owner |
| Rocket jump | Point-blank, about 9 blocks, set as a height in blocks in the config | L |
| Firework | 1 per round, a vanilla elytra boost toward where you aim | H |
| Elytra | Unbreakable | H |
| Jump Boost | None (removed in MCC 8) | H |
| Punching | On: no damage, vanilla knockback, and the punch counts as the last hit | owner |
| Scoring | **PLACEHOLDER** at MCC coins ÷ 5: outlive 0.6, last-hit elimination 2, sole-team survival 20. The "+30 players remain" bonus is skipped | owner |

**Cannon colour:** the owner asked for green "unless old MCC looked different". Old MCC used a purple-glint shovel, so
we follow old MCC and say so in the README.

## Code

- **Pure-Java map layer** (`map/`, no Bukkit): `Canvas` (String block ids, sparse), with `NanotubeMap`, `RingsMap` and
  `EasterEggsMap`, each sized 1:1 for 40 players with at least 40 spawn points. `tools/PreviewMain` writes `.mcmdump.gz`,
  and `tools/render.sh` renders `previews/*.png`.
- The game plugin pastes a canvas into the void world `mcc_rocketspleef`. Phases: COUNTDOWN (5 s, frozen),
  ACTIVE, TNT_TIME, INTERMISSION. Every number lives in `config.yml`.
- **Delete:** the 4 invented maps and their previews, `ArenaMaps`, `RsCanvas`, the imported-arena loader (only 3 maps
  are wanted), `VoidGenerator`, sky-pads, the red crumble and the updrafts.

## Test (own staging only: `scratchpad/staging-rs`, port 25577)

A mineflayer bot test covers:

- 3 rounds on the 3 maps;
- a rocket removes blocks and knocks a player back;
- rocket-jump height in blocks;
- TNT Time starts at 2:00, the firework is removed and TNT falls;
- the placeholder points;
- the elytra is still unbreakable at the end.

Renders and side-by-side sheets go in `previews/` and `rocket_spleef_old/compare/`.

## Still open

The L numbers (reload, shot gap, rocket speed, blast size, jump height, TNT rate) need frames from an MCC 11–13 VOD.
No footage was downloaded.
