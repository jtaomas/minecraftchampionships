# Battle Box v2 — spec (mechanics + arenas)

Source of truth for this rebuild. Research is in `..\VISUAL_REFS.md` (section "Battle Box") and `..\MAP_PROFILES.md`; read both. Goal: play like the real event's Battle Box, with our own arenas, kit names and item names.

## Mechanics
- **Matchups:** each tournament team is a side. Round robin over up to 9 rounds; every round, each pair gets its own copy of the arena (all copies use the same arena theme that round; pick a new random theme each round — "map rotation" — config `rotate-maps-each-round: true`). Odd number of sides → one bye. Solo players are grouped into squads (target team size 4). 1 player → practice rounds vs empty side.
- **Phases per round:**
  1. **Kit select (15 s; 20 s in round 1):** players stand in their side's spawn room behind glass gates. A **kit selection menu** (chest GUI) opens automatically and can be reopened with a hotbar item; selecting shows the kit's items. Each kit can be taken by at most one player per side (more when the side has >5 players); unpicked players get a random free kit at the end. Show who picked what (action bar/sidebar line per side).
  2. **Fight (60 s):** gates open with a sting. One life. On death → spectator over your arena for the rest of the round (no item drops, no respawn). **No friendly fire.** Items can't be dropped or shared.
  3. **Round end (5 s):** result titles, fireworks in the winner's colour over their arena, short results line per matchup, then next round.
- **Centre:** a 3x3 pad framed by a yellow/black hazard-stripe border (on a 1-block plinth). Players place their **team-coloured concrete** (config option `centre-block: concrete|wool`; wool gives shears, concrete gives an efficiency pickaxe that only breaks centre blocks). Players can break enemy blocks on the pad. Blocks may only be placed on the 9 centre cells (and nowhere else).
- **Winning a round:** full 3x3 in your colour = instant win. At time-out: more centre cells wins. Equal → draw. Wiping the other side does not end the round by itself (survivors still have to fill the centre), but if a side has no players left alive, the round ends immediately when the other side completes the centre (normal rule) — and if time runs out, the surviving side wins ties (alive-count tiebreak) — keep it simple and clear in the README.
- **Scoring (scaled to the server's ~100-point games):** elimination 3 to the killer (last damager within 10 s counts); round win 8 to each member of the winning side; draw 3 to each member; bye 4 each. Show "+N" feedback.
- **Kits (5, our own names; loadouts vary per arena theme — define a loadout table per theme):**
  - **Medic** (sustain): melee weapon + bow + a few arrows + regeneration bursts.
  - **Marksman** (ranged): bow or crossbow + more arrows + one mobility charge.
  - **Tinkerer** (utility): Punch bow or crossbow + a utility item (harming fuse-burst, leaping, blindness fuse-burst…).
  - **Runner** (mobility): axe (sometimes Knockback) + 2–3 speed charges (+ TNT on some themes).
  - **Brawler** (frontline): sword + golden apple, or crossbow + firework rockets, or harming splash potions.
  - Everyone also gets team-coloured leather armour, the centre blocks (16) and the centre tool.
- **Custom items (our names):**
  - **Charges** (consumable, e.g. a dyed amethyst shard with a name): right-click = apply the effect to yourself; hit an enemy with it = apply to them. Effects: Speed, Regeneration, Levitation, Invisibility, Escape (short dash + speed).
  - **Bursts** (thrown, snowball-based): apply an area effect where they land (Regeneration to teammates only, Levitation/Glowing to enemies only).
  - **Fuse Bursts** (thrown): effect applies after ~1.5 s with a ticking particle ring (Harming, Blindness) to enemies only.
  - Use custom names, lore, and `custom_model_data` hooks (resource pack constants live in `C:\Users\LeonS\Desktop\Mcc\resourcepack\McmGlyphs.java` — don't depend on it yet; just leave TODO hooks).
- **HUD:** boss-bar timer per phase; sidebar: round x/9, phase, top matchups; action bar: opponent, your kills, centre count (you vs them) with coloured squares.

## Arenas (the visual part)
- Follow the Battle Box notes in `..\VISUAL_REFS.md` closely for scale and technique: **~29x29 inner floor** (configurable), fully enclosed, walls ~12–14 tall with a ceiling or sky-painted upper walls (light-blue concrete with white block clouds, maybe a pixel sun), glass **spectator windows** in the long walls framed in quartz/stone, a checker/pixel frieze band near the top, spawn rooms at both ends with glass gates, mirrored cover: **6–10 themed props per half** (every prop "is" something), 1–2 raised walkways/rooftops per side reached by stairs, water channels or lava pits as obvious hazards where the theme suits, bright flat lighting.
- **3 original themes** (not the official ones — avoid market/bazaar, canal, park/splash, campsite, spaceship, factory/thermodynamics, classic plain): **Greenhouse Conservatory** (glass + white iron frames, planters, flower beds, potting benches, a fountain, hanging vines), **Toy Workshop** (giant toy blocks, a wooden train track, workbenches, paint pots in primary colours, a rocking horse sculpture), **Ski Lodge** (snowy chalet, log piles, ski racks, frozen pond hazard, fir trees, fireplace). Each has its own kit loadout table.
- Spectator gallery: a corridor behind the windows where eliminated players are placed (or free spectator), plus a decorated holding area between rounds.
- All arena copies are generated per round from the pure-Java generator (see `..\MAP_BRIEF.md` offline rendering) and reset fully between rounds.

## Quality loop
Offline-render each theme (2 angles + `--top` + a cut-away dump without the ceiling), study against the reference notes, iterate ≥2 times. PNGs in `BattleBox\previews\`.
