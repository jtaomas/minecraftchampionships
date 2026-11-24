# To Get To The Other Side — spec v2 (owner-approved, with the difficulty section below)

Game id `tgttos`, plugin `McmTgttos`, world `mcc_tgttos` (created at plugin enable). Research: `..\research\tgttos_rules.md` (rules, gap list G1–G18, checklist), `..\research\tgttos_maps.md` (structure, 17 design requirements, concepts); second-opinion files `tgttos_rules_sonnet.md`, `tgttos_maps_sonnet.md`. Follow `..\LESSONS.md`. The current plugin's maps and kits are replaced.

## Rules (researched)
- **6 rounds**, each on a different map from our pool (random, no repeats). Round length **100 s** (config). Pre-round ~15 s in the start strip with a "3-2-1" title; players **already hold their kit**.
- **Finish** by punching any goal chicken in the pen at the far end: placement title, "Whack!" effect + sound, chat line with placement. (Chickens, not player-skinned fans.)
- **Void/hazard fall:** back to the map start within 1 s, keep racing; a pun death message in chat.
- **No damage ever.** Punches and snowballs knock back only. **Players don't collide.**
- **Kits by map type:** bridging maps = 64 team-colour concrete + pickaxe + 5 snowballs (SURVIVAL, may break only player-placed concrete, anyone's); parkour/door maps = 5 snowballs only (ADVENTURE); boat map = start seated in a boat; elytra map = elytra + fireworks; other map-specific items as listed per map.
- **Karma (owner decision after playtest 2: karma off; `karma.enabled: false` by default, code kept):** repeated punching makes the puncher take more knockback, glow red, then lose punching for a few seconds; decays. Players punched a lot take less knockback.
- **Round ends** when everyone has finished or at 0:00. Player-placed blocks cleared between rounds.
- **HUD:** boss-bar timer; sidebar: round n/6, map name, players finished n/N, your placement per round so far; `McmFx` title card per map, countdown, finish effects, results.

## Scoring (researched shape, scaled ÷5 to our points scale; config)
- Finishing: 8 points for 1st, 1 fewer per 5 places, minimum 1. Top-10 bonus: 16, 15, 14 … 7. Non-finishers 0.
- Team bonus: first 4 sides with every online member finished get 20 / 15 / 10 / 5, split evenly among members (sides of 2+ only).

## Defaults, not researched (config; listed in the README)
- Start: players held behind a barrier line that drops at GO (LESSONS gate rules).
- Respawn refills the starting kit. Lava/hot water = respawn.
- Finished players: spectator mode, confined to the map (LESSONS), instead of the real game's finish pen with a spectate item.
- Time skip: when 80% have finished, the timer drops to 20 s.
- Karma thresholds; pickaxe tier (iron, no enchant); snowballs don't regenerate.

## Maps (8 originals, each a different crossing type; pure-Java generators per `..\MAP_BRIEF.md`)
Follow the 17 design requirements in `tgttos_maps.md`. Common frame: straight run 70–140 blocks; flat start strip ~36×7 with a start line; goal = low-walled chicken pen 10–14 across with a tall landmark above it visible from the start (outdoor maps); always at least two lines to choose from; team-colour concrete never used in the map itself; 2–3 dominant colours + trim; scenery frames the lane on both sides and behind the goal but never sits on the route; void below/aside; each map has its own silhouette.

1. **Laundry Line** (bridging) — rooftop to rooftop over a 24-block void gap threaded with washing lines and hanging sheets; chimney stepping-stone line.
2. **Dam Face** (climb) — 36-high curved dam; zig-zag ladders and catwalk terraces every 8 blocks vs pillaring straight up; bridging kit.
3. **Bookshelf Run** (parkour, scale gag) — mouse-sized players on a giant library shelf; books, pencil bridge, dictionary stack; goal in a teacup. Snowballs only.
4. **Orchard Drop** (drop) — from a windmill gallery 60 up onto floating apple targets with water centres, short run to the pen.
5. **Kite Festival** (elytra) — gantry floor drops at GO; 130-block valley glide through tethered kites and arches. Elytra + 2 fireworks.
6. **Frozen Canal** (boats on ice) — 20-wide ice canal with two bends, low footbridges, moored barges.
7. **Ticket Gates** (doors/choice) — station concourse: 12 turnstiles (open / jammed / drop to a lower corridor, re-rolled each round), then a crossing through two parked carriages. Indoor.
8. **Quarry Steps** (pushing + bridge) — narrow haul-road ledges where players punch each other off vs bridging straight across the bowl. Bridging kit.
Spares for later: Hot Springs (geyser launch pads), Conveyor Bakery (moving platforms), a riptide-trident water map.

Validators: route check per map (flood-fill from start strip to the pen using that map's movement: walk/jump, bridge budget ≤ 64 blocks, glide, boat), start strip holds 40, landmark visible from start (ray-cast, outdoor maps), connection post-pass, staircase helper, no team-colour concrete in the build. Renders per map: top-down, side elevation, start view, 1 angle, in `Tgttos\previews\`.

## Acceptance checklist
1. 6 rounds on 6 different maps; sidebar shows round n/6 and map name.
2. Round timer 100 s; pre-round ~15 s; nobody leaves the start strip before GO; kit already in hand.
3. Kits match the map type (bridging: exactly 64 team concrete + pickaxe + 5 snowballs; parkour: snowballs only; boat: seated in a boat; elytra: wearing elytra + fireworks).
4. Only player-placed concrete can be broken; map blocks can't; nothing can be placed on non-bridging maps.
5. Punches and snowballs knock back without damage; players don't collide; no fall/fire/lava death.
6. Falling out returns the player to the start within 1 s with a fresh kit and a chat message.
7. Punching a goal chicken finishes the player once: title, effect, chat line, spectator mode; chickens can't be killed or pushed out.
8. Scoring matches the table (placement, top-10 bonus, team bonus split); non-finishers 0.
9. Karma: spam-punching raises the puncher's knockback taken, shows a red glow, then locks punching; decays.
10. Round ends when all finish or at 0:00; 80% time skip works; placed blocks are cleared.
11. After round 6 `finish` is called once; players back in the MCC lobby; `/mcm stop` cleans up mid-round (boats, elytra, blocks, tasks).
12. Works with 1 player; quitters don't block round end.
13. Every map passes its route check; best-case crossing ≤ 35 s, and the built route is completable without placing blocks where one exists.
14. Turnstile outcomes are re-rolled each round and at least 4 of 12 are open.

## Difficulty and nuance (owner requirement — the old maps were "just rush forward, way too easy")
Every map must make players think and fail sometimes. Mandatory per map:
- **A real decision in the first 3 seconds:** at least two clearly different lines (safe/slow vs risky/fast, or built route vs own blocks), with the fast line saving ≥ 20% time but punishing a mistake with a fall/respawn. Document both lines and their estimated times in the README.
- **A skill gate, not a corridor:** something that an average player fails 1–2 times per round — e.g. the bridging gap is longer than a lazy straight bridge allows before someone breaks it (blocks ARE breakable by rivals), obstacles in the gap force the bridge to bend, parkour has 3–4 block jumps and head-hitters on the fast line, the drop targets on the fast line are small, glide arches on the low route are tight, boats must brake for bends and duck bridges, half the turnstiles fail.
- **A choke point where the crowd matters:** one place 3–5 wide where punching/snowballs and karma come into play, with a wider but longer way round.
- **Resource pressure on bridging maps:** 64 blocks must NOT be enough to pillar/bridge the whole map carelessly: total straight-line bridging need ≥ 40 blocks, and falling refills only to the starting kit.
- **No free run:** nowhere may a player sprint more than ~15 blocks in a straight line without an obstacle, jump, turn or decision (start strip excluded).
- Time targets: best line by an expert 20–35 s; median player 50–80 s; a noticeable share of weaker players should NOT finish every round within 100 s. The validator reports the block-count and jump-difficulty profile of the safe line and the fast line per map.
