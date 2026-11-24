# Rocket Spleef Rush — Research & Design Notes

## Sources
- [MC Championship Official — Rocket Spleef Rush](https://www.mcchampionship.com)
- [MCC Wiki (Fandom) — Rocket Spleef Rush](https://mcchampionship.fandom.com/wiki/Rocket_Spleef_Rush) (`Journey`, `Temple`, `Santorini`, `Overlode`, `Classic`, `Construction`)
- Community gameplay breakdowns on Elytra hopping, Updrafts, platform crumbling, and knock-off credit (Reddit r/MinecraftChampionship)

---

## 1. Core Mechanics & Kit (1.21 Paper)
- **Rounds:** 3 rounds per game. Last player/side standing wins the round.
- **Unbreakable Elytra (Spacebar Gliding):**
  - Every player wears an **unbreakable Elytra** in `EquipmentSlot.CHEST`.
  - `allow-double-jump` defaults to `false` (`p.setAllowFlight(false)`) so pressing Spacebar mid-air reliably deploys the Elytra without flight-toggle interference, enabling authentic **elytra hopping** and aerial strafing.
- **Rocket Launcher (`BLAZE_ROD`, right-click):**
  - Fires straight-flying rockets with **swept per-tick raycast hit detection** so fast projectiles never clip through 1-block-thick floating islands or bridges.
  - Blasts a `2.7`-block sphere out of the arena (no block drops) and imparts directional knockback within `4.5` blocks—including to the shooter for **rocket-jumping**.
  - Deals zero HP damage; eliminations occur strictly by falling below the void threshold (`elim-y: 64`).
- **Updrafts (`FEATHER`, right-click):**
  - 2 Updraft charges per round (`updraft.charges-per-round: 2`, configurable).
  - Right-clicking the Feather launches the player steeply upward (`vertical-velocity: 1.35`) with a small forward boost so they can recover from knock-offs and re-engage Elytra glide.
- **Knock-Off Credit:** Falling into the void within `10s` of taking an enemy rocket blast awards `+5` KO points to that enemy (no credit for teammates or self-hits).

## 2. Dynamic Crumbling & Floating Sky-Pads
- **Outer-to-Inner Crumbling:**
  - During the final 40 seconds of `ACTIVE` play, outer-edge blocks progressively warn (`RED_CONCRETE` for `1s` with sound cue) and vanish.
  - During `OVERTIME`, crumbling accelerates rapidly (`35` blocks every 5 ticks) inward toward the center, preventing stalemate camping.
- **Mid-Round Floating Sky-Pads:** On active rounds (especially `journey_isles`), periodic floating emergency pads spawn in mid-air with particle beacons to reward aggressive Elytra repositioning.

## 3. Four MCC-Inspired Floating Void Maps (`mcc_rocketspleef`)
Original voxel builds inspired by MCC Rocket Spleef Rush map archetypes:
1. **`journey_isles`** *(Journey-inspired)*: Floating sky-archipelago of tapered mossy/stone islands with custom trees, lantern-lit bridges, high outer sanctuary ring, and mid-round spawning sky-pads.
2. **`sky_temple`** *(Temple-inspired)*: Sandstone & prismarine floating temple ruins featuring a tiered central ziggurat shrine, 4 corner domed pavilions, and stepped aqueduct bridges.
3. **`santorini_cliffs`** *(Santorini-inspired)*: Whitewashed quartz & calcite Mediterranean cliff-islands topped with vivid blue terracotta/concrete domes, stepped plazas, colonnade arches, and bell-towers.
4. **`overlode_core`** *(Overlode-inspired)*: Dark deepslate sky-foundry with glowing cyan/magenta crystal conduits, layered octagonal catwalks, suspended gantries, and a central energy core.
