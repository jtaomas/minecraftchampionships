# McmSurvivalGames (`hungergames`)

Modern 1.21.1 MCC-faithful Survival Games / Hunger Games plugin for **MinecraftMondayCore** (Paper 1.21.1, Java 21).

## Commands
- `/mcm play hungergames`
- `/mcm map hungergames <id>` — force any imported real-world map (`breeze_island`, `holiday_resort`, `teweran_sg1`, `turbulence`, `sg4`, `kalas`) or procedural map (`city_district`, `woodland_basin`, `harbour_citadel`).
- `/mcm skipwait` — skips pre-game countdown or grace period.
- `/mcm fast on|off` — cuts countdowns to <= 1s.

## Imported Real-World Maps (`plugins/McmSurvivalGames/worlds/*/map.yml`)
`default-map: turbulence` is configured in `config.yml` (overridable via `/mcm map hungergames <id>`). When `default-map` is blank and `prefer-imported-maps: true`, matches pick randomly from loaded imported worlds:
- `breeze_island` *(Breeze Island)* — `boat: true`, radius 220
- `holiday_resort` *(Holiday Resort)* — `boat: true`, radius 250
- `teweran_sg1` *(Teweran SG1 — Howling Mountains)* — `boat: false`, radius 230
- `turbulence` *(Turbulence)* — `boat: false`, radius 240
- `sg4` *(Survival Games 4 / SG4)* — `boat: false`, radius 230 (`repo_world: sg4_world`)
- `kalas` *(Kalas SG)* — `boat: true`, radius 240 (`repo_world: sg_world`)

Each imported match copies a clean template into `mcc_hg_<id>` (skipping `uid.dat`, `session.lock`, `map.yml`, `level.dat_old`, `playerdata`, `stats`, `entities`), scans chests with a tick time budget, and locates 24 safe spawn pedestals around the Cornucopia ring.

## Procedural Maps (~124-block radius)
- `city_district` *(MCC City Living inspired)*
- `woodland_basin` *(MCC World of Cars - Forest/Farm inspired)*
- `harbour_citadel` *(Coastal/Waterways SG)*

## Researched MCC Mechanics (MC Championship Wiki)
- **Match Structure:** 11-minute match (`game-seconds: 660`, MCC 17+) with a **25s grace period** (`grace-seconds: 25`, MCC 16+), teammates spawning on adjacent pedestals, and **full saturation (`20.0f`)** at start (`MCC 8+`).
- **Infinite XP Enchanting & Anvils:** Players receive Lv. 30 at start and regain Lv. 30 after enchanting or using an anvil (with Lapis Lazuli auto-filled in enchanting tables).
- **3-State Coin Crates (MCC 18–19+):** 4 Coin Crates per match (`crate-count: 4`), clamped inside the shrinking border at landing time:
  - **White Beacon:** Incoming descent (`crate-drop-seconds`), trackable by the **Crate Tracker Compass**.
  - **Green Beacon:** Landed crate ready to contest.
  - **Red Beacon:** Being broken open at a fixed channel speed (`crate-break-seconds`; tools do not speed it up, and enemy PvP interrupts). Awards **`180` coins shared amongst the team** + 1.21 supply burst.
- **Last Stand Endgame (MCC 25+):** The `WorldBorder` smoothly converges to a playable final arena (`last-stand-radius: 20`), then players' max health decays by **`0.5❤` (`1 HP`) every `3s`** down to **`1❤` (`2 HP`)**.
- **Loot & World Rules:** No iron chestplates or iron axes in regular chests (`MCC 31`), boosted splash Harming potions (`MCC 31`), Wind Charges & Chorus Fruit (`MCC TCC`), no Fire Aspect books (`MCC TCC`), 1-enchant cap on Crossbows (`MCC TCC`), Snowballs (`MCC 19`), Boats (`MCC 14`/`16`), breakable leaves (`MCC P21`), and `vanilla` 1.9+ combat configured for `mcc_hungergames` and all `mcc_hg_*` worlds in `plugins/OldCombatMechanics/config.yml`.
- **MCC Coin Rewards (MCC 19+, base x1.0 before core multiplier):**
  - **Kill:** `65`
  - **Outlive (per elimination while alive):** `3`
  - **Coin Crate:** `180` (team pool)
  - **Team Placement (1st–10th):** `[650, 600, 560, 520, 480, 450, 420, 390, 360, 330]` (team pool)

## 1.21.1 Additions & Configurable Defaults (Not Fixed by Wiki Research)
The following values in `config.yml` are sensible defaults tunable after playtesting:
- `timings.countdown-seconds: 10`
- `timings.refill-at-elapsed-seconds: 270` (mid-game chest refill)
- `timings.crate-drop-seconds: 20` & `timings.crate-break-seconds: 6`
- `timings.last-stand-at-remaining-seconds: 90` & `border.last-stand-radius: 20`
- `combat.mace-max-damage: 11.0` (ultra-rare nerfed 1.21 **Heavy Mace** in Cornucopia/Coin Crates)
- `combat.kill-assist-window-seconds: 10` (attributes Wind Charge / Border / TNT deaths to last attacker)
- `combat.tnt-fuse-ticks: 45` (auto-igniting **Tactical TNT** with zero map-terrain grief)
