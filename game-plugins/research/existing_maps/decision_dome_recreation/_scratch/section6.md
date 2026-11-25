## 6. Direct comparison with `DecisionDome/SCHEMATIC.md` and the Nocturne Orrery report

The comparison is against **our planned/built Nocturne Orrery** (SCHEMATIC.md + `previews/nocturne_orrery/REPORT.md` + the four previews I opened), not against the official MCC stills. The reference stills are context only. **Evidence rule for this table:** every downloaded-recreation value is **H** (block/NBT scan) or H-raycast; every Orrery value is **H** from SCHEMATIC/REPORT (itself a built-and-validated report, not re-measured by me); the verdict/advice is **M**. Where a cell is a design judgement, it says so.

| Criterion | Downloaded recreation (this dossier) | Nocturne Orrery (SCHEMATIC/REPORT) | Verdict |
|---|---|---|---|
| Scale / footprint | 53-wide drum, 85 × 66 envelope, 57 tall, **20,054 blocks** (H) | 97-wide drum (r48.5), footprint ≤100, interior 64–102, **451,515-block venue** (H report) | Reference is a ~1:2 model; ours is a venue. Reference is 4.4% of our block count. |
| Dome / exterior | **No dome, no roof**; open drum + crown (H) | Glass dome apex y146, 12 meridian ribs, marquee 57 × 22, facade clock, armillary topper y150–170 (H report) | **Ours** for the "dome" identity; reference is a drum/cake. |
| Pod count / type | 10, solid 5×5/7×7 towers, no door, no room, 0 seats (H) | 10 rooms, 5×5 interior min, 4 seats each, gate + window + team glass (H report) | **Ours** for play; reference for instant colour read. |
| Seats | **0** (H) | **40** (10 × 4) (H report) | Ours. |
| Stands / spectators | **0 tiers**; 4-wide standing ring (H) | 120 spots in 12 rows × 3 tiers, r24–35 (H report) | Ours. |
| Dial | y58, 8 × 45° zones, r9.0/9.5, beige quartz on gray, 4 continuous + 4 dashed dividers, 10 below terrace (H) | y64, r12, 8 × 45° (6 × 60°), black/cream chevrons, 3-high comb dividers, podium r7.5, 12 below terrace (H report) | **Ours** for readability; reference for vertical drama. |
| Wedge orientation | boundaries 0/45/90…; **E/W and N/S are dividers**; no wedge faces an entrance (H) | boundaries 22.5 + k45; **wedge 1 centred on 90°**, per SCHEMATIC (H report) | **Ours** matches the schematic rule; reference violates it. |
| Dividers | **none** (flat floor lines only) (H) | 8 + 6 sets × 51 cells, 3 high when raised (H report) | Ours. |
| Reveal / hero | Gold crown/truss y69–82, 17 × 17, 8 red jewels, white cross, yellow ring (H) | Copper armillary 7 × 7 × 7 y73–79, 4 reveal surfaces r8.5, gold planet (H report) | Both have a single hero. Reference is a literal crown; ours is thematic. Mixed. |
| Concourse / stair | 4-wide ring; **no stair into the pit**; 1-block tunnel/annex steps (H) | Concourse r19.6–23.4; 7-wide grand stair y64 r26 → y76 r39, 12 rises (H report) | **Ours**. |
| Entrances | W/E gatehouses + N red carpet + NW annex + SE door (H) | 13-wide portal, 9-wide doors, carpet, 2 aisles, elevator/lore door (H report) | Reference is more distributed; ours has one clear axis. Mixed. |
| Sightlines | 0–18/25 dial points from terrace, mean 9.8; 5/24 positions see none; **0/25 from N carpet**; 9/25 from y100 (H raycast) | 40/40 sightline checks pass; 8/8 labels from every pod (H report) | **Ours**, decisively. |
| Palette | 74.4% concrete, 9.4% terracotta, 9.2% glass, 2.2% quartz, 0.7% gold, **0% copper** (H) | Basalt/deepslate/blackstone, copper ribs, calcite/quartz, team jewel glass; 60/25/15 hierarchy (H report) | Reference is bolder/cleaner; ours is richer/thematic. Mixed. |
| Lighting | 18 sea lanterns, fixed noon, unlit pit (H) | Warm/teal uplights, LEDs, planetarium, facade clock (H report) | Ours. |
| Block budget | 20,054 (H) | 451,515 (within 275k–485k) (H report) | Reference is more efficient; ours is more detailed. Both meet their goals. |
| Runtime / logic | 2 command blocks (egg + glow), no vote/gate/tally/reset (H) | API v2: dialCapacity, setDial, setWedge, setPodGate, setPodState, setDividers, revealWedge, clearRuntime, revealSurface (H report) | **Ours**, decisively. |
| 6-game variant | only 8 zones; no 6-game geometry (H) | 6 × 60° set + 6 divider set + `six_dial_plan.png` proof (H report) | Ours. |
| Lore / arrival | Red carpet + plate + 17 heads + 24 banners + apiary + 2 armor stands (H) | Marquee, countdown, planetarium, 4 reveal faces; no trophy/heads (H report) | Reference has a human/lore layer; ours has event branding. Mixed. |
| Safety | 42-deep pit, **no outer rail**, **no stair**; inner barrier only (H) | Rails, concourse, stair, 120 stands (H report) | **Ours**. |
| Iconic read from far | Dark drum + white rim + 10 team towers + gold crown → instant "trophy/cake" (H/M) | Dark basalt drum + huge glass dome + marquee → reads as a building/venue (H/M) | Reference wins the icon; ours wins the venue. |

### 6.1 What the reference does better (adopt the principle, not the artefact)

1. **Team-colour legibility.** (H) Ten full-height team concrete towers make the 10 teams readable from every side; team colour is 26% of the build. Our report deliberately confines team colour to a thin rim/inner strip and glass suites; from a distance ours is a dark bowl. **Adopt:** stronger team-colour verticals above the rim (banners/light columns on the pod backs or outer wall), not solid towers.
2. **One unambiguous hero.** (H) The gold crown with 8 red jewels is the only object above the rim; it reads in one glance. Ours has an armillary + planetarium + marquee + clock + screens; richer, but more diffuse. **Adopt:** keep one hero silhouette; if a crown/coin variant is wanted for MCC fidelity, make it the single topper.
3. **Compact budget.** (H) 20k blocks, 85 types; the whole model reads with 4 materials. Ours is 451k and 34 source classes. **Adopt:** detail discipline on low-importance surfaces (foundation, backing, service walls), not on the playable core.
4. **Ceremonial arrival.** (H) The north dais is a clear 3-step red-carpet approach with a one-block plate and a two-command-block egg/glow prop. Ours has a 13-wide portal/carpet but a more complex entry sequence. **Adopt:** a single ceremony axis and a kit/egg station wired to the plugin, not a pressure plate.
5. **Vertical drama.** (H) The 42-deep pit + 50-block team shafts is a stronger section than our bowl. **Adopt:** a **shallow** reveal pit (3–5 blocks) under the dial for drama; do not copy 42.
6. **Human/lore layer.** (H) 17 heads, 24 banners, 2 beehives, 2 armor stands give the annex a story. Ours has event branding but no trophy wall. **Adopt:** a small winners/team-banner wall in the entry lounge, if lore permits.

### 6.2 What the Nocturne Orrery does better (keep)

1. **Playable pods.** (H) 10 rooms × 4 seats = 40, gates/windows/team glass; the reference has 0 seats and no rooms.
2. **Spectator capacity.** (H) 120 stand spots in 12 rows; the reference has 0 tiers.
3. **Circulation.** (H) 7-wide grand stair, concourse, aisles; the reference has no stair into the pit.
4. **Readable dial.** (H) 8/6 wedges, black/cream chevrons, 3-high comb dividers, wedge 1 on 90°; the reference's beige/gray dashed dial is low-contrast and hidden.
5. **Sightlines.** (H) 40/40 checks; the reference is 0–18/25 from the terrace and 0/25 from the main carpet.
6. **Dome/exterior.** (H) Glass dome, marquee, clock, topper; the reference has no roof.
7. **Lighting.** (H) Warm/teal/LED/planetarium vs 18 sea lanterns and fixed noon.
8. **Runtime.** (H) API v2 vs 2 command blocks.
9. **6-game variant.** (H) 6 × 60° set; the reference is 8-only.
10. **Safety.** (H) Rails/stairs/stands vs a 42-deep unrailed pit.

### 6.3 Adopt later (design notes only)

- A **team-colour vertical band** on the outer pod backs / rim, 2–4 blocks wide, so the 10 teams read from the plaza without becoming solid towers. (M)
- A **single hero** above the dial: keep the armillary, or make a crown/coin the only topper; remove competing silhouettes (marquee/clock are on the facade, so they do not compete inside). (M)
- A **red-carpet arrival + kit station**: one ceremonial axis, a clear 3-step approach, and an egg/kit dispenser driven by the venue API rather than a plate. (M)
- A **trophy wall / team-banner lounge** near the entrance; 10 team banners + winner heads, if lore permits. (M)
- A **shallow reveal pit** (3–5 blocks) under the dial, with a rail and a stair; not 42. (M)
- **Bright rim band** (white/cream) against the dark drum, as the reference does; our cream cornice is close, but the reference's white rim is stronger at distance. (M)
- **8 zones with a wedge facing the entrance**; ours already does this; keep it. (H/M)

### 6.4 Do not adopt

- Solid towers instead of pod rooms; 0 seats, 0 stands, 0 stair. (H)
- A 42-deep unrailed pit with no outer barrier. (H)
- A dial hidden under the crown and behind the towers. (H)
- No dome/roof while calling it a dome. (H)
- Static, uniform, low-contrast wedges with dashed dividers and no team colour. (H)
- Two command blocks as the "game loop"; a one-player egg prop is not a vote system. (H)
- 18-light daylight look with an unlit pit. (H)
- The 1:2 scale + 36.5% dial/drum ratio: it would consume our concourse/pods/stands. (H/M)

---
