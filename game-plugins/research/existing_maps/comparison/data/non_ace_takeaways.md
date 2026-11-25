# Non-Ace-Race takeaways — TGTTOS, Sky Battle, Parkour Tag, Decision Dome

Read-only synthesis. Evidence tags: **H** observed, **M** inference, **L** judgement. Cross-links point to
the owning dossier; this file does not re-analyse those maps.

---

## 1. TGTTOS (Badlands / Industry / Pits) — the portability lesson

### What the three maps are

All three are **single-player practice courses** driven by a small datapack and a clickable start sign:
Badlands has 2 functions (8 + 130 lines) and 0 command blocks; Industry has 7 functions and 0 command
blocks; Pits has 6 functions and 0 command blocks (`../tgttos_badlands/README.md` §3;
`../tgttos_industry/README.md` §3; `../tgttos_pits/README.md` §2; H). Each implements a start latch, a
countdown, a per-player stopwatch, a chicken-punch finish (`damage_dealt_resisted` on Resistance-boosted
chickens), a spectator hold and a global reset.

### What transfers

1. **The state-machine shape is right.** Start → countdown → playing → finish → spectator → reset is the
   same skeleton an Ace Race needs. Badlands' `countdown`/`playing`/`finished` objectives are a readable
   prototype of a round state machine (`../tgttos_badlands/README.md` §3; H). Take the shape, not the
   datapack.
2. **A finish detector can be cheap.** The chicken-resistance trick needs no plugin event and works for any
   chicken (`../tgttos_badlands/README.md` §7.2; H/M). Keep it only as a fallback for imported maps with no
   event hooks; a premium race should use a directional gate, because the chicken has no order, no pen and no
   team attribution.
3. **A finish should be a place.** Badlands' 45-long MCC wall, four towers and 1-wide chicken ledge is visible
   from the mesa (`../tgttos_badlands/README.md` §7.2; H). This is the composition to copy for an Ace Race
   finish, not the chicken.
4. **A barrier pen is better than a door.** 184 barriers, 140 of them the gate wall, plus a visible
   black-wool/quartz frame (`../tgttos_badlands/README.md` §2; H). It is cheap, invisible, and resets by
   fill. For Ace Race, scale the strip to 40 slots.
5. **The one-block offhand bridge is a strong optional mode.** Blue concrete in a warm palette is the only
   cool saturated player colour; the 1-block refill makes every placement count
   (`../tgttos_badlands/README.md` §7.2; H/M). It is not an Ace Race mechanic, but it is a good side-event or
   practice drill.

### What does not transfer

1. **Datapacks are version-bound, not portable.** On 1.21.1 all three packs are **disabled on import** and
   still use plural `functions/` + `tags/functions/`, `pack_format` 10 (or 9/10) and pre-1.20.5 item NBT
   (`../tgttos_badlands/README.md` §6; `../tgttos_industry/README.md` §1; H). Enabling them is not enough:
   the folder rename and item components are fatal. **Do not make a datapack the game runtime.**
2. **Single-player timer assumptions.** One finish removes `playing` from everyone; the first finisher's
   reset teleports all players; titles and countdowns target `@a`; the countdown scales 1/N with N players
   (`../tgttos_badlands/README.md` §7.3; `../tgttos_industry/README.md` §3.6; H). A 40-player race cannot
   inherit any of this.
3. **Approximate reset.** Pits has no global block reset; Industry's `block_reset` rebuilds 8 of 10 curtain
   segments; Badlands' marker+box cleanup misses 4 cacti (`../tgttos_pits/README.md` §2.5;
   `../tgttos_industry/README.md` §3.10; `../tgttos_badlands/README.md` F9; H). A reset that is "mostly
   right" is worse than no reset in a tournament.
4. **Sign-click start.** The click event is on the empty first line, not the visible "Start"; one global
   latch (`../tgttos_badlands/README.md` §6; `../tgttos_industry/README.md` §3.4; H). Use the runtime's start
   gate.

### The map-contract warning

`McmTgttos` v2.0.0 rejects the Badlands build because it counts **544 dyed-concrete decoration blocks** as
player concrete, requires **≥40 start spawns**, a pen and play-bounds, and the world has 1 spawn and no
metadata (`../tgttos_badlands/README.md` §7.5; H code + H blocks). **Lesson for Ace Race:** publish the map
contract before art. Either allow decoration concrete while tracking player placements, or paint the MCC
lettering in terracotta/wool. Do not discover the contract at import.

---

## 2. Sky Battle — loot, teams, reset

### What transfers

1. **Fixed loot, not RNG.** All 13 containers are pre-filled with the same 3×3 pattern, and the 93
   `item replace block` commands refill the same items. That is competitive fairness and a replayable route
   (`../minigames/sky_battle/README.md` §4.1–4.2; H). For Ace Race, fix the kit per section (trident,
   elytra, boots) and never randomise a movement item.
2. **Palette-scoped building.** The 38-block `CanPlaceOn` list is the arena's own palette, so bridging is
   possible on every intended surface and blocked everywhere else (`../minigames/sky_battle/README.md` §4.4;
   H). Even if Ace Race has no building, the principle — a whitelist of legal surfaces — is the right
   anti-cheat model.
3. **Team identity as geometry.** Four gift-box spawn pods at the cardinal points, four team canopies, four
   loot balloons. A player knows their side and the enemy sides from shape and colour
   (`../minigames/sky_battle/README.md` §8.3; H/M). For Ace Race, use team-coloured start rows and tab
   colours; do not build four separate spawn islands.
4. **Traffic-light cages.** Barrier → red → yellow → lime → air is a wordless start signal
   (`../minigames/sky_battle/README.md` §8.3 item 7; H). It is a strong alternative to a title-only
   countdown, and it can be scaled to 40 slots as a row of cage segments.
5. **A border that matches the architecture.** The nested circular damage rings (r50 → 5) match the circular
   gallery, unlike a square world border (`../minigames/sky_battle/README.md` §5.6; H). For Ace Race, the
   lesson is that bounds should follow the course's shape and be visible; do not copy damage rings.
6. **Reset discipline is a design feature.** The map's biggest failures are reset failures: only wool/cobweb
   is swept, and the bug reset destroys containers without recreating them
   (`../minigames/sky_battle/README.md` §8.2 items 5–6; H). Design the reset before the loot.

### What does not transfer

1. **XP level as the state machine.** Levels 8/9/10–13/14 encode lobby, countdown, team and dead; any XP
   change breaks the game (`../minigames/sky_battle/README.md` §8.2 item 7; H). Use explicit runtime state.
2. **`@e` border damage.** The wither selector is not filtered to alive adventure players; it can hit lobby
   players and non-player entities (`../minigames/sky_battle/README.md` §8.2 item 4; H). Damage only the
   intended racer.
3. **85 walk components / low canopy.** The ring's canopy dips leave 1–2 blocks of headroom at the diagonals;
   players cannot walk between galleries under cover (`../minigames/sky_battle/README.md` §2.2; H). A race
   course must be traversable at speed, not a set of islands that require a bridge.
4. **Invisible boundary and no HUD.** A barrier wall 13 blocks outside the build, no sidebar, no spectator
   handling (`../minigames/sky_battle/README.md` §8.2 items 11/13/14; H). All three are anti-patterns for a
   premium race.
5. **No time limit.** A mid stalemate can run forever (`../minigames/sky_battle/README.md` §8.2 item 9; H).
   Ace Race needs a race time limit and a DNF rule.

---

## 3. Parkour Tag — role pads and timer

### What transfers

1. **Role pads as physical choice.** Runner pad (chiseled stone) and hunter pad (chiseled blackstone) on the
   gallery; a barrier locks the hunter slot so only one player can take it; a glowstone pad unlocks it
   (`../minigames/parkour_tag/README.md` §4.2; H). For Ace Race, the analogous device is a **ready pad** or
   team-row pad, not a role pad; the idea of a physical, visible selection surface is sound.
2. **A 60-second bossbar with a clear style.** `notched_6`, max 60, name "Parkour Tag - Timer"
   (`../minigames/parkour_tag/README.md` §4.5; H). This is the best timer presentation in the set: one
   glance, no sidebar scan. Ace Race can use the same bossbar for the race clock or the final countdown.
3. **Opposed doors + colour-cycle countdown.** Runner and hunter enter from north and south; the doors fill
   black, then red → yellow → lime, with note-block hats and a bell
   (`../minigames/parkour_tag/README.md` §4.4; H). The colour cycle is the strongest wordless start theatre
   in the set and scales to a shared start gate.
4. **Stadium envelope with an invisible barrier roof.** The 400-block barrier plane at y27 is flush with the
   gallery floor, so spectators walk over the arena on an invisible "glass floor"
   (`../minigames/parkour_tag/README.md` §3.1; H). This is the cleanest spectator-deck trick in the set;
   reuse the idea for an Ace Race finish stadium.
5. **One self-contained arena.** 123 command blocks, 25 signs, all state inside a 44×59×46 envelope
   (`../minigames/parkour_tag/README.md` §1; H). Compactness and a single paste volume are real virtues.

### What does not transfer

1. **XP level as role state.** Level 4/5/6/7 encodes lobby/runner-selected/runner-in-game/hunter; any XP
   change breaks the role (`../minigames/parkour_tag/README.md` §4.1; H). Same anti-pattern as Sky Battle.
2. **Runtime redstone team split.** The runner team only forms while a command-placed redstone block at
   `1057 6 1001` exists; if it or the repeater fails, everyone stays on `pve` and cannot damage
   (`../minigames/parkour_tag/README.md` §4.1; H/M). A race runtime must not depend on a redstone state.
3. **Bossbar selector narrower than the arena.** The list is set once from a box at y15–20; a late joiner or
   a player on the top perches can be missed (`../minigames/parkour_tag/README.md` §4.5; H). Set HUD players
   explicitly from the roster, not from a box.
4. **Broken tellraw and wrong timer score.** `PT-C100` has an extra `}`; the timer-stop commands set the
   hunter's personal score, not the fakeplayer clock (`../minigames/parkour_tag/README.md` §4.11; H). Test the
   end-of-round text and the clock independently.
5. **Glowing runners vs hiding spots.** The map builds hiding rooms but gives every runner Glowing, so stealth
   is pointless (`../minigames/parkour_tag/README.md` §7.3; H). For Ace Race, do not add stealth geometry that
   the rules negate.

---

## 4. Decision Dome — playable Nocturne Orrery vs display model

### The comparison in one line

The downloaded Decision Dome is a **20,054-block, 53-wide display model**: 0 seats, 0 pod rooms, 0 stairs into
a 42-deep pit, no roof, no outer rail, and a dial that is invisible from the only walkable ring (0–18 of 25
sampled points; 0/25 from the north carpet) (`../decision_dome_recreation/README.md` §0/§2.2; H). The
Nocturne Orrery is a **451,515-block, 97-wide playable venue**: 10 pod rooms × 4 seats, 120 spectator spots
in three tiers, a 7-wide grand stair, 8/6 readable dial wedges, 40/40 sightline checks, and an API v2 runtime
(`../decision_dome_recreation/README.md` §6; H report values).

### What the display model does better — adopt the principle

1. **Team-colour verticals.** Ten full-height team concrete towers make the ten teams readable from every
   side; team colour is 26% of the build. The Orrery confines colour to a thin rim and reads as a dark bowl
   from distance (`../decision_dome_recreation/README.md` §6.1 item 1; H). **For Ace Race:** use team-colour
   verticals on the start/finish stadium (banners, light columns, gate posts), not solid towers.
2. **One unambiguous hero.** The gold crown with eight red jewels is the only object above the rim and reads
   in one glance (`../decision_dome_recreation/README.md` §6.1 item 2; H). **For Ace Race:** one finish
   topper or start arch, not three competing silhouettes.
3. **Compact budget.** 20k blocks, 85 types, four main materials plus gold
   (`../decision_dome_recreation/README.md` §6.1 item 3; H). Spend detail on the playable core, not the
   foundation.
4. **Ceremonial arrival.** A clear three-step red-carpet approach and a single plate
   (`../decision_dome_recreation/README.md` §6.1 item 4; H). **For Ace Race:** one ceremony axis into the
   start pen.
5. **Vertical drama, in moderation.** The 42-deep pit + 51-block shafts is a stronger section than a bowl,
   but it is unplayable. Adopt a **3–5-block reveal pit** under the finish/dial, not 42
   (`../decision_dome_recreation/README.md` §6.1 item 5; M).
6. **Human/lore layer.** 17 heads, 24 banners, two beehives, two armor stands give the annex a story
   (`../decision_dome_recreation/README.md` §6.1 item 6; H). A small winners/team-banner wall is cheap and
   memorable.

### What must never be copied

1. **A dial the player cannot see.** The terrace sees a mean 9.8/25 dial points; five of 24 standing
   positions see none; the crown and ten towers occlude it
   (`../decision_dome_recreation/README.md` §0/§2.7; H). If the finish/start has a hero surface, it must be
   visible from the player's approach.
2. **Solid pods with no door, room or seat.** The 10 pods are 51-block hollow shafts with 3×3 caps; they are
   not rooms (`../decision_dome_recreation/README.md` §2.5; H). A start/finish tower that looks enterable but
   is not is a lie.
3. **A 42-deep unrailed pit and no stair.** The only walkable ring is 4 wide; there is no outer rail and no
   stair into the pit (`../decision_dome_recreation/README.md` §2.2; H). Spectators and racers will fall.
4. **A display model used as a venue.** 0 seats, 0 spectator tiers, 2 command blocks, no vote/gate/tally/reset
   (`../decision_dome_recreation/README.md` §0; H). A beautiful sculpture is not a runtime.

### What this means for Ace Race

The Decision Dome is a **reference for start/finish stadium composition**, not a map to port: one hero
silhouette, team-colour verticals, a ceremonial axis, a shallow reveal, and a visible dial/scoreboard from
the approach. The Nocturne Orrery remains the playable reference because it has seats, stairs, sightlines and
an API.

---

## 5. Cross-mode summary

| Lesson | Source | Use in Ace Race? | Why |
|---|---|---|---|
| Small state machine (start → countdown → play → finish → reset) | TGTTOS | **Yes, as a shape** | The runtime must own it; the datapack is not portable |
| Finish as a physical place | TGTTOS Badlands | **Yes** | A destination reads better than an invisible trigger |
| Fixed loot / fixed kit | Sky Battle | **Yes** | Competitive fairness; no RNG movement item |
| Palette-scoped placement | Sky Battle | **Adapt** | Useful if the race allows building; otherwise anti-cheat bounds |
| Team-colour identity | Sky Battle / Decision Dome | **Yes** | 10 teams × 4 must read at a glance |
| Traffic-light / colour-cycle gate | Sky Battle / Parkour Tag | **Yes** | Wordless, scalable start signal |
| Role pads | Parkour Tag | **Adapt** | Good for ready/team selection; no roles in a race |
| 60 s notched bossbar | Parkour Tag | **Yes** | Best timer presentation in the set |
| Invisible barrier roof as spectator floor | Parkour Tag | **Yes** | Clean spectator deck over a finish stadium |
| One hero silhouette + compact budget | Decision Dome | **Yes** | Composition discipline |
| 42-deep pit / buried dial / solid pods | Decision Dome | **No** | Display model, not a venue |
| XP level as state | Sky Battle / Parkour Tag | **No** | Fragile; explicit runtime state only |
| Global `@a` effects, global spawn, global kill | Clouds 2/City/Yeti pads; Space elytra clear and kill planes | **No** | The defining multiplayer bug of the set |
