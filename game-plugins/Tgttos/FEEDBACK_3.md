# Owner feedback 3 - Kite Festival (after playing the playtest-2 build)

He likes the direction; "definitely still can be improved".

1. **Decoration**: "a lot more decorative elements, there's still a lot of blank space". Fill the dead air and bare walls along the whole course (kites, banners, balloons, lantern strings, floating islands, tethered structures, scenery framing each gate). Look at how Glider's Challenge / Elytra Rush / Hylur fill space and copy ideas.
2. **Three lines obvious at a glance**: strong colour framing, big labels / markers before each screen, one colour language. Easy, medium and hard holes clearly apart. Coordinator's reading: vary the layout (some screens side by side, others stacked vertically), but never so close that one flight line covers two tiers.
3. **Harder holes must clearly pay more**: keep the bonus scheme but make it felt - message on passing ("+2 hard line"), running bonus shown, values on the start signage. Reconsider whether tier points 1/2/3 with cap 8 is generous enough; pick and document.
4. **Last wall**: a single-block-wide opening everyone must thread, tall enough to be fair (validated).
5. **Crashing kills**: slamming into a wall or a hard fall kills the racer and returns them to the last checkpoint ("They need to go through the holes, otherwise they die"). Must be reliable, including slow face-plants that stop the glide. Touching the course floor = death: not decided, config flag, default off.
6. **Moving obstacles**: "pretty cool" but too small, too few, too easy to avoid. Much bigger (sweep the whole opening), more of them, in the CENTRE of the flight path (one on the left side "does nothing" - find and fix). Do NOT make them faster.
7. "start off with little amounts of fire... I don't really know" - unclear; if it means the fire rings: little fire early, more later.

## Goal design correction (all eight TGTTOS maps)

"The ending is always a whole row of chickens, not a small box that's hidden. We should stop doing these fake things, they ruin the purpose of the game. The ethos isn't that it's hard to find the ending - everyone knows where the ending is, but it's hard to get there because everyone is punching each other."

- Every map ends at an unmistakable finish: a long ROW of goal chickens across the full width of the finish line (no queueing for one chicken), visible from far away, clearly framed (banner / arch / colour). No small hidden or awkward pen, no climbing or searching at the end.
- Decoy chickens removed entirely (default 0; the scattered decoys and the "Punch me!" / "haha lol" fakes off; code kept behind config, default off). No fake-looking pens or misleading dead ends.
- Each map's last stretch: the route to the finish must be obvious.
- Keep the chunk-hold / persistence fix. Validators check the row: count, spacing, a reachable standing spot in front of every chicken, visible from the approach.

## Dam Face holes (playtester, on the playtest-2 build; world coordinates of mcc_tgttos, Dam Face = slot 1, origin 600, 64, 0)

- world 582,65,15 (local -18,1,15, the left training wall, i.e. the racers' right side): "why is the right side of the map a killbox and a hole into the void????"
- world 603,87,35 (local 3,23,35, inside the dam under terrace 3): "there are still gaps in the map here"
- Coordinator: close every remaining way to fall out or get trapped in a lethal pocket on both sides; find out why the holes validator missed them and strengthen it (flood-fill from every cell a player can reach - walking, jumping, falling, knocked ~6 blocks by a snowball - and fail on any reachable void column or enclosed hazard pocket that is not a declared, visible hazard).

## Round timing (owner)

- round-seconds: 300 (was 100); time-skip finished-fraction: 0.5 (was 0.8). Already set in config.yml and on the live server by the coordinator - do not revert. Once half the racers have finished, the remaining time shortens (to seconds-left, 20 s).

## Change of direction (owner, via coordinator)

"To Get To The Other Side is supposed to be jumping around, building blocks, punching each other with snowballs. We drifted too far from that. Move all the elytra races, boat races and dropper maps out into a different game mode - call it Race for now (separate from Ace Race). Keep the original TGTTOS maps. Get rid of the Orchard one, that one's rubbish."

- New plugin game-plugins/Race (McmRace, game id "race", world mcc_race, /race admin command): Kite Festival, Frozen Canal, the imported-map loader (plugins/McmRace/imported/<id>/, map.yml unchanged, types elytra/boat/drop), MapFeatures/FeatureRuntime, Paster/chunk holds, crash rules, boat handling, validators and render tools. Own package com.mcm.games.race, copied code, no plugin dependency.
- Race rules (defaults, not researched): several rounds on different maps; finish by crossing a finish line / gate volume (pluggable goal; imported chicken rows convert to a finish line); placement points; checkpoints, bonus lines; time skip at 50%; round 300 s; no snowballs (flag); punching off.
- Orchard Drop deleted.
- TGTTOS keeps Laundry Line, Dam Face, Bookshelf Run, Quarry Steps, Ticket Gates.
- Correction (owner): Race is ELYTRA and BOATS only. The three imported dropper maps (thedrop_trees, h4rs_nether, h4rs_end) go to the core's built-in Multi-Stage Dropper (another agent). No drop type / kit / validators in Race; drop removed from TGTTOS with Orchard Drop. Imported loader classes left readable for that agent (PackedBlocks, map.yml format); plugin-src (core) not touched.
