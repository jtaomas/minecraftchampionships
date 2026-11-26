# MCC animations and feedback inventory (Block Showdown)

Read-only research, 2026-11-25. Goal: a 1:1 replica of MCC's on-screen moments using the real MCC assets in our pack. One row per moment: what MCC shows, how it moves, what it sounds like, where the evidence is, what our code does now, and a status.

Coin pop-ups are **not** covered here beyond one pointer row. See `game-plugins/COIN_FEEDBACK.md` (written by another agent).

## 0. How to read this file

**Path shorthand** (all under the repo root `C:\Users\LeonS\Desktop\Mcc`):

| Short | Full path |
|---|---|
| `A/` | `game-plugins/research/mcc_archive/` |
| `PK/` | `A/resource_pack/event/downloads/mcc_java_pack_1_18/assets/` (the MCC Island Java pack, 1.18 era; a fan repack, not from Noxcrew) |
| `EV/` | `A/sounds/event/` (69 OGGs from the MCC x Minecraft 15th Anniversary Party server; pack keys `mcc.*`) |
| `MI/` | `A/sounds/mcci/` (65 OGGs from MCC Island; **not in our pack yet**) |
| `EX/` | `game-plugins/research/existing_maps/` |
| `McmFx` | `plugin-src/com/mcm/api/McmFx.java` |
| `McmHud` | `plugin-src/com/mcm/McmHud.java` |
| `Core` | `plugin-src/com/mcm/MinecraftMondayPlugin.java` |

**Evidence labels**

| Label | Meaning |
|---|---|
| **H** | Seen directly: a frame I opened myself (8 frames: Build Mart title card and build-complete, GR room-complete crop, Dodgebolt victory banner, Sky Battle round card and border banner, SoT boss-bar crop, PT `Tagged!`), or a file/JSON in the pack. |
| **M** | Stated in an archive `notes.md`, `REFERENCE_IMAGES.md` or the wiki, including frames those files describe that I did not open. |
| **L** | Inference or closest-fit guess. Verify before shipping. |

**Status**

| Status | Meaning |
|---|---|
| `matches` | Wording, colour and placement agree with the evidence. Timing or sound may still be unmeasured (noted in the row). |
| `differs` | We do it, but not the way MCC does. |
| `missing` | MCC has it and we do not. |
| `gap (needs VOD)` | The archive has no usable evidence for the exact behaviour. Section 6 says what to look for. |

**Limits of the evidence (important)**

- The archive holds **still frames, notes and pack files only**. Almost no in/stay/out timing was measured from video. Where a row says "not measured", it means exactly that. Do not invent fade values.
- The Java event pack (Battle Box, Sky Battle, HITW and so on) is not public (`A/resource_pack/event/GAPS.md` s1). The fonts and GUI sprites in `PK/` come from the MCC Island pack (1.18). The event HUD is a close relative, but it is not proven identical.
- Event sounds in `EV/` come from the 15th-anniversary server, so they are "character and timing, not a guaranteed main-event mix" (`EV/notes.md` s1).
- MCCI sounds in `MI/` are mostly **inferred ids**; only a few are source-confirmed (`MI/notes.md` s2). Per-game cues (wall pass, rocket, border, chest) are absent.
- Pre-existing owner rules baked into code: victory stings are quieter (pack volume 0.35, `_candidates/pack/build_pack.py`), and the sidebar uses the default font in normal case, not `mcc:hud` (McmHud.hud).

**What our pack contains today** (`_candidates/pack/build_pack.py`, `report.json`):

- 80 sound events: all `mcc.*` event keys, plus 15 Ace Race `mcc.ar.*`.
- `mcc:hud` TTF font (13,508 B), `mcc:icon` font with 14 glyphs and 10 team animals, and a Bingo card font.
- Every vanilla boss-bar sprite is replaced by a transparent PNG.
- Old-MCC hotbar and XP bar, Dodgebolt bow/bolt, Build Mart floor and cart, and about 372 block textures.

Not in the pack yet: any MCCI sound, `game_titles`, `game_icons`, the `*_receive` frame sets, `level_up`, `spinning_glow`, the boss-bar and callout fonts, the glow strips and the coin particles.

Note: `resourcepack/` (synthesized `mcmshow:*` sounds, `McmGlyphs.java`) is the older original pack. `McmFx` and `McmHud` target the `_candidates/pack` build.

---

## 1. Core, shared moments (McmFx / McmHud / Core)

| ID | Moment | What MCC shows | Animation (in / stay / out, frames) | Sound | Sources | Ours now | Status |
|---|---|---|---|---|---|---|---|
| C1 | **Game intro / title card** | Full-screen game logo card. Battle Box: logo on a team-colour burst, then `Teleporting soon…` and 10 team banners. Build Mart: logo top-left (about x 270-735, y 105-345 on 1920x1080) on a blue patterned background, with a translucent black rules panel bottom-right (yellow `YOU EARN COINS FROM`, white `- ...` lines). The MCCI pack has 4 title glyphs (BB, HITW, SB, TGTTOS) in the icon font. | Stills only. Sparkle particles drift over the Build Mart card (H). In/stay/out not measured. | Candidates: `MI/countdown/game_starting.ogg` (11.64 s, inferred id), `game_starting_ingame.ogg` (13.66 s). Unconfirmed: `EV/ui/noxcrew.ogg` (15.6 KB, a Noxcrew sting, role unknown). | H `A/build_mart/bm-2/frames/ui/03_game_title_card.jpg`; M `A/battle_box/bb-3/notes.md` s2.1 and `.../frames/splash/021942_splash_title_card.jpg`; H `PK/mcc/textures/_fonts/game_titles/*.png`; H `A/parkour_warrior/pw-1/frames/current_title_card.png` (wiki still) | `McmFx.titleCard` (McmFx.java:95), called for every game at `Core:3749`: gold bold title, grey subtitle, fade 12 / 70 / 20 ticks (4/12/4 in fast-test), amethyst chime + note-block chime | `differs` |
| C2 | **How-to-play / pregame rules cards** | Battle Box: 3 pages, translucent black text panel with a blue patterned card border, bottom-right `WAITING FOR PLAYERS… PAGE n/3`. Dodgebolt: 4 pages. Survival Games: `HOW TO PLAY` overlay with the logo. Copy examples: "Each round lasts 1 minute, and you only get 1 life per round". | Page cadence not measured. | None known. Closest: `mcc.ui.click` (`EV/ui/start_button_interact.ogg`). | M `A/dodgebolt/db-1/REFERENCE_IMAGES.md` rows 032125-032201; M `A/battle_box/bb-3/frames/_shared/021910/021922/021930_bb_howto_page*.jpg`; M `A/survival_games/sg-1/notes.md` s1 | Games print a chat description at start (for example HITW `context.broadcast`). No cards. | `missing` |
| C3 | **Pre-game bar `GAME STARTING IN: mm:ss`** | Top-centre dark translucent strip about 200 px wide at 720p with a thin coloured fill line. White label, coloured time. SoT: `GAME STARTING IN: 01:21` yellow fill, then `SAND TIMER STARTS: 00:16` green fill. Battle Box `GAME STARTING IN: 00:20` yellow on dark, later `GAME PAUSED`. | Fill drains with the countdown. The bar vanishes when the timer runs. | None. The tick at 3-2-1 is C4. | H crop `A/sands_of_time/sot-2/frames/37_bossbar_sand_timer_starts.png` (strip and text; the fill line is not visible in the crop); M `A/sands_of_time/sot-2/notes.md` s8.1; M `A/battle_box/bb-3/notes.md` s4 | `McmHud.bossBar` (McmHud.java:343): a text-only timer. `build_pack.py` hides all boss-bar sprites, so the dark strip and fill are gone. | `differs` |
| C4 | **Countdown numbers (`Starting in N`)** | Centre title: a small label plus a big number. Labels: `Starting in` (PT white, about 2x size; SKB giant cyan), `Chaos begins in` (TGTTOS, large green), `Dropping in` (RSR giant cyan, from 5), `Descent begins in` (Terra Swoop Force). Dodgebolt: cyan block letters, about 1 s per number. Counts run from 5-10. | One number per second. Fade values not measured. | `EV/countdown/count_3_2_1.ogg` (`mcc.countdown`, 1.004 s, one tick per number). Pitch step per number unknown (L). Isolated 3-2-1-go reference: `MI/countdown/pre_game_countdown.ogg` (11.83 s). Shorter alternative: `countdown_jingle_8s.ogg`. | M `A/parkour_tag/pt-2/frames/015757_urbanjungle_starting_countdown.jpg`; M `A/tgttos/tg-6/notes.md` s1; M `A/rocket_spleef_rush/rsr-2/notes.md` s2; M `A/dodgebolt/db-1/notes.md` s4; M `A/skyblockle/skb-1/notes.md` | `McmFx.countdown` (McmFx.java:181): bold number coloured red 3 / yellow 2 / green 1 (aqua above 3), game-supplied subtitle, title fade 0/18/4 ticks. Pack tick only when n <= 3. | `differs` |
| C5 | **GO / release** | Event frames show no `GO!` title. TGTTOS: the barrier drops after `Chaos begins in 1`. GR: cages open and the room timer starts at 0:00. MCCI Dojo: two-line `Course 1` / `Go!`, timer starts at once. | Unknown. | `EV/countdown/go.ogg` (`mcc.go`, 4.01 s, launch + first second) | M `A/parkour_warrior/pw-2/REFERENCE_IMAGES.md` row 000012 (Dojo, MCCI); M `A/tgttos/tg-5/notes.md`; M `A/grid_runners/gr-2/notes.md` s1 | `McmFx.countdown` go(): green bold `GO!`, fade 0/20/10, `mcc.go` (or vanilla pling + levelup + firework blast without the pack) | `gap (needs VOD)` |
| C6 | **Round card** | PT: white `Round n:` over `TeamA [icon] vs [icon] TeamB`, team colours, about 2 s. SB: full-screen dark maroon card (about #400000, eyeballed), white pixel-font `Round 1`, then `Starting in N`. MCCI: a clean black screen with white text (`Game 2`). | PT about 2 s (M). Others not measured. | None known | H `A/sky_battle/sb-2/frames/000159_mesamadness_round-start-titlecard.jpg`; M `A/parkour_tag/pt-2/notes.md` s1 and s4; M `A/mcci/mcci-3/notes.md` | Battle Box and Dodgebolt use a gold bold `Round N` (BattleBoxGame:670, DodgeboltGame:379). No full-screen card. PT: see G14. | `differs` |
| C7 | **`Round Over!` and `NEXT ROUND IN`** | `Round Over!` in huge red with shadow (HITW, PT about 2x, RSR); huge **pink** in Dodgebolt. Above it `NEXT ROUND IN: 00:29` (HITW 25-30 s, DB 17-20 s, RSR 00:29). PT sidebar `Next Round: 00:08` in pink. Chat: `The winner of this round is X`; `you received 72 coins from dodging 24 walls this round`. | Not measured. | None known | M `A/dodgebolt/db-1/REFERENCE_IMAGES.md` row 032733; M `A/hole_in_the_wall/hitw-1/REFERENCE_IMAGES.md` row 011316; M `A/parkour_tag/pt-2/notes.md` s4; M `A/rocket_spleef_rush/rsr-2/notes.md` | RSR red bold 2500 ms (RocketSpleefGame:677) and PT red (ParkourTagGame:1043) read right. DB light-purple (DodgeboltGame:567), TGTTOS yellow `Round over` (TgttosGame:515). `NEXT ROUND IN` is an action bar in DB only. | `differs` |
| C8 | **Round timer** | Top-centre dark rounded plate with white `M:SS`. RSR measured at 180x60 px at (855,5)-(1035,65) on 1920x1080. PW: `TIME LEFT: MM:SS`, warm yellow on a dark bar under a progress bar. PT: sidebar `Time left: 00:16`, red during play. `OVERTIME!` red banner replaces the timer. | Ticks per second. | None | M `A/rocket_spleef_rush/rsr-2/notes.md` s3; M `A/parkour_warrior/pw-1/notes.md` s4; M `A/parkour_tag/pt-2/notes.md` s2 | `McmHud.bossBar` shows `TIME LEFT: mm:ss` (yellow label, white value), no plate (see C3) | `differs` |
| C9 | **Sidebar layout** | `MCC: <EVT> (n/8): <GAME>`, `MAP:`, `ROUNDS (1/3)`, `PLAYERS REMAINING: 40/40`, `GAME COINS (X2.5):`, top-4 coin rows (rank, team icon, name in team colour, dotted leader, value + coin), footers. `LIVE SCORES: MCC.LIVE`, `PLAY.MCCISLAND.NET` and `MERCH` are broadcast ads, so do not replicate. | Updates every second. | None | H frames `A/sky_battle/sb-1/frames/001110_blizzard_bridge_border.jpg`, `A/build_mart/bm-2/frames/ui/05_build_completed_feedback.jpg`; M `A/hole_in_the_wall/hitw-2/notes.md` s4; M `A/survival_games/sg-1/notes.md` | `McmHud.sidebar` (McmHud.java:144): same lines, team icons, dot leaders, coin glyph. Owner rule: default font, normal case. | `matches` |
| C10 | **Sidebar coin-row flash** | Coin values turn green when a team scores. | Not measured. | None | M `A/survival_games/sg-1/notes.md` s1 | None | `missing` |
| C11 | **Coin award** | See `game-plugins/COIN_FEEDBACK.md`. Related assets: `PK/mcc/textures/_fonts/coin_receive` (11 frames), `champion_score_receive` (11), `gem_receive` (11), `EV/ui/coins_small.ogg`, `coins_big.ogg`, `MI/ui/coin_jingle.ogg`. | n/a | n/a | n/a | n/a | see COIN_FEEDBACK.md (not counted) |
| C12 | **Kill / elimination feed** | Chat line with a gold `[+N ⛁]` prefix and a skull icon: `[+3 ⛁] [☠] Roscumber got yeeted by a moving wall.`; SB `[+2] A was slain by B`, `didn't want to live in the same world as X`; RSR `[+3] ✓ X was eliminated by Y!`. Top-right `0 ⚔ 0 ☠` counters (SB, BB); RSR rocket and skull counters. Centre text `DOUBLE ELIMINATION`, `Only 5 players remain!`. The killer gets no title. | Chat only; counters tick. | Killer: `EV/ui/score_acquired.ogg` (`mcc.ui.score`). MCCI `MI/elimination/kill.ogg` (2.73 s, loud foreground hit, not in pack). | M `A/hole_in_the_wall/hitw-1/notes.md` s2; M `A/sky_battle/sb-1/notes.md` s5; M `A/rocket_spleef_rush/rsr-2/notes.md` s3 | `McmFx.eliminated` (McmFx.java:546): chat `☠ V was eliminated by K`, victim `ELIMINATED` title, killer action bar `⚔ Eliminated V`, `mcc.ui.score` + arrow ding, team-colour burst | `differs` |
| C13 | **Own death and spectator** | Vanilla death screen `You Died` + Spectate/Title Screen buttons (SB, GR). MCCI HITW: centre `Eliminated` / `5th Place!`. Event: chat `Your team was eliminated, you achieved 2nd Place!`. PT spectator: heavy red vignette, free-fly camera, living teammates with a thick team-colour glow outline through walls, name + `20 ♥`, `You've been teleported to <player>`. MCCI: `SPECTATING` pill + `Re-Queue` button. | Vignette persists while spectating. | `EV/elimination/death.ogg` (`mcc.death`, 1.266 s); MCCI `MI/elimination/player_died.ogg` (1.25 s). | M `A/parkour_tag/pt-2/notes.md` s6 (frames 015941, 020029); M `A/hole_in_the_wall/hitw-2/notes.md` s5; M `A/skyblockle/skb-1/frames/mcci_001505_skyblockle_team_eliminated_hud.jpg` | `McmFx.eliminated` gives the title + `mcc.death`; `McmSpectator` keeps spectators in the arena. No vignette, outline, follow cycle or pill. | `differs` |
| C14 | **Team eliminated** | MCCI SKB: red `Team eliminated` over cyan `Team 5th Place!`. Event: chat line only. | Not measured. | `MI/elimination/team_eliminated.ogg` (2.88 s, two-stage). Play only for the last teammate; `teammate_eliminated.ogg` (1.58 s) for the others, never both. | M `A/skyblockle/skb-1/notes.md` s6 (`mcci_001505`); M `A/sounds/mcci/notes.md` s6 | `McmFx.teamEliminated` (McmFx.java:573): chat, `TEAM ELIMINATED` red title for members, subtitle for others, thunder + bass note. No pack sound, no placement subtitle. | `differs` |
| C15 | **Placements and results board** | Full-width text panel: `MC CHAMPIONSHIP <EVENT>` / `Presented by ...` / `<GAME> - GAME COINS:` then 10 rows (rank, team icon, team name in team colour, four player names, right-aligned coins). The live sidebar stays. PT shows it after every round. MCCI HITW recap `Game Over!` / `Game winners:` 1st-6th with coins and times. | Not measured. | `EV/victory/game_end.ogg` (9.02 s, quiet "results" cue); `big_success.ogg` (5.6 s) and `small_success.ogg` (3.2 s) for result tiers. | M `A/build_mart/bm-2/frames/ui/09_game_results_overlay.jpg`; M `A/parkour_tag/pt-2/notes.md` s4; M `A/hole_in_the_wall/hitw-2/notes.md` s5 | `McmFx.placementReveal` (McmFx.java:608): 3rd, 2nd, 1st titles 50 ticks apart with chat and sounds | `differs` |
| C16 | **Winner celebration and fireworks** | Dodgebolt: full-screen banner over the live arena, gold pixel crown above the team name in its colour above `TAKE THE CROWN`, about 1/3 of the screen. PW: `COURSE COMPLETED! HARD ENDING`, gold trophy/frog, torii, fireworks. TGTTOS: fireworks at the finish pen. SG: crown + fireworks POV. | Banner animates in over the arena (no black screen). Not measured. | `EV/victory/win.ogg` (`mcc.victory.win`, 8.0 s); `crowd_cheer.ogg` (4.8 s, broadcast mix caveat); `confetti_tag.ogg`, `party_popper.ogg`. | H `A/dodgebolt/db-1/frames/032901_dodgebolt_victory-banner.jpg`; M `A/parkour_warrior/pw-1/notes.md` timeline 8:51; M `A/survival_games/sg-2/frames/000000_copper-forest_pov-crown-fireworks.jpg` | `McmFx.celebrate` (McmFx.java:351): about 5 s of random-colour fireworks every 16 ticks around winners; `fireworkShow` in the lobby (`Core:1703`, 4064); DB title `♛ <team> ♛` | `differs` |
| C17 | **`BACK TO HUB IN:` bar** | After victory, top bar `BACK TO HUB IN: 01:08` (RSR and PW survivor on MCCI). | Counts down. | None | M `A/mcci/mcci-3/notes.md` (RSR victory); M `A/parkour_warrior/pw-2/frames/000648_survivor_game_victory.jpg` | None | `missing` |
| C18 | **Decision Dome vote and game reveal** | Full-screen game logo on a team-colour burst, then `Teleporting soon…` and 10 team banners. | Not measured. | `EV/countdown/lock_in.ogg` (`mcc.lock_in`, 5.125 s riser; "5 s lock window", L). | M `A/battle_box/bb-3/notes.md` s2.1; M `A/battle_box/bb-3/frames/soup/011550_soup_decision_dome_battle_box_vote.jpg` | `DomeSession` (DomeSession.java:159, 377): yellow `GET READY TO VOTE...` boss bar (red in the last 5 s), gold title `<game>` + `Teleporting soon...` (250 ms / 3 s / 500 ms), toast sound | `differs` |
| C19 | **Multiplier reveal** | Only evidence is the sidebar `GAME COINS (x2.5)`. No reveal animation is documented. | Unknown | Unknown | M `A/sands_of_time/sot-2/notes.md` s7.3 (multiplier table) | Core chat line `Game Coins: (x2.5)` (`Core:3754`) plus the sidebar | `gap (needs VOD)` |
| C20 | **Standings between games and event end** | Not documented in the archive (no event-level standings frame). | Unknown | Unknown | none | Core podium and fireworks (`Core:4064`) | `gap (needs VOD)` |
| C21 | **Lobby / hub arrival** | Nothing documented for arrival. MCCI queue: `MATCH FOUND! TELEPORTING (8/12) IN 10!`. Hub music bed and queue sounds exist. | Unknown | `EV/music/hub.ogg` (30 s excerpt), `EV/ui/queue_success.ogg`, `queue_teleport.ogg`, `queue_joining.ogg`; MCCI `hub_orienteering_50s.ogg`. | M `A/hole_in_the_wall/hitw-2/notes.md` s5; M `A/sounds/event/notes.md` s4 | `Core:4291`: `BLOCK SHOWDOWN` / `Welcome to the MCC hub!` (5/40/10) is our own invention | `gap (needs VOD)` |

---

## 2. Per-game specials

| ID | Moment | What MCC shows | Animation | Sound | Sources | Ours now | Status |
|---|---|---|---|---|---|---|---|
| G1 | **HITW wall and platform warnings** | Centre banner: red rounded square with a white glyph plus two white lines: `PLATFORM SHRINKING!`, `WALL DIFFICULTY INCREASED!`, `WALL SPEED INCREASED!`. `Wall Type Appearing` + the wall name in small text. Red cross-shaped floor blocks mark what is about to go. Super Speed: yellow outlined centre text (MCCI). | Banners not timed. Shrink warning about 5 s before the removal (L). | None known. | M `A/hole_in_the_wall/hitw-2/notes.md` s4; M `A/hole_in_the_wall/hitw-1/notes.md` s1 (`020415_dehef_platform_shrink_red.jpg`) | HITW: subtitle `The platform is shrinking!` + red terracotta ring + bass note (HoleInTheWallGame:644), subtitle `New Wall Type appearing: <type>` (665-670), `OVERDRIVE` title. Red ring matches; banner differs. | `differs` |
| G2 | **HITW eliminated / game over** | Event: `Round Over!` + `NEXT ROUND IN`. MCCI: `1/8 PLAYERS ALIVE`, `Game Over!`, `Eliminated` / `5th Place!`; chat `Finished in 1m 48s · Dodged 13 walls · Survived 5 traps · Outlived 3 players`. | Not measured. | None known | M `A/hole_in_the_wall/hitw-2/notes.md` s5 | Titles `Knocked off!` (HoleInTheWallGame:710), `Winner` (852), no recap | `differs` |
| G3a | **TGTTOS start** | `Chaos begins in N` (large green). Movement locked until it ends. | Counts 10 to 1. | `mcc.countdown` / `mcc.go` | M `A/tgttos/tg-6/notes.md` s1; M `A/tgttos/tg-5/notes.md` s1 | `McmFx.countdown(..., "Chaos begins in...")` (TgttosGame:404, 1650); wording right, visuals per C4 | `differs` |
| G3b | **TGTTOS finish chicken** | Whack the NPC at the finish; chat `[+300] You whacked X, finishing in Nth Place!`; older toast `You claimed eskay's chicken!`. MCCI: `Round Victory!` over `1st Place`. Fireworks at the finish pen; a crowd of chickens/fans. | Not measured. | Vanilla chicken hurt for the hit; `mcc.checkpoint` (`checkpoint_pass.ogg`, 1.2 s) is the only checkpoint cue. | M `A/tgttos/tg-1/frames/0026_blankorp_finish_chicken_claimed.jpg`; M `A/mcci/mcci-4/frames/tgttos/000042_tgttos_badlands_round_victory.jpg`; M `A/tgttos/tg-2/notes.md` | Title `<ordinal>` + `Whack! <time> \| +pts` (TgttosGame:1010), vanilla toast/levelup (volume 0.45), burst + team firework, chicken hurt (768) | `differs` |
| G4 | **Rocket Spleef: drop, overtime, reload** | Giant cyan `Dropping in` + countdown from 5. `OVERTIME!` red banner replaces the timer. `Round Over!` + `NEXT ROUND IN: 00:29`. Reload prompt: rocket icon + 3 pips + black progress bar; elytra bar; `Rocket Cannon` / `Updraft` item names in yellow-green. | Reload bar not timed. | Vanilla explosion; `mcc.pad.launch` (`blastoff.ogg`, 4.0 s) is a candidate for the rocket (L). | M `A/rocket_spleef_rush/rsr-2/notes.md` s2-4; M `A/rocket_spleef_rush/rsr-2/frames/hud_reloading-prompt.jpg` (listed) | `Dropping in` aqua, vanilla hat tick (RocketSpleefGame:378-381); `TNT Time!` red (431); `Round Over!` red (677). No reload HUD in this pass. | `matches` (drop-in wording and colour only) |
| G5a | **Sands of Time start bars** | `GAME STARTING IN: 01:21` (yellow fill), giant `Starting in`, then `SAND TIMER STARTS: 00:16` (green). Bars disappear once the timer runs. No timer HUD during play. | Not measured. | `EV/music/sands_of_time.ogg` (30 s excerpt) | H `A/sands_of_time/sot-2/frames/37_bossbar_sand_timer_starts.png`; M `.../36_bossbar_game_starting.png`; M `A/sands_of_time/sot-2/notes.md` s2.2 and s8.1 | Titles `<n>`, `GO!` (SandsOfTimeGame:345-357), a permanent seal timer title (413), no bars | `differs` |
| G5b | **Sand timer ticks** | Physical gauge only (30 s colour bands, numerals 120/90/60/30). Tick every 10 s, deeper at 30 s and 20 s, every second under 10 s. A start sound mimics the final-10 s tick (since MCC 33). | The gauge level drains. | `EV/gameplay/timer_10.ogg`, `timer_20.ogg`, `timer_30.ogg` (4.0-4.5 s phrases, `mcc.sot.timer_*`); `sand_place*.ogg` (x3). | M `A/sands_of_time/sot-2/notes.md` s2-3; the event `sand_timer.animation.json` is Bedrock-only (see s5) | Titles at 10 s intervals + note-block drum (SandsOfTimeGame:440-443); 3x3x2 bulb, no bands | `differs` |
| G5c | **SoT coins, keys, vaults** | `+N [coin]` floating above the player; `+42 [coin] [+24 [coin]]` when two gains land together. Item toast `1x Golden Vault Key` (gold, centre). Chat `Cyan Coyotes opened a vault!`. | Floating popup. | `mcc.sot.key_gold`, `key_vault`, `team_vault` (80 KB), `chest_open`, `chest_wrong`, `rescue`, `sparkle` (near silent, check). | M `A/sands_of_time/sot-2/notes.md` s7.1, s8.4; M `A/sands_of_time/sot-1/notes.md` s2 | Action bar `+N coins (carrying N)` (SandsOfTimeGame:554), vanilla sand/door/levelup sounds | `differs` |
| G6 | **Grid Runners room complete / course complete** | Dark translucent box: green `ROOM COMPLETED!` over white room name, gold running-player icon on a blue/white card with an animated arrow. Also `GET TO THE EXIT`, `Race to the Finish!`, `COURSE COMPLETED`. `Starting in` at the start. | Arrow animates (frames unknown). | `EV/checkpoint/room_complete.ogg` (6.29 s), `finish.ogg` (10.16 s), `victory/gr_win.ogg`; music `grid_runners_intro.ogg` (10 s), `grid_runners.ogg` (30 s). | H `A/grid_runners/gr-2/frames/hud/hud_room_complete_overlay_mcc32.jpg`; M `A/grid_runners/gr-2/notes.md` s1-2 | Title `Room Cleared!` + `+pts team coins - Nth team` (GridRunnersGame:561) + vanilla levelup; `Course Complete!` (605) | `differs` |
| G7 | **Build Mart build complete / gold build** | Action bar plate (dark translucent) with green `BUILD COMPLETED!` and white build name. Dense teal/green particle burst and falling gold bars at the plot. Chat `[+25 ⛁] [6th] Your team was 6th to complete X`. Top timer pill; pre-game `GAME STARTING IN: 00:46` gold bar. | Particles burst; bars fall. | None known. Closest: `EV/gameplay/cake_complete.ogg` (34 KB), `mcc.ui.score` (L). | H `A/build_mart/bm-2/frames/ui/05_build_completed_feedback.jpg` | Title `BUILD COMPLETED!` / `GOLDEN BUILD!` + toast sound (BuildMartGame:745-747); gold-window boss bar (1213) | `differs` |
| G8 | **Dodgebolt rounds, arrows, victory** | `Starting in 3..2..1` in cyan block letters. `Round Over!` huge pink + `NEXT ROUND IN: 00:17`. Violet arrow glyph about 2 blocks above the arrow holder (spectator-visible). White/gold sparkle at a hit. Chat `⚠ The arena is about to shrink!` sent twice. Victory banner (C16). Red event banner `MCC: <EVENT>: THE FINAL DUEL` at top with round-win pips and a gold crown medallion. | Banner animates in. | `mcc.countdown`/`mcc.go`, `mcc.crowd.cheer` + `mcc.victory.win` for the final (EV/notes s6.3). | M `A/dodgebolt/db-1/frames/032733_dodgebolt_round-over_timer.jpg`; H `.../032901_dodgebolt_victory-banner.jpg`; M `A/dodgebolt/db-1/notes.md` s2-4 | Gold `Round N` + score (DodgeboltGame:379), green `GO!`, aqua countdown (455), light-purple `Round Over!` (567), win title + win sound + cheer + fireworks (663-698) | `differs` |
| G9 | **Battle Box** | Boss bar `GAME STARTING IN: 00:20`, then `Starting in 5…`; 1:00 round clock; `Round Over!`; capture pad converts; scoreboard `ROUNDS [n/9]` with one tick per round; older HUD `Kit Selection: 00:08` then `Battle starts in: 00:05`. | Not measured. | `mcc.countdown`, `mcc.go`; MCCI `MI/music/battle_box.ogg` (4:55 loop). | M `A/battle_box/bb-3/notes.md` s2, s4 | `titleCard` + custom countdown 5-1 with note-block plings (BattleBoxGame:584-623), `FIGHT!` red (717), `ROUND WON!/LOST/DRAW` (899-906) | `differs` |
| G10 | **Survival Games grace, border, crates** | Pill timer `GRACE PERIOD 00:29` (25 s since MCC 16), `NEXT SHRINK: mm:ss`, `SHRINKING`, `FINAL SHRINK`, `LAST STAND mm:ss`, `OVERTIME!`. Chat `[!] The border will start shrinking in 30 seconds.`; large red `BORDER SHRINKING!` toast with a siren icon; red translucent striped screen overlay outside the border; coin-crate beam green when landed. | Pill label swaps per phase. | `MI/elimination/border.ogg` (2.9 s, not in pack). Closest in pack: none. | M `A/survival_games/sg-2/notes.md` s2; M `A/survival_games/sg-2/frames/024855_dappled-farm_border-alert-hud.jpg` | `sendTitle` countdown `Prepare for Survival Games!` (SurvivalGamesGame:725), dragon growl / wither sounds, compass action bar | `differs` |
| G11 | **Sky Battle border** | Top-centre plate: red rounded square with two inward arrows + `BORDER SHRINKING!` (about 03:30 into the 4:00 round); centre red text `Border closing` / `lowering` / `raising` (three axes); `DOUBLE ELIMINATION`; `OVERTIME` stops the border. | Banner on the shrink step; axes not timed. | `MI/elimination/border.ogg` (2.9 s). | H `A/sky_battle/sb-1/frames/001110_blizzard_bridge_border.jpg`; M `A/sky_battle/sb-2/notes.md` s2 | Dragon growl and action bar `Outside the border!` (SkyBattleGame:322, 352); no banner | `differs` |
| G12 | **Parkour Warrior checkpoint / fall / finish** | Gold action bar `Spawn point updated` above the hotbar; `+1` popup at the crosshair with a medal icon; chat `You completed X and earned a BRONZE!`; red chat `You fell. Returning to last checkpoint.`; `You completed the Downtown zone!`; `You finished the entire course!`; `COURSE COMPLETED! HARD ENDING`. Dojo: two-line checkpoint counter + run timer. | Popup not timed. | `EV/checkpoint/checkpoint_pass.ogg` (`mcc.checkpoint`, 1.2 s; Ace Race-voiced, audition) | M `A/parkour_warrior/pw-1/frames/current_000015_first_checkpoint.jpg`; M `A/parkour_warrior/pw-2/frames/000012_dojo_hud_timer_crop.jpg`; M `A/parkour_warrior/pw-1/notes.md` s4 | Green action bar `Spawn point updated` + note-block chime (ParkourWarriorGame:649-650), `<zone> zone complete!`, `FINISHED!` + team firework (665-667) | `differs` |
| G13 | **Ace Race lap / checkpoint** | No MCC frame in the archive (Ace Race excluded; the Clouds 2 clip is a fan recreation with `Starting In` and `You are in 1st place` only). | Unknown | `mcc/ar/*` Ace Race set is in the pack (jump/launch/speed/elytra/castle door). | M `EX/video_clouds2/README.md` (fan; no lap HUD) | `WorldRace` lap and missed-checkpoint titles (WorldRace.java:239-257), `mcc.ar.launch1` | `gap (needs VOD)` |
| G14 | **Parkour Tag hunter select, tag, survive** | Sidebar `Select Hunter: 00:08` (pink), chat `X is going to hunt the opposing team this round. They can hunt 3 more times.`; `Round n:` card; `Starting in N`; centre overlays: `Tagged!` large yellow, `Hunted!` + `Well played!` yellow, `Survivor!` + `Well played!` green, `Round Over!` red; chat `[+6.0] X was tagged by Y`. | Not measured. | `mcc.ui.score` for the hunter (L). | H `A/parkour_tag/pt-2/frames/001842_wintertown_tagged_overlay.jpg`; M `A/parkour_tag/pt-2/notes.md` s1, s4, s5 | ParkourTagGame: `Round n:` card (836), `Starting in` white (686), `RUN!`/`HUNT!` (942-963), `Tagged!` yellow (1133), `Hunted!` (1145), `Survivor!` green (1019), `Round Over!` red (1043) | `matches` (text and colour; sounds and timing unmeasured) |
| G15 | **Bingo** | Not in the archive (excluded from scope). | Unknown | Unknown | none | `FULL CARD!`, `BINGO!`, `GO!` titles + toast sounds (BingoGame:614-758) | `gap (needs VOD)` |
| G16 | **SkyBlockle** | MCCI: giant cyan `Starting in`; red `Team eliminated` over cyan `Team 5th Place!`; `Game begins: 00:26` then `00:16`; red `The border is shrinking!` panel with a white arrow; `Next Shrink` sidebar line (first shrink at 11:00 left). | Not measured. | `MI/elimination/border.ogg`, `team_eliminated.ogg` | M `A/skyblockle/skb-1/frames/mcc4_001712_skyblockle_border_shrinking_eggs_hud.jpg`; M `A/skyblockle/skb-1/notes.md` s3-4 | `titleCard`, `PvP is now ON`, `LAST STAND`, red `Border shrinking` titles, dragon growl (SkyBlockleGame:267-431) | `differs` |
| G17 | **Foot Race, Meltdown, Railroad Rush** | Not in the archive (excluded). | Unknown | Unknown | none | Own titles via `McmFx` | `gap (needs VOD)` |

---

## 3. Status counts

Counted from the tables above. `C11` (coin award) is excluded; it belongs to `COIN_FEEDBACK.md`.

| Status | Count | Rows |
|---|---:|---|
| `matches` | 3 | C9, G4, G14 |
| `differs` | 27 | C1, C3, C4, C6, C7, C8, C12-C16, C18, G1, G2, G3a, G3b, G5a, G5b, G5c, G6-G12, G16 |
| `missing` | 3 | C2, C10, C17 |
| `gap (needs VOD)` | 7 | C5, C19, C20, C21, G13, G15, G17 |
| Counted rows | 40 | plus C11 (coin award), which links to `COIN_FEEDBACK.md`: 41 rows in all |

---

## 4. Sound cheat sheet: exact archive file, or closest vanilla

Pack keys already exist for every `EV/` file (`mcc.*`). `MI/` files are not in the pack.

| Moment | Archive file | Length | In pack? | Closest vanilla if absent |
|---|---|---:|---|---|
| Count tick | `EV/countdown/count_3_2_1.ogg` | 1.00 s | yes (`mcc.countdown`) | `block.note_block.pling` pitch 0.6 -> 1.4 |
| GO | `EV/countdown/go.ogg` | 4.01 s | yes (`mcc.go`) | `entity.firework_rocket.large_blast` 1.0 |
| Decision lock | `EV/countdown/lock_in.ogg` | 5.13 s | yes (`mcc.lock_in`; hot, attenuate 5 dB) | `block.beacon.activate` 1.2 |
| Own death | `EV/elimination/death.ogg` | 1.27 s | yes | `entity.player.hurt` 0.8 + `entity.item.break` 0.6 |
| Kill | `MI/elimination/kill.ogg` | 2.73 s | no | `entity.experience_orb.pickup` 1.6 |
| Teammate eliminated | `MI/elimination/teammate_eliminated.ogg` | 1.58 s | no | `block.note_block.bass` 0.7 |
| Team eliminated | `MI/elimination/team_eliminated.ogg` | 2.88 s | no | `entity.lightning_bolt.thunder` 0.5 |
| Border | `MI/elimination/border.ogg` | 2.90 s | no | `block.beacon.deactivate` 0.7 |
| Checkpoint | `EV/checkpoint/checkpoint_pass.ogg` | 1.20 s | yes (`mcc.checkpoint`) | `block.note_block.chime` 1.5 |
| Room complete | `EV/checkpoint/room_complete.ogg` | 6.29 s | yes | `entity.player.levelup` 1.4 |
| Finish | `EV/checkpoint/finish.ogg` | 10.16 s | yes | `ui.toast.challenge_complete` |
| Win | `EV/victory/win.ogg` | 8.0 s | yes (vol 0.35) | `ui.toast.challenge_complete` |
| Game end | `EV/victory/game_end.ogg` | 9.02 s | yes | n/a |
| Result tiers | `EV/victory/big_success.ogg`, `small_success.ogg` | 5.6 / 3.2 s | yes | levelup |
| Score | `EV/ui/score_acquired.ogg` | 45 KB | yes (`mcc.ui.score`) | `entity.experience_orb.pickup` 1.6 |
| Queue | `EV/ui/queue_success.ogg`, `queue_teleport.ogg`, `queue_joining.ogg` | 25 KB / 24 KB / 44 KB | yes (joining is very quiet, raise 18 dB) | `block.portal.travel` 0.25 |
| SoT timer | `EV/gameplay/timer_10/20/30.ogg` | 4.0-4.5 s | yes | `block.note_block.basedrum` |
| Pad / rocket | `EV/gameplay/blastoff.ogg` | 4.0 s | yes (`mcc.pad.launch`) | `entity.generic.explode` 1.3 |
| Celebration | `EV/gameplay/confetti_tag.ogg`, `party_popper.ogg`, `crowd/crowd_cheer.ogg` | 13 KB / 17 KB / 4.8 s | yes | `entity.firework_rocket.blast` |
| Announcer | `MI/announcer/gameover.ogg` (the only MCCI-confirmed line) | 31 KB | no | none |
| Music | `EV/music/hub.ogg`, `grid_runners*.ogg`, `sands_of_time.ogg` (30 s excerpts); `MI/music/*.ogg` (full loops) | 10-295 s | event only | none |

---

## 5. Asset list: exact archive files the pack would need

Everything below is a **private** archive path. Copy into the pack build only; never into tracked files. Sizes were read from the PNGs on disk.

**Fonts and icons (MCCI 1.18 pack, `PK/`)**

| Path (under `PK/`) | Frame size | Frames | Frame time / use | Provider |
|---|---|---:|---|---|
| `mcc/font/hud.ttf` | n/a | n/a | already in pack | `hud.json` size 7, shift y 2.25 |
| `mcc/textures/_fonts/game_titles/battle_box.png` | 98x65 | 1 | static; title glyph U+E07C | ascent 7, height 32 |
| `.../game_titles/hole_in_the_wall.png` | 81x67 | 1 | U+E07D | ascent 7, height 34 |
| `.../game_titles/sky_battle.png` | 92x56 | 1 | U+E07E | ascent 7, height 28 |
| `.../game_titles/tgttos.png` | 107x54 | 1 | U+E07F | ascent 7, height 27 |
| `.../game_icons/game_battle_box.png`, `_hole_in_the_wall`, `_sky_battle`, `_tgttosawaf` | 16x16 | 1 each | static | ascent 16, height 16 |
| `.../game_icons/*_expanded.png` (BB, HITW, SB, TGTTOS, island_main) | 32x32 | 1 each | static | ascent 24, height 32 |
| `.../game_icons/*_small.png` (BB, HITW, SB, TGTTOS, Dynaball, island_main) | 16x16 | 1 each | sidebar/tab size | ascent 7, height 8 |
| `.../team/<colour>.png`, `_big`, `_glow` | 16x16 | 1 each | `<colour>.png` already in pack | ascent 7, height 8 |
| `.../winner.png`, `kills.png`, `score_point.png`, `team/spectators.png` | 16x16 | 1 | winner and kills are already in the pack; `spectators.png` is also in the pack | ascent 7, height 8 |
| `.../point_indicator/empty.png`, `full.png` | 16x16 | 2 states | round-win pips (`best_of_game.json`, U+E000/E001) | ascent 7, height 8 |
| `.../spectating.png` | n/a | 1 | `SPECTATING` pill | ascent 7, height 21 |
| `.../allies_vs_enemies.png`, `allies_vs_teams*.png`, `bb.png`, `clock.png`, `party.png`, `sb.png`, `sg.png`, `queue.png` | n/a | 1 each | boss-bar strip art (`boss_bar.json`) | ascent 7, height 13 (`queue` 31) |
| `.../coin_receive/` `1_start`, `2_start`, `3_start`, `4_start`, `4_end`, `5_loop`, `5_end`, `6_loop`, `6_end`, `7_loop`, `7_end` | 128x128 | 11 | **server-driven glyph swap** (no mcmeta); frame time unknown, see `COIN_FEEDBACK.md` | ascent 121, height 128 |
| `.../champion_score_receive/` (same 11 names) | 128x128 | 11 | same | same |
| `.../gem_receive/` (same 11 names) | 128x128 | 11 | same | same |
| `.../experience_receive/` `a1..a4`, `b1..b4`, `c1..c4`, `d1..d4` | 128x128 | 16 | same | same |
| `.../level_up/1..32.png` + 10 colour variants | 128x128 | 32 + 10 | probably the level-up burst; unmeasured | same |
| `.../spinning_glow/1..3.png` | 256x256 | 3 | glow loop behind a reward; unmeasured | ascent 156, height 256 |

**Animated strips with `.mcmeta` (Java; vertical strips)**

| Path (under `PK/`) | Frame size | Frames | Frametime (ticks) | Use |
|---|---|---:|---|---|
| `mcc/textures/island_interface/battle_box/kit_selection.png` | 16x16 | 2 | 9 | kit icon shimmer |
| `minecraft/textures/gui/gui_icons/team_*_glow.png` (10 teams), `game_*_glow.png`, `trophy_glow.png` | 16x16 | 2 | 9 | glow on team/game/trophy icons |
| `minecraft/textures/gui/gui_icons/glow.png` | 18x18 | 2 | 9 | generic slot glow |
| `minecraft/textures/gui/gui_icons/explosion_common.png` (and `_uncommon`, `_rare`, `_epic`, `_legendary`, `_mythic`) | 48x48 | common 7, legendary 11 (others not measured) | 1 | rarity burst |
| `minecraft/textures/gui/gui_icons/refresh_animation.png` | 50x50 | 8 | 1 | refresh spinner |
| `minecraft/textures/gui/gui_icons/holo_screen.png` | 68x68 | 3 | 5 | hologram panel |
| `mcc/textures/particle/coin/forward/coin_1.png` / `coin_2` / `coin_3` | 16x16 | 25 sheet frames; 8 rotation frames + hold; custom order of 65 / 65 / 73 entries | 2 / 3 / 4, interpolate | coin particle (see `COIN_FEEDBACK.md`) |
| `mcc/textures/particle/coin/reverse/coin_1_reverse.png` (`_2`, `_3`) | 16x16 | 24 sheet frames; custom order 64 / 64 / 72 | 2 / 3 / 4, interpolate | reverse coin |
| `mcc/textures/island_lobby/game_signs/battle_box_anim.png`, `sky_battle_anim`, `tgttos_anim` | 192x192 | 4 | 10 | hub game signs |
| `mcc/textures/island_lobby/game_signs/hitw_anim.png` | 192x192 | 20 | 4 | hub game sign |
| `mcc/textures/island_lobby/game_lobby/kiosk_arrow.png` | 32x32 | 5 | 3 | hub kiosk arrow |
| `mcc/textures/island_interface/main_menu/rank/rank_1..3.png` | 64x64 | 24 | 2 | rank badge shimmer |

**Bedrock event assets (`A/resource_pack/event/downloads/main_rp/`, not drop-in)**

| Path | Notes |
|---|---|
| `animations/sand_timer.animation.json` (28,954 B), `entity/sot/sand_timer.entity.json`, `models/...` | Real SoT gauge geometry. Needs a Java rebuild (item display or custom model). |
| `animations/podiums.animation.json`, `treasure_chest.animation.json`, `coins.animation.json`, `trophy_help.animation.json`, `start_button.animation.json` | Reference for timing only. |
| `particles/` (115 JSON: `collect`, `key_collect`, `idle_sparkle`, `party_popper`, `sot_despawn`, 80 firework variants) | Colour and lifetime tables are in `A/resource_pack/event/notes.md` s7. |
| `textures/flipbook_textures.json` | Only `water_still_grey` (8 ticks/frame, blend) is in scope; Ace Race pads are 8 frames at 2 ticks. |

**Sounds to add to the pack from `MI/`** (all Ogg Vorbis, mono unless noted):

`elimination/kill.ogg`, `player_died.ogg`, `eliminated.ogg`, `teammate_eliminated.ogg`, `team_eliminated.ogg`, `border.ogg`; `countdown/pre_game_countdown.ogg`, `game_starting.ogg`, `game_starting_ingame.ogg`, `countdown_jingle_8s.ogg`; `ui/click_small.ogg`, `coin_jingle.ogg`, `gem_jingle.ogg`; `announcer/gameover.ogg`; `music/*.ogg` (stereo, `stream: true`).

---

## 6. Prioritised gap list (what still needs a VOD)

Ordered by how much of the owner's "literally everything" ask each one blocks. All timestamps are from existing archive notes; unknown ones say so.

| Pri | Gap | What to look for | Where |
|---:|---|---|---|
| 1 | **In/stay/out timing of every title** (`Starting in`, `Round Over!`, `Tagged!`, `ROOM COMPLETED!`) | Frame-step (60 fps) 10 s around one title in each style. Record fade-in, hold, fade-out in ticks, text size relative to the screen, and shadow. One reference per style is enough. | PT S2 Wintertown at 00:17:38-00:18:10 (round over, countdown, tagged at 00:18:42); GR MCC 32 at 00:03:56 (room complete); DB at 03:27:33 (round over, 03:29:01 victory). |
| 2 | **Real MCC `GO!` / release moment** | Does a title appear at release? Is there a sound other than the 3-2-1 tick? Check the last 2 s of `Chaos begins in` in TGTTOS and the cage release in PT. | TGTTOS S1 start (frames `0001_badlands_start_countdown`), PT S1 Urban Jungle 01:57:57-01:58:10. |
| 3 | **Event-level flow**: Decision Dome vote UI, multiplier reveal, standings between games, event winner | The 10 s around each game hand-off: logo card, banners, `Teleporting soon…`, the multiplier line in the sidebar, any standings overlay or podium. | Battle Box P24 at 02:19:10-02:19:42 (has the card and how-to pages); a full MCC event VOD at the dome after game 7. |
| 4 | **Hub arrival and queue/teleport sounds** | What the player sees when entering the hub after a game; the title (if any), music cut, teleport sound. | MCC event after the last game (VOD end), or MCCI hub entry. Not located yet. |
| 5 | **Pre-game and timer boss-bar art** | A clean 1080p crop of the boss bar with a visible fill: dark strip width/height, fill thickness, colours per phase (yellow, green, red). | SoT MCC 33 (`pG6DPMIWemA`) around `GAME STARTING IN` and `SAND TIMER STARTS`; BB `GAME STARTING IN: 00:20`. |
| 6 | **Per-game warning banners**: HITW shrink/difficulty banners, SB/SG `BORDER SHRINKING!` toast | A 1080p crop with the red rounded square icon and exact text offsets; duration on screen. | HITW Frozen Pit (`p1GnkQYIlhk`) at 01:12:52; SB Blizzard at 03:30; SG Dappled Farm border alert (frame 024855). |
| 7 | **Result screens**: MCC results board animation, HITW/SB recap, `Round Victory!` for TGTTOS | Does the board scroll or fade in? How long does it stay? Does the sidebar stay? | BM admin POV `SqGxUTwxhNs` at 00:31:20; MCCI TGTTOS `yoME302evpk` at 0:42. |
| 8 | **Ace Race lap, checkpoint, finish** | Real MCC Ace Race HUD: lap text, checkpoint counter, finish feedback, `Starting in` look. The archive has none (fan recreation only). | An MCC Ace Race VOD (City Marathon or Yeti Set Go). Not located. |
| 9 | **Bingo, Foot Race, Meltdown, Railroad Rush** | Whatever MCC-style intro, countdown and finish these games have. Bingo is MCCI only. | Not located; outside the archive's scope. |
| 10 | **Per-game sound cues**: wall pass, rocket fire, chest open, kit pick, border (event version) | Clean audio of a single cue with no commentary. `MI/GAP.md` s5 lists these as missing. | One clean POV per game. |
| 11 | **Kill / death victim screens** | Does MCC show a title for the victim at all? Frames show chat and the vanilla death screen only. | SB MCC AZ Tubbo POV; RSR MCC 31 (`2u0foeOvbRI`, 4:15-7:30). |
| 12 | **Spectator extras**: vignette strength, glow outline, follow-key cycling | A spectator POV with the HUD visible. | PT S1 Urban Jungle at 01:59:41 and 02:00:29. |

---

## 7. Proposed build order

### 7.1 Core, shared (do first; every game benefits)

1. **Pack sound hookup** (zero risk): wire the remaining `mcc.*` keys into `McmFx`: `mcc.checkpoint`, `mcc.finish`, `mcc.room_complete`, `mcc.victory.end`, `mcc.victory.small_success`, `mcc.crowd.cheer`, `mcc.queue.*`, `mcc.lock_in`. Add the MCCI eliminate/kill/team/border files to `build_pack.py` and give `McmFx` helpers (`kill`, `teamEliminated` sound, `border`).
2. **Boss-bar strip** (C3, C8): stop hiding the strip, or draw it with a font-bitmap behind the text (see `PK/mcc/font/boss_bar.json`), with yellow / green / red fills and a thin fill line. Fixes pre-game, SoT start and timer rows at once.
3. **Centre-overlay style** (C4, C7): decide one title style from MCC (small label + big number, red shadowed `Round Over!`), rebuild `McmFx.countdown` and a new `McmFx.roundOver` around it. Keep the existing 3-2-1 tick; drop the red/yellow/green colours.
4. **Elimination and spectator pass** (C12-C14): move the victim title to the MCC form (chat-first, `Eliminated` / `Nth Place!`), add `team_eliminated` sound and the `Team eliminated` / `Team Nth Place!` title, add spectator vignette and teammate glow.
5. **Results board** (C15): one `McmFx.resultsBoard` (title lines, 10 rows with team icon and player names, coin column). Keep `placementReveal` for the Core podium only.
6. **Winner banner** (C16): a `McmFx.winnerBanner` (crown + team name + subtitle) shared by Dodgebolt and others; keep fireworks.
7. **Intro card and how-to pages** (C1, C2): game title glyphs from `game_titles`, optional translucent rules panel. Needs the timing from gap 1 first.
8. **Event flow** (C18-C21): only after gaps 3 and 4 are closed. Do not invent a reveal.

### 7.2 Per-game (after the core is stable)

| Order | Game | Work |
|---:|---|---|
| 1 | Parkour Tag | Already `matches`; add sounds only. |
| 2 | Rocket Spleef | Add the reload HUD (rocket icon + 3 pips) and the `OVERTIME!` banner. |
| 3 | TGTTOS / Race | `Round Victory!` + `Nth Place!` title, `You whacked X` chat, fireworks at the chicken, `mcc.checkpoint`. |
| 4 | Parkour Warrior | Gold `Spawn point updated`, medal `+1` popup, red `You fell. Returning to last checkpoint.` chat, `COURSE COMPLETED!`. |
| 5 | Grid Runners | `ROOM COMPLETED!` box + `room_complete.ogg`, `Race to the Finish!`, `COURSE COMPLETED`. |
| 6 | Sky Battle / SkyBlockle / SG | `BORDER SHRINKING!` plate (red square + arrows), `border.ogg`, `OVERTIME!`, kill/death counters. |
| 7 | HITW | Banner plate for `PLATFORM SHRINKING!` and friends, `New Wall Type appearing` banner, recap line. |
| 8 | Dodgebolt | Pink `Round Over!`, `NEXT ROUND IN` bar, violet arrow-holder glyph, victory banner (shared). |
| 9 | Sands of Time | Real gauge model with bands and numerals, bossbar start sequence, tick cadence from `timer_*`, vault/key toasts. This is the biggest rebuild. |
| 10 | Build Mart | Action-bar plate for `BUILD COMPLETED!`, teal particle burst, falling gold bars, chat feed. |
| 11 | Battle Box | Tutorial pages, `GAME STARTING IN` bar, kit-selection timer. |
| 12 | Ace Race, Bingo, Foot Race, Meltdown, Railroad Rush | Blocked on gaps 8 and 9. |
