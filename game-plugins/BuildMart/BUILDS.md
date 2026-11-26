# Build Mart — builds, Blueprint Gallery and Gold Builds

Everything MCC's "Big Sales at Build Mart" is made of, and what this plugin does with it. Two parts:

1. **The catalogue** — every MCC build, its stars, its official render and how far our recreation has got (§6).
2. **The rules** — how teams see and pick the next build (the Blueprint Gallery, §3) and how the Gold Build works (§4),
   with the sources for every number.

Snapshot of the catalogue: **25 Nov 2026**. All 114 build files are in place; statuses are read from the files themselves
(§6, §8).

---

## 1. Where this comes from

| Source | What it gives |
|---|---|
| `research/mcc_archive/build_mart/bm-3/downloads/big_sales.wikitext` | the game page: goal, gallery composition, gold rules, coin payouts, full history table |
| `research/mcc_archive/build_mart/bm-3/downloads/list_builds.wikitext` | the build list: names, debut events, the 14 Golden Builds |
| `research/mcc_archive/build_mart/bm-3/build_list.txt` | the same list as data: id, name, first event, wiki image, GOLD flag (90 builds) |
| `research/mcc_archive/build_mart/bm-3/downloads/revamped_stars.json` | the published star ratings of the **P25+** build set (a different, smaller set of builds) |
| `research/mcc_archive/build_mart/bm-2/notes.md` | gallery displays, holograms, stars, gold booth, HUD, book |
| `research/mcc_archive/build_mart/bm-2/frames/gallery/` | the gallery itself: `01` overview, `03` pedestals + hologram, `05` empty pads, `09` selection button |
| `research/mcc_archive/build_mart/bm-1/notes.md`, `frames/004400…`, `zz_detail_cur_gold_plot_hologram.jpg` | the physical gallery deck and the gold plot with its arch and `???` curtain |
| `game-plugins/COINS.md` | the coin values this server pays |
| `src/main/resources/builds/*.json` | the recreations: id, name, stars, status, gold flag, layers |

---

## 2. How a build becomes a build

- One JSON per build in `src/main/resources/builds/`: a 7×7 themed floor, up to 10 layers of 7×7 block states, a
  palette of full 1.21.1 block states, a `reference` to the official render, a `stars` rating and a `status`.
- Every build was recreated from its official render in
  `research/mcc_archive/build_mart/bm-2/frames/builds/`. The white plinth, the nether-brick notch and the red carpet in
  those renders are the wiki's photo stand, not part of the build.
- `gold: true` marks one of the 14 builds MCC used as a Golden Build. It is metadata only: the same build can also appear
  as an ordinary blueprint, and only when the game is running it as the gold does it show 7 stars and the gold reward.
- The 24 builds that were in the game before the status convention have no `status` key in their file; they are listed as
  `done (legacy)` in §6.2.
- `stars` is metadata, not computed from block count (bm-2 §4). MCC never published a per-build star table for the
  classic builds, so the ratings here come from the authoring rubric in `bm-3/bm_build_brief.md` (1 = tiny, 5 = large
  with many rotation-sensitive blocks). The P25+ set *does* have published ratings (`revamped_stars.json`), but those are
  different builds under different names, so they are not used for the classic catalogue.

---

## 3. The Blueprint Gallery (as implemented)

MCC Pride 25 added the gallery ("Blueprint Update"), and it is what the game is played through today: there is no fixed
build order any more, the team picks what it wants to build.

| Rule | Value | Source |
|---|---|---|
| Plots in the gallery | **7** blueprint pads | wiki: "seven blueprint plots that can be chosen" |
| Star mix per restock | **2× 1★, 2× 2★, 1× 3★, 1× 4★, 1× 5★** | wiki: "The Blueprint Gallery" |
| Restock | every **90 s** | wiki; P25 update notes |
| Builds at once per team | **3** | wiki: "They can work up to three builds at a time" |
| What a selection delivers | a **reference copy** on the plot, the name/star/material hologram, and shopping-list pages | wiki; bm-2 §3 |
| Picking | press the **red button on the gold post** in front of the pad | bm-2 `frames/gallery/09_selection_cart_detail.png` (the "cart" is a button, not a vehicle) |
| Blueprints are exclusive? | **No** — several teams can take the same blueprint | bm-2 §1/§2 (three teams completed "Whale"); REBUILD_PLAN §1 F8 |
| Restock while in use | a pad whose build is on a team's plot **does not rotate out** until it is completed | bm-2 §2 |
| Empty pad | model removed, pad greyed, `This blueprint will appear soon...` | bm-2 §3 + `frames/gallery/05` |
| After a completion | the plot is freed and stays empty until the team picks again | wiki (selection model) |

**In this plugin** (`gallery.*` in `config.yml`):

- The gallery is a **3×3 pad lattice flush with the floor in the mall plaza** — the classic map's central hall (the
  classic map never had a gallery; MCC's gallery is in the central hall of the island map, bm-1 §2.3). Seven blueprint
  pads, the gold pad and one decorative checkered pad, which is exactly MCC's 3×3 arrangement (bm-2 `gallery/01`, `05`).
- Each pad is a 9×9 square: stone-brick rim, 7×7 build area that takes the build's own themed floor, and the model
  standing on it. Each has the name + star row + materials hologram above its front edge and a **gold post with a red
  button block** at the front (bm-2 `gallery/03`, `09`).
- Right-click the post: the build goes to the team's first free plot. Three at a time; a busy team is told to finish a
  build first. The pad keeps its build for other teams.
- Restock every 90 s replaces the seven pads (skipping any build currently on a team's plot), with a 2 s
  "will appear soon" gap. The tier list is `gallery.tiers` and can be changed.
- If a pad's tier ever has no build, that pad falls back to any build; today all five tiers are populated (the 1★ and 5★
  pools are two builds each, §6).
- `gallery.enabled: false` restores the old behaviour: three builds appear automatically, no gallery and no golds.

---

## 4. Gold Builds (as implemented)

| Rule | Value | Source |
|---|---|---|
| Difficulty | **7 stars** | wiki: "a difficulty rating of seven stars" |
| First one | **4:00** into the game | wiki: "The first Gold Build will appear 4 minutes into the game" |
| Rotation | every **2:00** | wiki: "with the Build being rotated every 2 minutes" |
| Window | **2:00** to finish it | wiki: "Each Gold Build is available for two minutes" |
| Failure | the blocks placed on the gold plot are **removed**; it cannot be completed later | wiki: "Failure to complete the Gold Build will result in the blocks placed in the Gold Build removed. Teams can not retroactively complete past Gold Builds." |
| Reference copy | **none** — you may only look at it in the gallery, or memorise it | wiki: "the Gold Build does not have a reference and can only be observed from the mart itself" |
| Plot | its own designated plot (ours: the first free plot instead, §7.1) | wiki: "has its own designated build plot" |
| Payout | MCC 14: **double** a regular build; modern MCC: 7★ × 20 coins = **140** | wiki History (MCC 14); wiki "Coin Rewards" + History (Fall Stars) |
| Which builds | the 14 builds tagged Golden on the wiki, all flagged `"gold": true` (§6) | `list_builds.wikitext`, `build_list.txt` |

**Look** (bm-1 `frames/004400_cur_gallery_gold_tendril_plot.jpg`, `zz_detail_cur_gold_plot_hologram.jpg`): a gold-framed
pad, a red/white striped arch behind it, a purple `???` curtain while there is no gold, and a floor hologram reading
`<Name> / Golden Build / ★×7` — no material list on the gold hologram. The HUD carries a `GOLDEN BUILD IN: 01:04` bar at
the top centre.

**In this plugin** (`gold.*` and `coins.gold-bonus` in `config.yml`):

- The gold pad sits in the gallery with the red/white arch and the `???` curtain. Before 4:00 the hologram counts down;
  when it opens the curtain disappears, the 7★ gold build stands on the pad, and a yellow boss bar counts the two minutes
  down. The plaza-floor hologram shows `★×7` and the time left.
- It is a normal selection: right-click the gold post and the build goes to your first free plot — **without a reference
  copy** (the plot behind it stays a bare quartz plot) and with a 7★ gold hologram instead of the star row.
- When the two minutes are up, every unfinished gold plot is cleared (the blocks are removed), its slot is freed and the
  team is told why. Completions already paid are kept. There is no partial pay for a gold at the end of the game
  (MCC excludes golds from the end-of-game percentage since MCC BG).
- `gold.enabled: false` turns golds off. `first-at-seconds`, `every-seconds`, `window-seconds` and `stars` are all
  configurable; in `/bm fast` they are compressed for testing.
- Admin: `/bm gallery`, `/bm gallery restock`, `/bm gold`, `/bm gold now`, `/bm gold clear`.

---

## 5. Coins

| Key | Value | Meaning |
|---|---|---|
| `coins.per-build` | 30 | MCC 3-13: coins **to each player** for a build nobody has completed yet (COINS.md) |
| `coins.decay-per-completion` | 2 | MCC 3-13: 2 less for every earlier completion of that build by any team |
| `coins.min-per-build` | 10 | **ours**: floor so a popular build is never worth nothing |
| `coins.gold-bonus` | 30 | **MCC 14**: a Golden build pays double a regular build, so it earns its value **plus** this (30 + 30 = 60 at the base value) |

The 30 is the MCC 14 value in this game's scale: MCC 14 introduced golds with "double the coins obtained compared to
regular builds", and this game's regular build is 30. The modern MCC gold (7★ × 20 = 140) sits on a scale where a 5★ is
100 and a normal build pays per star, which is not the scale this game uses (COINS.md). The key is added to the build's
own value, so a modern-scale gold would be `coins.gold-bonus: 110` (30 + 110 = 140), not 140.

The Employee of the Month bonuses (Entrepreneur / Line Cook / Quality Assurance, 100 / 50 / 25) belong to the P25+
scoring and are **not** implemented; this game pays the MCC 3-13 rules plus the gold bonus.

---

## 6. The catalogue

The game ships **114 builds**: the 90 of MCC's build list (the set the build workers recreated) plus the 24 that
were already in the game before that list. The two groups do not overlap (`fountain`, `penguin` and `stonehenge`
are only in the second group), so 90 + 24 = 114.

- **Stars** — the rating in the build file (1 = tiny … 5 = large with many rotation-sensitive blocks).
- **Reference** — the official render in `research/mcc_archive/build_mart/bm-2/frames/builds/` that the build was
  recreated from. All 114 have one.
- **Status**:
  - `done` — the file exists and matches its render block for block (hidden backs filled by obvious symmetry count);
  - `approximate` — the file exists but a real part is guessed (hidden interior, build bigger than 7×7×10 so it had
    to be cropped, an unclear block, a block that cannot be placed and was substituted);
  - `done (legacy)` — one of the 24 builds that predate the `status` field: the file has no `status` key and is
    treated as done, because it was reviewed and accepted before the field existed;
  - `pending` / `missing reference` — no build file yet / no official render to author from. Neither occurs now.
- **GOLD** — MCC used that build as a Golden Build. **All 14** are flagged `"gold": true` in their files, so the
  gold pad draws from the full MCC gold set.

Snapshot counts: **11 done, 24 done (legacy), 79 approximate, 0 pending, 0 missing reference** (114 files).

The star spread is 1★ 2, 2★ 39, 3★ 58, 4★ 13, 5★ 2 — the rubric rewards shape complexity, so most builds land on
2-3★. Every gallery tier is populated, but the 1★ and 5★ pads have only two builds each to draw from, so those two
pads will repeat builds more often than the others.

### 6.1 The 90 MCC builds

| id | Build | Debut | Stars | Reference | Status |
|---|---|---|---|---|---|
| a_maze_ing | a-Maze-ing | MCC P23 | 3 | BSABM_-_a-Maze-ing.png | approximate |
| blocksketball | Blocksketball | MCC 3 | 3 | BSABM-Basketball.png | approximate |
| biplane | Biplane | MCC 20 | 3 | BSABM-Biplane.png | approximate |
| birds_view | Bird's View | MCC 4 | 2 | BSABM-Birds_View.png | approximate |
| boot_camp | Boot Camp | MCC 20 | 2 | BSABM-Boot_Camp.png | approximate |
| business | BUSINESS! | MCC P22 | 3 | BSABM-BUSINESS!.png | done |
| captains_ship | Captain's Ship | MCC 14 | 4 | BSABM-Captain's_Ship.png | approximate |
| christmas_eve | Christmas Eve | MCC 13 | 3 | BSABM-Christmas_Eve.png | approximate |
| coin_casting | Coin Casting | MCC AS | 4 | BSABM-Coin_Casting.png | done GOLD |
| collage | Collage | MCC 10 | 3 | BSABM-Collage.png | approximate |
| construction_materials | Construction Materials | MCC R | 3 | BSABM-Construction_Materials.png | approximate |
| date | Date | MCC 11 | 2 | BSABM-Date.png | approximate |
| disk_jockey | Disk Jockey | MCC 4 | 2 | BSABM-Disk_Jockey.png | approximate |
| duckie | Duckie | MCC P21 | 2 | BSABM-Duckie.png | done |
| duck | Duck? | MCC 4 | 1 | BSABM-Duck-.png | done |
| easter_bunny | Easter Bunny | MCC 4 | 3 | BSABM-Easter_Bunny.png | approximate |
| easter_success | Easter Success! | MCC 4 | 3 | BSABM-Easter_Success!.png | approximate |
| fancy_flamingos | Fancy Flamingos | MCC R2 | 4 | BSABM-FancyFlamigos.png | approximate GOLD |
| fireplace | Fireplace | MCC 16 | 3 | BSABM-Fireplace.png | approximate |
| fried_egg | Fried Egg | MCC P21 | 3 | BSABM-Fried_Egg.png | approximate |
| frog | Frog? | MCC 8 | 2 | BSABM-Frog-.png | approximate |
| frog_jj | FROG! | MCC JJ | 3 | BSABM-FrogJJ.png | approximate |
| gamer_room | Gamer Room | MCC R | 3 | BSABM-Gamer_Room.png | approximate |
| ghostin | Ghostin' | MCC AF | 4 | BSABM-ghostin'.jpg | approximate |
| giant_jacuzzi | Giant Jacuzzi | MCC 10 | 3 | BSABM-Giant_Jacuzzi.png | done |
| gravestone | Gravestone | MCC 18 | 3 | BSABM-Gravestone.png | approximate |
| happy_birthday | Happy Birthday! | MCC 8 | 2 | BSABM-Happy_Birthday!.png | approximate |
| hot_cross_bun | Hot Cross Bun | MCC 4 | 2 | BSABM-Hot_Cross_Bun.png | approximate |
| hot_dog_stand | Hot Dog Stand | MCC P22 | 3 | BSABM-Hot_Dog_Stand.png | approximate |
| how_do_i_craft_this_again | How do I craft this again? | MCC 10 | 3 | BSABM-Craft_This_Again.png | approximate |
| iris | Iris | MCC 5 | 3 | BSABM-Iris.png | approximate |
| jasper | Jasper | MCC 10 | 3 | BSABM-Jasper.png | approximate |
| kitchen | Kitchen | MCC 13 | 3 | BSABM-Kitchen.png | approximate |
| lotta_grotto | Lotta Grotto | MCC 19 | 4 | BSABM-Lotta_Grotto.png | approximate GOLD |
| mcc_chess | MCC Chess | MCC P22 | 3 | BSABM-MCC_Chess.png | approximate |
| mcc_crown | MCC Crown | MCC 11 | 2 | BSABM-MCC_Crown.png | done |
| meltdown | Meltdown | MCC 24 | 3 | BSABM-Meltdown.png | approximate |
| mitsy | Mitsy | MCC 4 | 2 | BSABM-Mitsy.png | approximate |
| motorbike_ride | Motorbike Ride | MCC 10 | 2 | BSABM-Motorbike_Ride.png | approximate |
| mushroom | Mushroom | MCC 14 | 3 | BSABM-Mushroom.png | approximate |
| my_frst_hous | My frst Hous | MCC 8 | 2 | BSABM-My_Frst_House.png | approximate |
| mystery_boxes | Mystery Boxes | MCC P23 | 4 | BSABM_-_Mystery_Boxes.png | approximate |
| noxcrew | Noxcrew | MCC 4 | 4 | BSABM-Noxcrew.png | approximate |
| parkour | Parkour | MCC 3 | 2 | BSABM-Parkour.png | approximate |
| patio_party | Patio Party | MCC 10 | 3 | BSABM-Patio_Party.png | approximate |
| pizza | Pizza | MCC 16 | 3 | BSABM-Pizza.png | approximate |
| pkt_mini | PKT Mini | MCC P22 | 3 | BSABM-PKT_Mini.png | approximate |
| pool_table | Pool Table | MCC 13 | 3 | BSABM-Pool_Table.png | approximate |
| precious_ores | Precious Ores | MCC 17 | 3 | BSABM-Precious_Ores.png | approximate |
| pride_bunting | Pride Bunting | MCC P21 | 4 | BSABM-Pride_Bunting.png | approximate GOLD |
| pride_heart | Pride Heart | MCC P21 | 4 | BSABM-Pride_Heart.png | approximate GOLD |
| pudding | """Pudding""" | MCC 19 | 2 | BSABM-Pudding.png | approximate |
| race | Race! | MCC 8 | 2 | BSABM-Race!.png | approximate |
| railways | Railways | MCC 14 | 3 | BSABM-Railways.png | done |
| rainbow | Rainbow | MCC 3 | 3 | BSABM-Rainbow.png | approximate GOLD |
| rainbow_castle | Rainbow Castle | MCC PARTY 2 | 4 | BSABM-RainbowCastle.png | approximate |
| random_letters | Random Letters? | MCC P22 | 3 | BSABM-Random_Letters.png | approximate |
| robomingos | ROBOMINGOS | MCC AF | 4 | BSABM_-_ROBOMINGOS.png | approximate GOLD |
| rockem_sockem | Rock'em Sock'em | MCC 11 | 3 | BSABM-Rockem_Sockem.png | approximate |
| rocket_spleef | Rocket Spleef | MCC 20 | 2 | BSABM-Rocket_Spleef.png | approximate |
| sand_timer | Sand Timer | MCC PARTY 2 | 3 | BSABM-SandTimer.jpg | approximate |
| sandcastle | Sandcastle | MCC 6 | 3 | BSABM-Sandcastle.png | done |
| sea_temple | Sea Temple | MCC 3 | 3 | BSABM-Sea_Temple.png | approximate |
| seaweed | Seaweed | MCC P23 | 2 | BSABM_-_Seaweed.png | approximate |
| secret_recipe | Secret Recipe | MCC 11 | 3 | BSABM-Secret_Recipe.png | approximate |
| sightseeing | Sightseeing | MCC Party | 3 | Sightseeing-BSABM.png | approximate |
| snakey_worm | Snakey Worm | MCC 16 | 2 | BSABM-Snakey_Worm.png | approximate |
| snowflake | Snowflake | MCC 13 | 2 | BSABM-Snowflake.png | approximate |
| sticky_situation | Sticky Situation | MCC 17 | 3 | BSABM-Sticky_Situation.png | approximate GOLD |
| stone_archway | Stone Archway | MCC 16 | 3 | BSABM-Stone_Archway.png | done |
| stormy_day | Stormy Day | MCC 24 | 4 | BSABM-Stormy_Day.png | approximate |
| sunny_day | Sunny Day | MCC R | 3 | BSABM-Sunny_Day.png | approximate |
| swamp_brew | Swamp Brew | MCC 10 | 2 | BSABM-Swamp_Brew.png | approximate |
| sweet_holidays | Sweet Holidays | MCC 13 | 3 | BSABM-Sweet_Holidays.png | approximate |
| tennis_court | Tennis Court | MCC P23 | 3 | BSABM_-_Tennis_Court.png | approximate |
| the_ant | The Ant | MCC P22 | 5 | BSABM-The_Ant.png | approximate GOLD |
| tick_tock | Tick Tock | MCC P22 | 3 | BSABM-Tick_Tock.png | approximate |
| tiny_cafe | Tiny Cafe | MCC 17 | 2 | BSABM-Tiny_Cafe.png | done |
| the_mighty_piston | The Mighty Piston | MCC 14 | 5 | BSABM-The_Mighty_Piston.png | approximate GOLD |
| to_arms | To Arms | MCC 20 | 3 | BSABM-To_arms.png | approximate |
| trampampoline | Trampampoline | MCC 20 | 3 | BSABM-Trampampoline.png | approximate |
| treasure | Treasure | MCC 5 | 3 | BSABM-Treasure.png | approximate GOLD |
| treetop | Treetop | MCC 20 | 3 | BSABM-Treetop.png | approximate |
| trophy | Trophy | MCC AS | 3 | BSABM-Trophy.png | approximate GOLD |
| turtle_plush | Turtle Plush | MCC 14 | 2 | BSABM-Turtle_Plush.png | approximate |
| wedding_day | Wedding Day | MCC 20 | 3 | BSABM-Wedding_Day.png | done |
| whats_uphouse | What's Uphouse? | MCC AS | 4 | BSABM-What's_Uphouse.png | approximate GOLD |
| wheres_the_build | Where's the Build? | MCC 17 | 1 | BSABM-Where's_The_Build.png | approximate |
| wilson | Wilson | MCC 14 | 3 | BSABM-Wilson.png | approximate |
| witch_camp | Witch Camp | MCC 11 | 3 | BSABM-Witch_Camp.png | approximate |

### 6.2 The 24 builds that were already in the game

| id | Build | Debut | Stars | Reference | Status |
|---|---|---|---|---|---|
| art | Art | MCC 4 | 2 | BSABM-Art.png | done (legacy) |
| bad_granite | Bad Granite | MCC 3 | 3 | BSABM-Bad_Granite.png | done (legacy) |
| campfire_song | Campfire Song | MCC 11 | 2 | BSABM-Campfire_Song.png | done (legacy) |
| castle_wall | Castle Wall | MCC 5 | 2 | BSABM-Castle_Wall.png | done (legacy) |
| cheeseboard | Cheeseboard | MCC 4 | 2 | BSABM-Cheeseboard.png | done (legacy) |
| end_flower | End Flower | MCC 8 | 3 | BSABM-End_Flower.png | done (legacy) |
| forest | Forest | MCC 4 | 3 | BSABM-Forest.png | done (legacy) |
| fountain | Fountain | MCC 5 | 2 | BSABM-Fountain.png | done (legacy) |
| garden | Garden | MCC 3 | 2 | BSABM-Garden.png | done (legacy) |
| heart | Heart | MCC 8 | 2 | BSABM-Heart.png | done (legacy) |
| house | House | MCC 5 | 2 | BSABM-House.png | done (legacy) |
| island | Island | MCC 3 | 2 | BSABM-Island.png | done (legacy) |
| mine | Mine | MCC 3 | 2 | BSABM-Mine.png | done (legacy) |
| monument | Monument | MCC 6 | 2 | BSABM-Monument.png | done (legacy) |
| moon_base | Moon Base | MCC 3 | 3 | BSABM-Moonbase.png | done (legacy) |
| nether_portal | Nether Portal | MCC 10 | 2 | BSABM-Nether_Portal.png | done (legacy) |
| penguin | Penguin | MCC 8 | 2 | BSABM-Penguin.png | done (legacy) |
| present | Present | MCC 13 | 3 | BSABM-Present.png | done (legacy) |
| pumpkin_patch | Pumpkin Patch | MCC 11 | 3 | BSABM-Pumpkin_Patch.png | done (legacy) |
| snowman | Snowman | MCC 8 | 2 | BSABM-Snowman.png | done (legacy) |
| stonehenge | Stonehenge | MCC 3 | 2 | BSABM-Stonehenge.png | done (legacy) |
| swingset | Swingset | MCC 10 | 3 | BSABM-Swingset.png | done (legacy) GOLD |
| umbrella | Umbrella | MCC 5 | 2 | BSABM-Umbrella.png | done (legacy) |
| wooden_ritual | Wooden Ritual | MCC 10 | 2 | BSABM-Wooden_Ritual.png | done (legacy) |

---

## 7. Deviations and known gaps

1. **The gold has no dedicated fourth plot.** MCC gives the gold its own plot next to the three normal ones. This team
   room was generated with three plot pairs (`MallMap` v12, MCC 13 layout) and has no 7×7 of free floor for a fourth, so
   the gold is taken into the first **free normal plot** instead — the part that matters is kept: no reference copy, 7★,
   two minutes, blocks removed on failure, cannot be completed later. A team with all three plots busy must finish one
   before it can take the gold.
2. **The gallery is in the mall plaza.** The classic map (MCC 3-13) never had a gallery; the plaza is its central hall, the
   closest analogue to where MCC put the gallery in P25 (bm-1 §2.3). The pads are flush with the floor so the plaza stays
   walkable.
3. **No item icons in the holograms.** MCC's gallery holograms list materials with item icons (bm-2 §3); ours are text
   lines, because there is no gallery UI resource pack.
4. **Star ratings of the classic builds are ours** (rubric, §2). MCC published a per-build table only for the P25+ set.
5. **Employee of the Month is not implemented** (P25+ scoring, not the MCC 3-13 rules this game pays).
6. **Only two 1★ and two 5★ builds.** The rubric rated 58 builds 3★ and 39 2★, so the 1★ and 5★ gallery pads have a very
   small pool and will show the same build more often than MCC's did. Re-rating a spread of builds (`stars` in the build
   files) would even the gallery out; nothing else depends on it.

### Floor-only lava

`bad_granite` and `nether_portal` have lava in their **themed floor** (the lava pool in their render). That is pre-existing
floor decoration, part of the build's base rather than its materials: the floor is not counted in the shopping list, so
lava is **not** a build material and the mall does not have to stock it. The two build files are unchanged.

---

## 8. Refreshing the table

Each row's stars and status are read from `src/main/resources/builds/<id>.json` (`stars`, `status`; files without a
`status` key are the 24 legacy builds). To refresh the counts:

```bash
cd game-plugins/BuildMart
python -I -c "import json,glob,collections; print(collections.Counter((json.load(open(p,encoding='utf-8')).get('status') or 'done (legacy)') for p in glob.glob('src/main/resources/builds/*.json')))"
```

`done`, `done (legacy)` and `approximate` are all recreated; a build that lost its file would show up as `pending`.
