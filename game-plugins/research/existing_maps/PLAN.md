# Existing Maps — Deep Research Plan

Status: **plan only, no agents dispatched yet**
Created: 2026-11-24
Owner: coordinator

## 0. What is already done

All ten unique downloads (nine archives; `zipped-map.zip` is byte-identical to `YetiSetGo.zip`) are extracted, copied into the 25580 review server, and loaded:

| Server world | Source archive | Version | Command blocks | Notes |
|---|---|---|---|---|
| `mcc_clouds2` | Clouds 2 - Electric Boogaloo | 1.20.1 | 322 | 285 signs, 225 shulkers, 21.2 MB resource pack |
| `mcc_citymarathon` | city-marathon.zip | 1.19.2 | 67 | 140 shulkers, elytra auto-equip |
| `mcc_spacerace` | Space Race.zip | 1.18.1 | 29 | 22 signs, elytra zones |
| `mcc_yetisetgo` | YetiSetGo.zip = zipped-map.zip | 1.19.2 | 37 | 14 signs, 3 shulkers, 0.1 MB pack |
| `mcc_decisiondome` | Decision Dome.zip | 1.17.1 | 0 | pure build, 1,344 chunks |
| `mcc_minigames` | daniellaur's minigames version 3 | 1.17.1 | 0 | 18,628 chunks, multiple arenas |
| `mcc_tgttos_badlands` | tgttos-badlands-by-mickeykat.zip | 1.19.2 | 0 | `tgttos` datapack |
| `mcc_tgttos_industry` | tgttos-industry-by-mickey-kat.zip | 1.19.1 | 0 | `Industry` datapack |
| `mcc_tgttos_pits` | tgttos-pits-by-mickey-kat.zip | 1.19.1 | 0 | `Pits` datapack |

Server changes made: command blocks enabled, RCON enabled on `25576`, all worlds imported with Multiverse. No plugin code, no game build, and no existing world was deleted.

## 1. Rules for every agent

1. **Research only.** No game code, no plugin code, no build/paste, no registry edits, no server restarts.
2. **One owner per folder.** An agent may write only inside its assigned `game-plugins/research/existing_maps/<folder>/`. It may read anything.
3. **Shared tools are read-only.** `tools/world_to_mcmdump.py`, `tools/render_preview.py` and existing research dossiers must not be edited. If a tool is wrong, report it, do not fork it.
4. **The live server is read-only.** Only the server-import agent may issue read-only RCON queries. No `mv import`, `mv unload`, `stop`, `reload`, or world edits.
5. **Evidence, not prose.** Every claim gets `H` (directly observed file/command/frame), `M` (inference from two sources) or `L` (guess). Every map report includes actual images and a command/block export.
6. **Image-first.** Open the actual PNG/JPG frames and renders. Do not describe an image from memory or from another agent's text.
7. **No duplicate work.** If an agent needs another agent's area, it links to that report instead of re-analysing it.
8. **Stop at findings.** The output is a dossier and asset pack. No implementation plan is executed.

## 2. Agent roster, effort and justification

The subagent API exposes agent type and model, not a numeric reasoning dial. Effort below is therefore operational: number of passes, depth of evidence, and expected runtime. All forensic agents use `general` (full tool access); `explore` is not used for NBT/asset work because it is a read-only search agent and would under-deliver.

| ID | Agent | Effort | Why this effort | Owns |
|---|---|---|---|---|
| A1 | Clouds 2 forensic | **E3 deep** | The reference map; 322 command blocks, 225 shulkers, 21 MB pack, plus the video. It is the standard the others are judged against. | `clouds2/` |
| A2 | City Marathon forensic | **E2 standard** | Smaller command set (67), but a distinct elytra/city route; needs route + pads + blocks, not pack forensics. | `city_marathon/` |
| A3 | Space Race forensic | **E2 standard** | 29 command blocks, older 1.18.1 mechanics; route and elytra zones matter more than asset volume. | `space_race/` |
| A4 | Yeti Set Go forensic | **E2 standard** | 37 command blocks, small pack; compact map, quick to fully document. | `yeti_set_go/` |
| A5 | Decision Dome recreation | **E3 deep** | Pure build, no commands; the value is dimensional/palette accuracy and a direct comparison with the Nocturne Orrery. Wrong measurements would poison later integration. | `decision_dome_recreation/` |
| A6 | Minigames cartographer | **E3 deep** | One 18,628-chunk world containing several arenas. Needs a full survey before sub-arenas can be split safely. | `minigames/` |
| A6a | Sky Battle arena deep dive | **E2 standard** | Spawned only after A6 gives exact bounds; one arena, focused geometry/palette work. | `minigames/sky_battle/` |
| A6b | Parkour Tag arena deep dive | **E2 standard** | Same, after A6 bounds; route/parkour readability is the focus. | `minigames/parkour_tag/` |
| A6c | Other minigames inventory | **E1 quick** | Only after A6; catalogue remaining arenas, no deep dive unless one is requested. | `minigames/other/` |
| A7 | TGTTOS Badlands | **E2 standard** | Datapack-driven; no command blocks. Needs function/route/start analysis, not pack work. | `tgttos_badlands/` |
| A8 | TGTTOS Industry | **E2 standard** | Same pipeline as A7, different map; kept separate to avoid one agent juggling three routes. | `tgttos_industry/` |
| A9 | TGTTOS Pits | **E2 standard** | Same. | `tgttos_pits/` |
| A10 | Resource-pack forensics | **E3 deep** | Shared asset pack across maps; full texture/sound/model/lang inventory and custom-item mapping. This is the only agent allowed to own `resource_packs/`. | `resource_packs/` |
| A11 | Video/gameplay analyst | **E2 standard** | One video, but it is the ground truth for feel, HUD, pacing and route legibility; needs frame extraction and timestamped evidence. | `video_clouds2/` |
| A12 | Server-import verification | **E2 standard** | Cross-world compatibility and 1.21 breakage. Must not restart or edit; read-only RCON queries only. | `server_import/` |
| A13 | Cross-map synthesis | **E3 deep** | Reads all reports; compares the four Ace Race maps, extracts what works/doesn't, and produces one template recommendation. Cannot start until A1–A4, A7–A12 are done. | `comparison/` |
| A14 | Red-team QA | **E2 standard** | Audits every dossier for missing evidence, contradictions, unsupported claims and folder overlap. Runs after A13. | `_qa/` |

**Total: 16 agents.** Maximum concurrent: 11 (Wave 1). This is deliberately not 30+:
- one owner per world prevents conflicting interpretations of the same region files;
- splitting one map by “route / blocks / commands” would make three agents re-read the same chunks and produce overlapping reports;
- the only safe extra parallelism is the multi-arena world, split after its cartographer establishes bounds.

## 3. Waves and dependencies

### Wave 0 — done
Import, server verification of load, command blocks enabled.

### Wave 1 — launch together (11 agents)
A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12.

These have no write overlap. A1 and A10 both read the Clouds 2 pack, but A1 owns map-to-pack mapping and A10 owns the pack itself; A1 must link to A10's asset inventory rather than copy it.

### Wave 2 — after A6 publishes bounds (3 agents)
A6a Sky Battle, A6b Parkour Tag, A6c other arenas.

### Wave 3 — after Wave 1 + Wave 2 (2 agents)
A13 synthesis, then A14 QA.

No agent is left idle waiting inside Wave 1; each has a complete standalone brief.

## 4. Required output per map agent

Each folder must contain:

```
README.md                 full dossier, findings numbered with H/M/L
REFERENCE_IMAGES.md       image path -> source/timestamp -> what it shows -> what to take
frames/                   actual full-resolution PNG/JPG evidence
data/blocks.csv           every block type, count, share, use
data/command_blocks.csv   x,y,z,type,auto,condition,exact command,trigger,effect,1.21 risk
data/signs.csv            x,y,z,front/back text,colour,glow,command click event
data/checkpoints.csv      order,x,y,z,gate axis,width,what it grants/clears
data/route.md             leg-by-leg route with coordinates and sightline notes
data/palette.md           dominant palette, accents, gradients, lighting
data/entities.csv         armor stands, item frames, displays, shulkers, etc.
renders/                  top-down + 2 iso angles, labelled
STATUS.md                 done / blocked / open questions
```

Minimum acceptance:
- every command block accounted for, including repeating/chain/conditional state;
- every shulker/chest container opened and inventoried;
- route can be followed from the report alone;
- at least 6 full-resolution images per map (overview, start, signature section, checkpoint/pad, finish, detail);
- explicit “what this map does better/worse than the others” section;
- no claim without H/M/L.

## 5. Map-specific assignments

### A1 — Clouds 2 (E3)
- Decode all 322 command blocks: pads, mega jumps, elytra grants/clears, speed, water boost, kill planes, checkpoints, start/finish, world spawn.
- Rebuild the route leg-by-leg with coordinates and sightlines; explain why the player always knows where to go (walls, arches, colour, signage, camera framing).
- Inventory 225 shulkers and 285 signs; identify start kits, team kits, hidden skips.
- Map every custom texture/sound to the exact block/item that uses it; cross-link A10.
- Watch X4l2WSQv0Wk and mark which mechanics appear at which timestamps.
- Produce top/iso renders and a labelled route overlay.

### A2 — City Marathon (E2)
- 67 command blocks, 140 shulkers, auto-elytra chestplate zones.
- Route, checkpoint gates, pads, skips, failure planes.
- Block palette and city composition; how the track is bounded.
- 1.19.2 -> 1.21 command compatibility risks.

### A3 — Space Race (E2)
- 29 command blocks; decode elytra grant/clear, speed pads, dolphins grace, start countdown.
- Route, trident/elytra sections, ring placement, finish.
- 1.18.1 world upgrade notes.

### A4 — Yeti Set Go (E2)
- 37 command blocks, 3 shulkers, 14 signs.
- Route, pads, elytra/trident swaps, checkpoint logic, resource pack mapping.
- Compare compact 1.19.2 design with Clouds 2.

### A5 — Decision Dome recreation (E3)
- Exact dimensions, radii, heights, pod count, dial geometry, seat count, entrances, sightlines.
- Full material palette and block counts; lighting; banner/glass/copper usage.
- No command blocks: document how the build expects to be driven.
- Direct comparison table against `DecisionDome/SCHEMATIC.md` and the Nocturne Orrery report.
- No building and no code.

### A6 — Minigames cartographer (E3)
- Survey all 18,628 chunks; locate every distinct arena.
- For each: name, bounds, spawn, palette, intended game, completeness.
- Priority: Sky Battle arena and Parkour Tag arena; exact bounds and screenshots.
- Produce an arena index CSV and a labelled top-down world map.
- Publish bounds early in `STATUS.md` so Wave 2 can start.

### A6a/A6b/A6c — after A6
- A6a: Sky Battle — islands, chests, loot, spawn cages, border, void, mid.
- A6b: Parkour Tag — hunter/runner spawns, route loops, climbables, hiding spots, timer zones.
- A6c: catalogue any remaining arenas only.

### A7/A8/A9 — TGTTOS maps (E2 each)
- Unpack each datapack: load/tick functions, scoreboards, tags, respawn, finish detection, block reset.
- Route, start sign, checkpoints, pitfalls, block palette.
- 1.19 datapack pack-format upgrade risks under 1.21.
- Compare each with the current `McmTgttos` implementation, read-only.

### A10 — Resource packs (E3)
- Inventory every pack: `pack.mcmeta`, textures, `.mcmeta` animations, models, sounds, `sounds.json`, `en_us.json`.
- Identify every custom block/item mapping and which map uses it.
- Copy packs and extracted assets into `resource_packs/` (read-only source).
- Flag missing references (Clouds 2 `sounds.json` names `acerace.start` and `acerace.checkpoint` but those OGGs are absent).
- Document how to reproduce each effect without shipping the original pack, if licensing requires it.

### A11 — Video (E2)
- Download only X4l2WSQv0Wk; extract frames at start, first pad, elytra section, checkpoint, fail, finish.
- Timestamp HUD, lap/checkpoint text, camera, route readability.
- Compare video geometry with the downloaded world; note differences.
- Output a frame index and 10–20 full-res stills.

### A12 — Server import verification (E2)
- Confirm all worlds load, spawns are safe, command blocks run, datapacks load.
- Check every command string against 1.21 syntax; list exact failures.
- Check resource-pack compatibility; do not enable a global pack.
- Produce start instructions per world and a “known broken” list.
- Read-only RCON only; no restarts.

### A13 — Synthesis (E3)
- Read all dossiers; build a comparison matrix for the Ace Race maps: route clarity, boundedness, checkpoint count, mechanics, pacing, palette, pack, bugs.
- Rank what to adopt, what to reject, and why.
- Produce one recommended “MCC-faithful” template spec, still no code.
- Resolve contradictions; link evidence.

### A14 — QA (E2)
- Audit every folder against the acceptance checklist.
- Find missing images, unsupported claims, duplicate work, wrong coordinates, unopened containers.
- Produce `_qa/GAPS.md` and a per-agent fix list.
- No edits to other agents' folders; report only.

## 6. Time and risk

- Wave 1: 60–120 minutes (Clouds 2 and minigames are the long poles).
- Wave 2: 45–90 minutes.
- Wave 3: 30–60 minutes.
- Total: roughly 2.5–4 hours of agent time, mostly parallel.

Risks and controls:
- **NBT parse cost:** full Clouds 2 scan is large. Agents must use bounded crops/region subsets and `--scale 2–4` for overviews; only A1 may dump the full map.
- **1.21 upgrades:** old command syntax may fail. A12 owns the compatibility matrix; map agents record the original version but do not try to fix commands.
- **Conflicting packs:** A10 only inventories; no global resource pack is enabled.
- **Duplicate Yeti:** `zipped-map.zip` is a byte-identical copy. A4 analyses it once; no second agent.
- **Write collisions:** each agent has a unique folder; shared files are read-only. Coordinator merges only after QA.
- **Scope creep:** no agent is allowed to start building or writing plugin code, even if it finds an obvious fix.

## 7. Approval gate

Wave 1 does not start until the owner approves this roster and effort allocation. If approved, the coordinator launches A1–A12 together, then A6a–A6c, then A13–A14.
