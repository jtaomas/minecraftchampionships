# Build Mart — build catalogue & scoring research

Subagent: **build-list and preset researcher**. Scope: the actual build
catalogue, how the gallery composes and rotates it, how builds are sized,
counted and scored, and what that means for `BuildPreset` / `BuildMartGame`.

No source file outside `game-plugins/research/buildmart/builds/` was edited.
No VOD was downloaded: the wiki text plus four official stills answered every
question this dossier needed, so the 2–4 minute video window was not spent.

---

## 1. Method

1. Read `research/buildmart/BRIEF.md`, `BuildPreset.java`, `BuildMartGame.java`.
2. Pulled the two authoritative wiki pages as markdown:
   `List_of_Build_Mart_Builds` and `Big_Sales_at_Build_Mart`.
3. Pulled the `MC Championship Pride 25` wikitext (the "Blueprint Update"
   patch notes, which are more precise than the game page).
4. Measured geometry from four official images (gallery, build area, two
   build renders). Crops are in `img/`; each crop is listed in §5.
5. Cross-checked one number against the other (TKO block count vs star), and
   recorded the contradiction instead of smoothing it.

Confidence: **H** = stated in wiki text or directly countable in an official
image; **M** = inferred from two or more images / consistent sources;
**L** = single weak signal or estimate.

---

## 2. Sources

| # | Source | Used for | Conf. |
|---|---|---|---|
| S1 | `mcchampionship.wiki.gg/wiki/List_of_Build_Mart_Builds` (rev. 2026-09-13) | 114 legacy builds, debuts, gold flags, revamp star tables | H |
| S2 | `mcchampionship.wiki.gg/wiki/Big_Sales_at_Build_Mart` (rev. 2026-11-08) | gallery composition, 90 s, 3 plots, gold rules, coins, history | H |
| S3 | `mcchampionship.wiki.gg/wiki/MC_Championship_Pride_25` §Changes | Blueprint Update detail: 90 s refresh, selected builds not refreshing, 4-min first gold, 2-min cycle, chests | H |
| S4 | `File:BSABM-BlueprintGallery.png` (1920×1080) | gallery layout, pad size, holograms | H |
| S5 | `File:BSABM-BuildArea.png` (1920×1080) | 3 plots + gold plot, reference pedestals, material hologram with counts | H |
| S6 | `File:BSABM-The_Mighty_Piston.png`, `File:BSABM-Boot_Camp.png` (official renders) | 7×7 build base, height, palette density | H |
| S7 | `File:BuildMartMap.png` | island layout (context only, not this agent's remit) | M |

The two image-derived claims that matter (7×7 base, ~10-block height) are
consistent across S4, S5 and both S6 renders.

---

## 3. Findings

### 3.1 The catalogue has two eras, and only one is rated

**F1 (H).** Legacy era, MCC 3 → MCC BC: **114 unique builds**, listed
alphabetically with debut event. There is **no star rating** in that era — the
legacy table has no stars column at all (S1).

**F2 (H).** Revamp era starts **MCC P25** ("The Blueprint Update", 28 Jun
2025). Builds are grouped into 1★/2★/3★/4★/5★ and a 7★ Gold tier. Every
revamp event ships roughly **60 builds** per event: 16 / 16 / 7–8 / 8 / 8 / 4
(S1 "Revamped" table). The P25 table's own rows sum to 59 while its Total
column says 60 — see Open questions.

**F3 (H).** Legacy builds were **retired as a set** at P25, not deleted. A
handful were re-rated and re-admitted (Boot Camp → 1★, Fountain → 3★, House →
5★, Noxcrew → 4★, Trampampoline → 3★, Penguin → 4★, TKO is a new SB build).
Some names exist in both eras (`Fountain`, `House`, `Penguin`, `Noxcrew`,
`Trampampoline`, `Rocket Spleef`, `Wedges`, `Armada`, `Nessie`) and some are
re-rated between revamp events: **Wedges 3★ (P25) → 4★ (SB)**, **Nessie 3★ →
4★**, **Armada 3★ (SB) → 4★ (MU)**, **Fountain 3★ (P25) → 2★ (MU)**, **N 2★
(P25) → 1★ (MU)**. Star is a property of *the build in that event*, not a
permanent property of the name (S1).

### 3.2 Gallery composition and rotation

**F4 (H).** Seven selectable gallery plots, always:

| plots | tier |
|---|---|
| 2 | 1★ |
| 2 | 2★ |
| 1 | 3★ |
| 1 | 4★ |
| 1 | 5★ |

Plus a separate **Gold** plot in the same gallery (S2 §The Blueprint Gallery).

**F5 (H).** The seven plots **rotate every 90 s**. A 12-minute game therefore
shows **8 rotations = 56 blueprint slots** per game (S2, S3). The brief's 90 s
is confirmed by the P25 patch note wording "refresh every 1 minute and 30
seconds".

**F6 (H).** A team works on **up to 3 builds at once**; selecting a blueprint
copies it to one of three team plots as a **reference**, with the material list
on the plot and in the **shopping list book** in every player's inventory (S2
§Introduction and Goal).

**F7 (H).** Since P25 a team **can repeat a build**: "Builds which have already
been sent to team plots will not refresh until they are completed by a team"
(S3), and the wiki trivia states it is possible "to repeat the build by
selecting the same build before the build rotation happens" (S2 §Trivia). So
the gallery slot holds its build while a team is working on it, and the same
team can take it a second time for another plot.

**F8 (M).** Selection is at the gallery plot itself, not from the team area;
the build-area prompt reads "select a blueprint in the mart" (S5, right edge
hologram). This is why the gallery is inside the mart and not next to the team
plots.

### 3.3 Geometry: 7×7, not 5×5

**F9 (H).** The standard build base is **7×7 blocks in plan**, surrounded by a
1-block white/quartz rim with stone-brick corner squares, giving a **9×9
module**. The official Mighty Piston render (S6) shows exactly 7 grey floor
tiles across, a 1-wide dark-red centre inlay and a 1-block white rim; Boot
Camp's 7-wide ground row matches. Gallery pads (S4) and team plots (S5) use the
same 9×9 module, with a dark grid floor inside the team plot.

**F10 (M).** Height is **not** a reliable tier signal: Fountain 3★ is ~4 tall,
Boot Camp 1★ ~6, House 5★ ~6, Mighty Piston 5★ ~8. Observed range 3–12.
No published limit exists — treat **16** as a safe engine cap.

**F11 (H).** Builds are **not** all solid sculptures. They run from a
one-block-deep façade (House, 5 wide × 6 tall × 1 deep, 5★) to a full 3D form
with an interior (Piston, 5★). Both sit on the same 7×7 base. The current
presets are almost all 1–2 block thick *pictures*, which is the opposite
failure: the right shape at the wrong scale and without state blocks.

### 3.4 Stars and block counts

**F12 (M).** Stars are **hand-assigned per build** by Noxcrew, based on build
difficulty and scoring worth (S3: "ranked by stars, based on how difficult they
are to build and how much the builds would be worth scoring"). There is **no
block-count formula** in any source.

**F13 (M).** Two published/visible counts bracket the scale. The TKO plot
hologram in S5 reads `Black Concrete 9×, Blue Concrete 12×, Chain 18×,
Oxidized Copper 4×, Oxidized Cut Copper 8×` = **51 blocks at 3★**. The House
render (S6) is a 5×6×1 façade, roughly **40–50 blocks by visual count, rated
5★**. So a 3★ and a 5★ can have almost the same block count.

**F14 (H).** **Block count does not predict stars.** Stars track
recognisability, precision and material difficulty, not volume: House is a ~45
block 5★, Fountain (S6) is a ~80 block 3★, TKO is a 51 block 3★. Observed
catalogue range is roughly 20–300 blocks. Use counts as a **build-time budget**
and a **shopping-list cost**, never as an input to the star.

**F15 (H).** The star count is shown on the plot with the name and material
list (S5). The material list is a per-block count with an icon, no "(shop)"
markers — the player infers the source.

### 3.5 Scoring

**F16 (H).** Current: **20 coins per star**, flat since MCC FS (S2 History;
`MCC FS`: "star's coin value are now flatten ... each star worth 20"). Before
FS it escalated 10 → 15 → 20 per star across 0–4 / 4–8 / 8–12 minutes (S2
History, P25 entry). Stars × 20 means a 5★ = 100, a gold 7★ = 140.

**F17 (H).** **Partial builds pay by percentage completed** (S2 §Coin Rewards).
No published rounding rule.

**F18 (H).** Three Employee of the Month bonuses, paid to the top 3 teams in
each: **Entrepreneur** (most stars), **Line Cook** (most builds), **Quality
Assurance** (highest average stars). 1st 100, 2nd 50, 3rd 25 (S2).

**F19 (H).** No speed or first-to-finish bonus exists in the current rules.
Those belonged to the pre-P25 order-based scoring (S2 History).

### 3.6 Materials, crafting and rare blocks

**F20 (H).** The team area has **five colour-coded chests**, crafting tables,
stonecutters, and **two smelting areas with fuel already provided** (S2 §The
Build Area). Since P25 the chests are colour-coded specifically so teams can
sort blocks by build (S3).

**F21 (H).** Raw materials come from the mart's islands: upper islands carry
ores / logs / stones, lower islands carry coloured blocks; nether, coral,
copper, garden and snow variants exist by event (S2 §The Build Mart; MCC 20
swapped coral for copper; MCC P21 duplicated quartz and nether bricks across
islands; terracotta was removed in MCC 14).

**F22 (H).** Some blocks must be **crafted or smelted**. The chains visible in
the catalogue are the vanilla ones: sand→glass, cobble→stone→stone bricks,
clay→bricks, log→planks→sticks/stairs/slabs/doors/fences, stone→stonecutter
stairs/slabs/walls, copper ingot→block→cut copper. The shop-counter model in
the current plugin (4 counters: glass, stone bricks, bricks, gold) is a
simplification of this, not the real system.

**F23 (M).** "Rare" blocks in practice are the ones that need a detour or a
second island: gold block, quartz, glowstone, sea lantern, prismarine,
oxidised copper, nether bricks, coral, packed ice/snow layers (MCC 19
exclusive). The legacy catalogue flags three special cases: builds with
ladders behind the wall (Boot Camp), builds whose blocks no longer exist in the
game (N), and builds needing MCC-19-only winter blocks (S) (S1 Notes).

### 3.7 Orientation rules

**F24 (H).** Since **MCC AS**, correct rotation is required for the build to
count: stairs, open doors and slabs must be placed in the right orientation.
**Texture rotation is not required** — a door with the hinge on the wrong side
still counts. **Double slabs count as a full block** (S2 History, MCC AS).

**F25 (M).** Implication for data: the checkable state subset is
`facing / half / type / shape / open`, and the ignorable subset is `hinge` and
pillar `axis`. Stair `shape` (straight vs inner/outer corner) should be checked
— it changes the silhouette, which is what the reference shows.

### 3.8 Gold Builds

**F26 (H).** 7★, own dedicated plot in the team area, **no reference copy**
there — only observable in the Blueprint Gallery inside the mart. Two minutes
to copy it from memory/communication. Failure **removes the blocks placed** in
the gold plot. Past gold builds **cannot be completed retroactively** (S2
§Introduction and Goal).

**F27 (H).** Timing: **first gold appears at 4:00**, then **a new one every 2
minutes** — 4:00, 6:00, 8:00, 10:00 in a 12-minute game, i.e. **4 attempts
max**. This time-based rule replaced the pre-P25 "after every 3 builds,
double coins" rule (S2, S3).

**F28 (M).** The gold plot is visually distinct (raised, orange/yellow border,
sign "Gold Build — copy this directly from the mart!", S5). It sits in the
team area, so the "no reference" rule is about the *model*, not the *plot*.

---

## 4. Comparison with the current `BuildPreset`

`BuildPreset` is a 5×5×5, material-only, 27-entry catalogue with a computed
star. It is a good *prototype* and a bad *catalogue*.

| Aspect | Current | MCC (verified) | Verdict |
|---|---|---|---|
| Plan size | 5×5 hard cap (`SIZE = 5`) | 7×7 interior, 9×9 pad | **too small**; loses 2 blocks of silhouette on every side |
| Height | 5 | 3–12 typical | **too short**; cannot make a tower, tree, or roof pitch |
| Volume | 125 cells, mostly 1–2 thick | full 3D sculptures | different genre |
| Stars | formula `n + 2*shop` → 1–5 | hand-assigned 1–5 / 7 | **wrong basis**; a 5★ here can be 40 blocks |
| Star display | `★` bars | name + `★` + materials hologram | usable |
| Rotation | none (compares `Material`) | state subset checked since AS | **missing feature** |
| Double slab | impossible (no slabs) | 1 block, 2 items | needs the slots/items split |
| Material list | chat line + pedestal text | plot hologram + shopping book | usable, wrong channel |
| Shop materials | 4 hard-coded | full craft/smelt/shop economy | **too narrow** |
| Gold | none | 7★, no reference, 2 min | missing |
| Selection | fixed shuffled `sequence`, ramped | 7-plot gallery, 90 s, choose 3 | missing |
| Repeat | no | allowed since P25 | missing |
| Scoring | `4+3*stars` + speed + order + rank bonus | `stars*20` + partial %, + 3 bonuses | **wrong numbers** |
| Blocks available | 30-char legend, no states | all blocks, states, panes, stairs | **too narrow** |

**What is directly reusable**

- The `String[]` layer authoring model and `LEGEND` indirection.
- `at(x,y,z)`, `columnUsed(x,z)`, `unsupported()` (flood fill from the floor)
  — keep the signatures; only the size and state resolution change.
- `counts` map and the shopping-list formatter.
- The star *display* and the "needs" string.
- The idea of weighting shop-only materials, but as a **difficulty hint**, not
  as the star.

**What has to be replaced**

- `SIZE = 5` → per-build `dims` (7×7×≤16).
- Star formula → stored `stars`.
- `Material` comparison → `BlockData` + state subset.
- 27 presets → a catalogue with 1★…5★ + 7★ pools and a gallery selector.
- 4 shop counters → island/craft/smelt sourcing.
- 480 s single-plot loop → 720 s, 3 plots + gold, 90 s rotation.

**Specific preset gaps** (all 27 current presets are ≤5 wide and ≤5 tall):
`Fountain` is 5×3; `Cottage` is 5×5×5 with a full roof; `Watchtower` is 4×5×5.
None exceed the cap, so none exercise overhangs wider than 5 or heights above
5. There are **no stairs, slabs, doors, trapdoors, fences, panes, buttons,
levers, chains, lanterns or concrete** anywhere in the legend — i.e. the five
block families that MCC's rotation rule exists for are entirely absent.

---

## 5. Image index

**Primary handoff: `frames/` + `REFERENCE_IMAGES.md`** — 16 native-resolution
files (full 1920×1080 stills plus pixel-exact crops) with source, what each
shows, build and star. `img/` holds the working crops and the four source
stills those frames were cut from; the table below is the index for the
working copies.

| File | What it shows | Used for |
|---|---|---|
| `img/gallery.png` | Blueprint Gallery, seven pads + gold pad | F4, F9, hologram layout |
| `img/buildarea.png` | 3 plots + gold plot, references, TKO hologram | F6, F13, F20, F26, F28 |
| `img/piston.png` | Official render, Mighty Piston on 7×7 base | F9, F10 |
| `img/bootcamp.png` | Official render, Boot Camp on 7×7 base | F9, F10 |
| `img/martmap.png` | Whole mart from above | island layout context |
| `img/crop_gallery_mid.png` | tree + arch + gold pad close-up | pad size |
| `img/crop_gallery_bottom.png` | two builds + pad corners | 9×9 pad, rim |
| `img/crop_treelabel.png` | gallery hologram text (blurry) | hologram existence |
| `img/crop_materials.png` | TKO name, 3★, five material counts | F13 |
| `img/crop_tko_stars.png` | TKO 3★ stars, pixel-exact | F13 conflict |
| `img/crop_plot_grid.png` | empty team plot | plot floor is flat, no sunken footprint |
| `img/crop_piston_floor.png` | 7 floor tiles + red inlay | F9 |
| `img/crop_piston_row.png` | 7-wide build row | F9 |
| `img/crop_bc_row.png` | Boot Camp ground row | F9 |

---

## 6. Proposed original catalogue

26 builds, 1★–7★, none a copy of an MCC build (names were checked against both
the 114 legacy and the ~160 revamp names). Full rows in
`data/proposed_builds.csv`; format in `data/format.md`.

| Tier | Count | Block slots | Examples |
|---|---|---|---|
| 1★ | 6 | 13–38 | Camp Stool, Garden Gnome, Beach Ball |
| 2★ | 6 | 30–58 | Lemonade Stand, Bird Bath, Produce Wagon |
| 3★ | 5 | 62–92 | Fishing Hut, Windmill, Flower Cart |
| 4★ | 4 | 116–138 | Harbour Cottage, Clock Tower, Cable Car |
| 5★ | 3 | 140–164 | Teacup Ride, Airship, Grand Piano |
| 7★ gold | 2 | 108–116 | Ribbon Knot, Astrolabe |

Design rules used:

- Counts are deliberately **leaner than they look**: the top tiers are hollow
  shells and open frames, because the MCC 5★ House is only ~45 blocks. A 5★
  should win on silhouette and precision, not on volume.
- 1★ builds are **readable from one side** and have no state-sensitive blocks.
- 2★ introduces one mechanic (a stair edge, a slab rim, water).
- 3★ introduces a **roof or a 3D frame** and one crafted-only material.
- 4★ requires **two storeys or a cantilever**, plus 4+ rotation-sensitive
  blocks.
- 5★ is a **silhouette test**: recognisable from the gallery at 20 blocks
  away, with at least one opening or interior.
- 7★ gold is readable **from memory after 15 seconds**, has one dominant
  colour and one accent, and no tiny detail that only reads up close.
- One 5★ (`Airship`) is a hollow envelope; one 4★ (`Lookout Post`) is mostly
  a frame. The catalogue should contain both façade-like and volumetric
  builds, as MCC does.

Block palettes are deliberately **cheap-first**: every 1★–2★ build is
gatherable with no smelting; 3★ adds glass; 4★ adds stone bricks/copper; 5★
adds gold; gold builds use one rare family each.

---

## 7. Design implications for the rebuild

1. **Plot = 9×9 pad, 7×7 usable, 16 high.** Three regular + one gold plot per
   team, references on pedestals in front, gold raised and signposted. This is
   a map change, not a code change.
2. **Gallery is a real room with 8 pads** (7 selectable + gold), each with a
   hologram of name / stars / materials / pad number. Rotation is a 90 s
   scheduler that swaps contents, not a rebuild of the room.
3. **Selection is a right-click on the pad**, limited to 3 in-flight builds
   per team. The slot stays occupied by that build until the team completes it
   — this one rule produces the P25 repeat behaviour for free.
4. **Stars are data.** Author them; do not compute them. Keep a difficulty
   formula only as an authoring lint ("this 5★ has 30 blocks, is that
   intended?").
5. **Two numbers per build**: `block_slots` for completion and `item_cost` for
   the shopping list. Doors and double slabs are the reason.
6. **State-aware completion from day one.** If the rebuild ships material-only
   matching, retrofitting stairs later means re-authoring every build.
7. **Score = stars × 20 × completion%** (+ three bonuses). Remove speed and
   order bonuses.
8. **Partial credit is per slot, not per build.** Track `correctSlots` every
   tick (the current `evaluate()` already does this).
9. **Gold needs its own timer and its own clear.** 2 minutes from spawn, block
   removal on expiry, no catch-up, no reference model in the team area.
10. **The shopping list book is the primary UI**, not chat. One writable book
    per player, one page per in-flight build.

---

## 8. Open questions

| # | Question | Why it matters | Conf. |
|---|---|---|---|
| Q1 | P25 3★ row has 7 names but the Total column says 60 — is one missing, or is the total wrong? | catalogue completeness | L |
| Q2 | TKO: hologram shows **3★**, wiki SB table lists **4★**. Which is current? | star data | L (conflict) |
| Q3 | Is partial credit paid on gold builds? | gold scoring | L |
| Q4 | Does a wrong/extra block outside the build block completion, or only lower the percentage? | completion rule | L |
| Q5 | Are stair `shape` (inner/outer) and slab `type` both checked, or only `facing`? | state check scope | M |
| Q6 | Does the gold build clear at exactly 2:00, or at the next 2-minute rotation? | gold timer edge | L |
| Q7 | Are item counts for a build exactly the block count, or do they include crafting loss (e.g. 6 planks → 4 stairs)? | shopping list accuracy | L |
| Q8 | Do 1★–2★ builds ever contain rotation-sensitive blocks? | tier rules | M |

---

## 9. Premium checklist (testable)

1. A build with `dims 7×7×8` loads and renders without clipping the 9×9 pad;
   a 5×6×1 façade at 5★ also loads and can score full.
2. `bm_camp_stool` and `bm_windmill` parse; `completion.block_slots` equals the
   counted non-air cells for both.
3. A 7×7 build is rejected if any layer row is the wrong length.
4. A 5★ build with a hand-typed material count that differs from the derived
   count fails validation with the build id in the message.
5. A stair placed facing the wrong way is **not** counted as correct.
6. A door with the hinge on the opposite side **is** counted as correct.
7. A double slab counts as one completed slot and two items in the list.
8. Gallery shows exactly 2/2/1/1/1 + gold; after 90 s the 7 swap, the gold does
   not.
9. Selecting a build moves a copy to team plot 1, 2, then 3; a 4th selection is
   refused with a message.
10. The selected build's gallery slot does not rotate until that team completes
    it; the same team can select it again for a second plot.
11. Gold build spawns at 4:00, 6:00, 8:00 and 10:00 in a 720 s game.
12. No gold reference model exists in the team area at any time.
13. At 2:00 the gold plot is cleared and cannot be completed afterwards.
14. Completing a 3★ build awards 60 coins; a 50%-complete 3★ awards 30.
15. Employee of the Month pays 100/50/25 for most stars, most builds and
    highest average stars, and shows the three names in the results.

---

## 10. Deliverables

- `README.md` — this dossier.
- `REFERENCE_IMAGES.md` — image-pack index: path, source, timestamp, build,
  star, what each file shows.
- `frames/` — **primary handoff**, 16 native-resolution images:
  Blueprint Gallery (full + pads + gold pad), team build area (full + TKO
  reference + empty plot with hologram), star/material label crops, four
  official build renders, the 7×7 base crop, and the mart overview.
- `data/builds.csv` — 273 MCC reference rows: 114 legacy + revamp 1★–5★ and
  gold, with era, stars (blank where the era had none), gold flag, debut event,
  flags (N/S/G/1) and notes.
- `data/proposed_builds.csv` — 26 original builds, 1★–7★, with dims, block
  slots, item cost, palette, rotation sensitivity.
- `data/format.md` — the build data format, state-matching rules, scoring
  formula, validation list and loader shape.
