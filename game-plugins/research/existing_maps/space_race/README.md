# Space Race — forensic dossier (A3)

**Map:** SPACE RACE (MCC Ace Race Recreation): IMPROVED, by Agnor
**Source:** `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Space_Race\Space_Race.zip` (extracted)
**World version:** 1.18.1, DataVersion 2865, `version` 19133
**Live world:** `mcc_spacerace` (Multiverse import)
**Scale:** 14,198 chunks · 296,699 non-air blocks · 179 block states · 29 command blocks · 22 signs · 0 containers · 0 entities
**Author's listing:** planetminecraft.com/project/space-race-mcc-ace-race-recreation (gallery captions used below as external corroboration only)

Confidence tags on every claim: **H** = directly observed in NBT / block dump / render,
**M** = inferred from two independent observations, **L** = guess.

---

## 1. What this map is

A single-lap Ace Race course built as a chain of floating set pieces around one giant building.
The author's own gallery names the pieces: **Start/Finish Line, Trident Rings, Pool Island, Water
Features, Luminous Cavern, Elytra Islands, Main Building Entrance** (H: listing captions).

- The **main building** (`x -44..163`, `z -116..60`, `y 83..218`) is a black hangar whose interior
  is the "Luminous Cavern": a **lethal lime-glass floor**, invisible light grid, two sandstone
  catwalk arches, seven red levitation pads and two diamond elytra rings set into the walls
  (H: block dump + renders).
- The **left chain** runs south from the building: Pool Island A → 64-block water slide → Pool
  Island B (with a 21-sign blank leaderboard) → water bridge → Platform C → two **Trident Rings**
  → launch pad → **Elytra Islands** (H).
- The **right chain** is a separate 200-block-easterly staircase: bottom island → two islands →
  a space station → **Elytra Ring D**, pointing back north to the start (H).
- The link between the two chains is a single **orange mega pad** (~71-block lift) at the east tip
  of the Elytra Islands (H).

It is a **loop**: out through the west side of the building, down the left chain, across the
mega pad, up the right chain, and home on an elytra glide. The world's scoreboard has
`LapCheck1`, `Minutes`, `Seconds`, `milliseconds` and `Restart` objectives, and two stale player
scores (4:44 and 5:59) from a test run (H: `data/scoreboard.dat`).

---

## 2. The box: kit, rules, containers

### Start kit (H: `playerdata/00000000-0000-0000-0000-000000000000.dat`)
| Slot | Item | Notes |
|---|---|---|
| 0 | Trident, **Riptide II** | named "Trident", yellow |
| 4 | Carrot on a Stick | named **"Restart Attempt"**, red |

Caveat: the test player (`00000000…`, creative) carries both; the survival racer `9ed6d016…`
(AgnorGD) carries **only the trident** (H). No command gives a kit, so the live start kit is
whatever the player joins with — there is no enforced loadout.

No elytra item. Elytra is meant to come from the diamond rings. The `Restart` objective uses
`minecraft.used:minecraft.carrot_on_a_stick` (H), but **no command reads it** — the restart item
does nothing in this world (H).

### World rules that shape play (H: `level.dat` GameRules)
| Rule | Value | Effect |
|---|---|---|
| `keepInventory` | true | death is cheap; you keep the trident |
| `fallDamage` | false | only the void and the kill blocks kill |
| `doImmediateRespawn` | true | instant respawn |
| `doDaylightCycle` | false | permanent midnight (`DayTime=18000`) |
| `doWeatherCycle` | true | rain comes and goes (matters for Riptide) |
| `commandBlockOutput` | false | silent |
| `maxCommandChainLength` | 65536 | — |
| Difficulty | 0 (peaceful) | — |
| Default gamemode | **creative** (GameType=1) | the author's listing says to `/gamemode survival` |

### Permanent effects
Every tick, one repeating command gives **all players dolphins grace for 999,999 s, amplifier 1**
(`20,112,14`, H). The saved survival player carries effect id 30, amplifier 1, duration
19,999,980 (H: `level.dat` Player), i.e. the effect is already applied and continuously refreshed.

### Containers and entities
**None.** 0 chests, 0 barrels, 0 shulkers, 0 hoppers/dispensers, 0 armour stands, 0 item frames,
0 displays, 0 boats/minecarts/markers (H: full chunk scan). There is nothing to open and nothing
to loot. `data/entities.csv` records this explicitly.

---

## 3. Command blocks: the whole system

29 command blocks in one control room under the start platform (`x 10..23`, `y 111..112`,
`z 12..28`). Full export with exact text: `data/command_blocks.csv`.

| Group | Qty | Coords | Command | Verdict |
|---|---|---|---|---|
| elytra grant | 8 | `14..20,112,12` + `14,112,14` | `execute at @a if block ~ ~ ~-1…-7 diamond_block run item replace entity @p armor.chest with elytra` | **broken axis** (see F1) |
| elytra clear | 4 | `12,112,12` red sandstone · `12,112,14` water · `22,112,12` red concrete · `22,112,14` gray concrete | `execute at @a if block ~ ~-0.35 ~ <block> run item replace entity @a armor.chest with air` | works |
| levitation small | 1 | `17,112,16` | red concrete → `levitation 2 23` | works |
| speed 30 | 1 | `18,112,16` | red concrete → `speed 2 30` | works |
| levitation mega | 1 | `19,112,16` | orange concrete → `levitation 1 70` | works |
| speed 255 | 1 | `20,112,16` | orange concrete → `speed 1 255` | works |
| speed II | 1 | `16,112,14` | magenta terracotta → `speed 2 4` | works |
| world spawn | 1 | `11,112,16` | magenta terracotta → `setworldspawn` | **map-wide side effect** (F6) |
| kill | 2 | `21,112,16` bedrock · `23,112,16` lime glass | `kill @a` | **global kill — see F13** |
| dolphins grace | 1 | `20,112,14` | `effect give @a dolphins_grace 999999 1 true` | works, unconditional |
| actionbar | 1 | `13,112,16` | `title @a actionbar "You are in 1st place"` | **always on** (F5) |
| countdown | 3 impulse + 3 chain | `10,111,28/25/22` + `11,111,28/25/22` | `title` / `subtitle` | works |
| gate reset | 1 | `10,111,19` | `/fill 28 115 24 6 117 24 air` | works once, never restored |

All 22 repeating blocks are `facing=down`, `conditional=false`, `auto=1` (irrelevant for
repeating) and run every tick (H). The four impulse blocks face east and are clocked by a
six-repeater loop at `x=8, z=19..28`; the stone button at `6,116,28` is the start trigger
(H wiring; M that the button starts the loop).

`LastExecution` proves the countdown order: 2425233 (subtitle `>3<`), 2425249 (`>2<`),
2425265 (`>1<`), 2425281 (fill) — 16 ticks apart, so 2.4 s for 3-2-1-Go (H).

---

## 4. Findings, ranked

### F1 — The elytra grant is wired on the wrong axis (H command text, M consequence)
All eight grant commands test `~ ~ ~-1` … `~ ~ ~-7`: a **horizontal** (north) offset at foot
height. None tests `~ ~-0.35 ~`, the block actually under the player. A player flying through a
vertical diamond hoop has air at foot level inside the opening, so the command does not fire.
It can only fire if a diamond block sits at the player's exact foot height within 7 blocks north
— i.e. standing on or against the 1-block frame. **Result: elytra is effectively unobtainable;
the clear commands work, so elytra can only be lost.** Every flight leg of the intended route is
therefore impassable in survival.

This is the single most important finding. The author's sibling-map changelog says "Elytra only
available when flying through ring", so the intent is clear (H external) and the implementation
is a one-character mistake (M).

### F2 — No finish, no checkpoint, no lap detection (H)
Nothing in the 29 commands detects a region, a score, or a finish. `LapCheck1` is never written.
The only completion feedback is a static actionbar. The lap exists only in the geometry.

### F3 — "You are in 1st place" is unconditional (H)
Repeating command `13,112,16` gives every player that actionbar every tick. It is decoration, not
ranking. In a multiplayer race everyone is told they are first.

### F4 — The start gate never closes again (H)
`/fill … air` clears the 23×3 glass wall at `z=24`; no command places glass. Lap 2 begins with the
gate open, so the start ritual only works once per world load.

### F5 — Start cues contradict each other (H)
Spawn yaw is `-179.99` = facing **north** into the building. The quartz runway behind the player
has a large blue arrow pointing **south**. The only text sign ("START ->", lime, glowing, at
`6,116,29`, wall-facing **east**) points across the platform, not along the run. Three cues, three
directions.

### F6 — Any magenta block moves the world spawn (H)
`setworldspawn` fires on *any* magenta glazed terracotta (723 blocks, including the trident-ring
platforms and the right-hand station). Stepping on one relocates the world spawn to that pad. A
death later in the lap respawns the player mid-course, not at the start.

### F7 — The palette is also the trigger set (H)
Red sandstone is the island material **and** the elytra-clear trigger; gray concrete is the
building **and** a clear trigger; lime glass is the cavern floor **and** the kill trigger. The
map's most common materials are all "interactive", so effects fire constantly and cannot be read
from the build.

### F8 — The only scoreboard surface is blank (H)
21 acacia wall signs at `x -143..-139`, `z=176`, `y=174..178`, set into a light-blue glass window
above Pool Island B's basin. All four lines empty, black, non-glowing, facing east. It is a
leaderboard that nothing populates. `data/signs.csv` has the full text (empty).

### F9 — The cavern is a kill room with no visual warning (H)
The entire floor is lime stained glass, and lime glass under feet runs `kill @a` (see F13 — it
kills the whole lobby, not just the player). From above it reads as a glowing green floor, not a
hazard. The only safe path is the two catwalk arches, which are the same sandstone colour as the
islands. One step off = a lobby-wide death.

### F10 — The main building hides its own exit (H)
The route leaves through Elytra Ring A: a 7×9 opening (9×11 frame) in the west wall at `y 165..175`, 50 blocks
above the floor, 1 block thick, invisible from inside until you are on the catwalk. There is a
second ring (C) at `x=88, y 205..215` leading east, with no indication which one is the route.

### F11 — Distance between chains is held together by one pad (H)
The left chain ends at the Elytra Islands (`z 358..430`); the right chain starts 70 blocks east
(`x 108..127`). The only link is the 3×3 orange pad at `41,192,419`. Miss it and there is no
second cue and no way back except death.

### F12 — The map is well tuned, which proves the intent (M)
Levitation maths: red = 0.05 × 24 × 40 ticks ≈ **48 blocks**; orange = 0.05 × 71 × 20 ≈ **71
blocks**. Ring A sits 40 blocks above the top cavern pad, Ring B 48 above the west-platform pad,
Ring D 48 above the station pad, the right-chain top 57 below the orange apex. Four independent
pairs match. The author tuned pads to rings; only the grant check is wrong.

### F13 — Both kill planes are global: one player's foot kills the whole lobby (H command text, H surfaces, confirmed by A12)
Two repeating blocks, `conditional=false`, run every tick:

```
21,112,16  execute at @a if block ~ ~-0.35 ~ minecraft:bedrock            run kill @a
23,112,16  execute at @a if block ~ ~-0.35 ~ minecraft:lime_stained_glass run kill @a
```

`execute at @a` iterates every player and tests the block under **that** player; the `run` clause
then executes `kill @a`, where `@a` is **every player in the dimension** — for this single-world
map, the whole lobby. So a single player standing on either material kills everyone: other racers,
players waiting at spawn, anyone in the overworld.

Standable trigger surfaces (topmost block of the column):

| Material | Columns | Bounds | Where |
|---|---|---|---|
| lime stained glass | **6,090** | `x -32..131`, `z -73..32`, top `y=107` (a few at 185) | the **entire Luminous Cavern floor** |
| bedrock | **1,852** | `x -76..88`, `z -86..3`, `y 154..185` | the cavern "reactor" mass and west props |

Consequences:
- Any racer who falls off the catwalk in the cavern wipes the lobby (F9 already makes that floor
  lethal; F13 makes it lethal to everyone).
- A spectator or late joiner walking on the cavern floor wipes the lobby.
- With `doImmediateRespawn=true` and `keepInventory=true` the result is a simultaneous respawn, not
  an item wipe — but if any player has touched a magenta pad, `setworldspawn` (F6) has moved the
  respawn point, so the whole lobby can come back **mid-course** rather than at the start.
- This is also the map's simplest grief vector: one player, one block, whole server.

A12's independent 1.21 syntax pass found **no syntax failures** in the 29 commands and confirmed
these two global kill planes. Nothing was changed (research only). For a rebuild the condition must
target the player who triggered it (`kill @p`, `execute as @p run kill @s`, or `damage @s`), never
`@a`.

---

## 5. Direction and readability — what works, what fails

### Works
1. **One-way water.** Slides and drops physically carry the player; you cannot swim up a
   17-block waterfall. Direction is enforced by geometry, not signage (H).
2. **Skyline chain.** From each island the next is the only large object in view; the renders show
   a clean silhouette sequence with no competing geometry (H/M).
3. **Warm/cool split.** Orange asteroids vs gray/black tech vs cyan diamond: the eye can tell
   "island" from "machine" from "gate" at distance (H).
4. **Hazard yellow/black.** Used only on interactive rings (trident tops, elytra surrounds) — the
   one consistent "this is a game object" colour (H).
5. **Glowing edges.** Sea-lantern strips outline pod, station and portals, so the build reads at
   night (the map is permanently midnight) (H).
6. **The trident rings are unmissable** — 18-block glass tubes with hazard crowns (H).

### Fails
1. **Text is absent.** One sign in the whole course, pointing the wrong way; 21 blank signs.
2. **No state feedback.** Gates do not open/close visibly, no checkpoint lights, no lap counter,
   no finish title. The player never knows if they are on lap 1 or 3.
3. **Contradictory start cues** (F5).
4. **Two exits from the cavern, no marker** (F10).
5. **Interactive = decorative** (F7), so colour cannot be trusted as a signal.
6. **The lethal floor looks safe** (F9).
7. **The right chain is visually disconnected** from the left (F11).

**Verdict:** the map is readable as a *sculpture* and unreadable as a *race*. A first-time player
can see where the next pretty thing is; they cannot see what is a gate, what is a pad, what is
lethal, or whether they have finished.

---

## 6. What is reusable in a premium rebuild

1. **Elytra ring as landmark + grant.** The idea is strong: a unique cyan hoop that is both the
   visual target and the mechanic. Rebuild it with a volume check
   (`execute if entity @a[x=…,dx=…,dy=…,dz=…]`) or an advancement, never a block-under-feet test.
2. **Tuned levitation instead of jump pads.** 48-block and 71-block lifts matched to ring heights
   are predictable and cheap. Keep the two-tier system (small red / mega orange) but give each
   tier its own unique block.
3. **Trident rings.** Vertical water tubes with hazard crowns are a great "swim up" beat and read
   from 100 blocks away. Add a visible water source/particle at the top.
4. **Constant-level water slides.** All source blocks, glass sides, coloured ribs: cheap, robust,
   and direction-locking. Reuse at 3 blocks wide with a 1-block lip.
5. **Lethal-floor cavern + catwalks.** A strong hazard beat, provided the floor is visually
   distinct (e.g. a different glass colour plus a visible "do not touch" pattern).
6. **Start ritual.** Button → 3-2-1 titles → glass gate opening is the right shape. Add a closing
   fill, a lap counter and a finish title.
7. **Palette economy.** Three materials = 67% of the map. A premium rebuild can afford more
   variety, but the warm/cool split is worth keeping.
8. **Chain-of-islands silhouette.** Each island is the next target; no map needed. Keep the
   100–150 block spacing.

**Do not reuse:** material-as-trigger (F7), static "1st place" (F3), the 21 blank signs (F8), or
the one-shot gate (F4).

### Position against the other Ace Race maps
Cross-map ranking is A13's job; this dossier deliberately does not analyse Clouds 2, City
Marathon, Yeti Set Go or the TGTTOS maps. On its own terms Space Race is the **most vertical** of
the four Ace Race recreations (a 135-block cavern climb, 48/71-block lifts, a 186-block return
glide) and the **least instrumented** (no finish, no checkpoints, no lap logic).

---

## 7. 1.18.1 → 1.21 upgrade notes

### World conversion
| Item | Value | Action |
|---|---|---|
| DataVersion | 2865 (1.18.1) | 1.21.1 = 3955 (1.21 = 3953). Vanilla DataFixer upgrades on first load (H/M) |
| `level.dat version` | 19133 | fine |
| Enabled datapacks | `vanilla`, `mod:forge`, `mod:worldedit` (H) | **remove `mod:*`** or expect "unknown pack" logs; the world does not need them |
| `WasModded` | 1 (H) | harmless |
| `serverconfig/` | Forge folder (H) | ignore on Paper/vanilla |
| Entities | 0 (H) | no entity-format migration risk |
| Chunk format | 1.18 `block_states` palettes | read by 1.21 DataFixer; no manual work |

### Command syntax — all 29 still valid in 1.21.1
Checked against the 1.21 command tree (H: each string; M: version mapping):

- `execute at @a if block ~ ~-0.35 ~ <block> run …` — valid.
- `item replace entity @a armor.chest with minecraft:air|minecraft:elytra` — valid.
- `effect give @a minecraft:dolphins_grace 999999 1 true` — valid; effect id unchanged.
- `effect give @p minecraft:levitation|speed …` — valid.
- `setworldspawn` (no args) — valid.
- `kill @a` — valid.
- `title @a title|subtitle|actionbar {"text":…}` — valid JSON text in 1.21.1. 1.21.5+ prefers
  SNBT components but still parses JSON strings (M — verify on the exact target).
- `/fill 28 115 24 6 117 24 air` — valid.
- No removed selectors, no `data`/`execute store` tricks.

No command should fail to parse. The **semantic** bugs (F1–F7) travel unchanged into 1.21.

### Command-block NBT
- `LastOutput` still parses; `LastExecution`/`SuccessCount`/`TrackOutput` unchanged (M).
- The three chain blocks at `11,111,22/25/28` hold the subtitles; they are `conditional=false`,
  so they run whether or not the impulse succeeded (H). Keep that in mind if you rewrite.

### Resource pack
- `resources.zip.zip` is `pack_format 6` (1.16.2–1.16.5) (H `pack.mcmeta`).
  1.21.1 wants **34**; 1.21.2+ wants 42/46+ (M). The pack will show as incompatible and be
  disabled unless forced.
- Block textures use unchanged paths (`assets/minecraft/textures/block/…`) and will apply if the
  pack is forced (H listing).
- GUI overrides (`gui/icons.png`, `widgets.png`, `container/inventory.png`) are **not** safe:
  `icons.png` was split into individual sprites in 1.20.2 and widget layouts changed. Expect
  missing/misaligned GUI art in 1.21 (M).
- `sea_lantern.png.mcmeta` (`frametime: 5`) is still valid (H).
- Recommend re-packing under A10's guidance rather than shipping format 6.

### Behavioural changes to re-test in 1.21
1. **Rain + Riptide.** `doWeatherCycle=true`; Riptide needs rain or water. 1.21 weather timing is
   unchanged, but verify the saved `raining=1` survives the import (M).
2. **Void death + `doImmediateRespawn`** still instant (M).
3. **`keepInventory` + `item replace armor.chest`** interaction: the clear commands will delete
   any chestplate, including one picked up later (H logic).
4. **Creative default.** `GameType=1` survives; players will fly unless switched to survival (H).
5. **Sign NBT** is auto-migrated to `front_text`/`back_text`; the blank signs stay blank (M).

### Recommended pre-flight (for A12, not done here)
- Load once on 1.21.1, then check the 3 chain blocks still hold their subtitle commands.
- Confirm the `mod:*` datapack entries do not block world load.
- Force-load the pack only in a test client; do not enable it globally.
- Verify `LastExecution` on the countdown blocks resets (the clock needs the button, not a tick).

---

## 8. Evidence index

| Path | What it is |
|---|---|
| `renders/route_overlay.png` | **labelled reconstructed lap** over a top-down material map |
| `renders/overview_iso.png` | whole map, iso angle 0 |
| `renders/overview_iso_a2.png` | whole map, iso angle 2 |
| `renders/start_iso_a0.png` / `_a2.png` | main building exterior, both sides |
| `renders/pod_iso.png` / `pod_iso_a2.png` | start pod: checkered grid, gate, runway, redstone |
| `renders/hall_interior_iso.png` | Luminous Cavern interior (pool, murals, props) |
| `renders/hall_catwalk_plan.png` | plan of the two catwalk arches + seven pads |
| `renders/archA_iso.png` | Elytra Ring A in the west wall |
| `renders/islA_iso.png` / `_a2.png` | Pool Island A + black portal + slide mouth |
| `renders/islB_iso.png` | Pool Island B basin, portal, slides |
| `renders/signwall_iso.png` | the 21-sign blank leaderboard window |
| `renders/rings_iso.png` | Trident Rings + Platform C |
| `renders/islF_iso.png` | Elytra Islands + orange mega pad |
| `renders/right_chain_iso.png` | space station + descending islands + Ring D |
| `renders/finish_left_iso.png` | orange pad at the tip of the Elytra Islands |
| `renders/heightmap_grid.png` | whole-world top-down with grid |
| `renders/section_x17.png`, `section_x-141.png` | vertical sections (pod / slide) |
| `data/command_blocks.csv` | all 29 commands + type/facing/auto/last-exec + trigger/effect/scope/reset/1.21 risk |
| `data/signs.csv` | all 22 signs, block state, colour, glow, text |
| `data/blocks.csv` | 179 block states with counts and shares |
| `data/checkpoints.csv` | every de-facto gate, axis, width, grant/clear |
| `data/entities.csv` | proof of zero entities/containers |
| `data/route.md` | full leg-by-leg with coordinates |
| `data/palette.md` | palette, accents, lighting |

`REFERENCE_IMAGES.md` maps each image to what it shows and what to take from it.
`STATUS.md` is the done/blocked/open list.

---

## 9. Server configuration cross-check (A12, 2026-11-25)

A12 reports the live `mcc_spacerace` is served with **Multiverse gamemode = survival** and
**`allow-flight=false`**. Neither is part of the world save (H: the save's `GameType=1`, creative;
the server config is outside this dossier). Their combined effect on this map:

| Setting | Effect on Space Race | Confidence |
|---|---|---|
| Survival instead of creative | The author's own listing says to `/gamemode survival`, so survival is the intended play mode. But the elytra grant is broken (F1), so the flight legs are impassable; in creative a player could at least fly them manually. Survival removes that escape hatch. | H listing + H F1; M combined |
| `allow-flight=false` | Creative/spectator players are exempt (they hold `mayfly`). Survival players gliding with a **working** elytra are exempt while `isFallFlying()` is true. Survival players held airborne by **levitation** (1–2 s) and long falls between pads can accumulate airborne ticks; the vanilla anti-flight check can kick with "Flying is not enabled on this server". This needs an in-game test — it is a risk, not a confirmed failure. | M/L |
| Survival + `keepInventory=true` | Deaths are cheap for items (trident kept) but the respawn point may have been moved by F6. | H |
| Survival + `fallDamage=false` | Falls do not kill; only the void and the two kill planes do. | H gamerule |
| Survival + `allow-flight=false` + levitation pads | A player launched by the 71-block orange pad and missing the landing falls for ~6 s; that is long enough to reach the anti-flight threshold before void death. Test the orange pad and Ring D return specifically. | L |

**Operational reading (no fix applied):**
1. The map cannot be completed in survival as shipped, because F1 blocks every elytra section.
2. `allow-flight=false` adds a second, independent way to fail on the levitation pads; it should be
   tested before any live event.
3. The two global kill planes (F13) turn any fall in the cavern into a lobby-wide event.
4. None of this is a 1.21 syntax problem — A12 found no parse failures. It is command *semantics*
   and server policy.

---

## 10. Open questions

1. **Did the elytra ring ever work?** The commands as saved cannot fire in flight (H). Either the
   saved command room is a later edit, or the author tested only in creative (the test player is
   creative, H). No way to tell from the world (L).
2. **Where is the finish line exactly?** The author's caption says "StartFinish Line" and the
   scoreboard has `LapCheck1`, so the pod is both. Nothing measures it (H). The alternative is the
   right-chain bottom island as a podium (M) — it is the highest point and the orange pad points
   at it.
3. **Was there a datapack?** `LapCheck1`, `Minutes`, `Seconds`, `milliseconds` and `Restart` all
   exist with stale scores but no writer. A timing datapack was probably removed before the
   download (L). No datapack folder exists in the world (H).
4. **The 3 empty chain blocks** at `11,111,*` hold the subtitles; the missing `LastOutput` on some
   blocks suggests a WorldEdit paste (L). They still run (H `conditional=false`).
5. **Rain state on import** — if the 1.21 server clears weather, Riptide outside water stops
   working. Needs an in-game check (L; A12 territory).
