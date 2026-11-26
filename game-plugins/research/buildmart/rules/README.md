# Build Mart — rules, scoring and format (MCC-first)

**Agent:** rules/format researcher (Wave 1)
**Date:** 2026-11-24
**Design target:** current MCC Build Mart, i.e. the **Blueprint Update** (MCC P25) plus the **MCC FS** scoring flatten. Latest footage used: **MCC Fall Stars, 5 Sep 2026**.
**Scope:** rules and scoring only. Map geometry, island layouts and block coordinates belong to the map agent. No source files were edited.

---

## 1. Method

1. Read the official Noxcrew rules page, the MCC wiki game page and the Season 5/6 patch notes.
2. Cross-checked every current-value claim against a recent VOD (Philza, MCC Fall Stars) and, where possible, the official page.
3. Downloaded **two targeted windows only** (39:00–42:20 and 43:40–45:00; 4 min 40 s kept; the second window was re-cut at 1080p for legibility). No full VOD. Frames were extracted at full resolution.
4. Marked confidence **H/M/L** per claim. Where the wiki and footage disagree, footage wins for the current build; where neither shows it, it is under Open questions.

The official page blocks direct fetching (403) and was read through a text proxy; the content is the live official page. The wiki is community-maintained but is the only source that records the full patch history.

---

## 2. Sources

| Tier | Source | Used for | Confidence |
|---|---|---|---|
| 1 | **Official rules page** — `mcchampionship.com/pages/build-mart/` (retrieved 2026-11-24) | Current scoring: stars, 20 coins/star, gold 7 stars, partial completion, EOTM categories and 100/50/25 | H |
| 1 | **MCC wiki — Big Sales at Build Mart** (`mcchampionship.wiki.gg`, last edited 2026-11-08) | Duration, gallery, plots, gold rules, scoring history, AS rotation rule | H (current), H (history) |
| 1 | **Games Updates (Season 5)** — MCC P25 Blueprint Update notes | 90 s reroll, selected slots not rerolling, gold timing, star value phases, chests | H |
| 1 | **Games Updates (Season 6)** — MCC FS notes | Star value flattened to 20; new block sets | H |
| 1 | **List of Build Mart Builds** (`mcchampionship.wiki.gg`) | Revamped build pool: 60 regular per event (16/16/8/8/8) + 4 gold; build names and stars | H |
| 1 | **VOD** — “MCC FALL STARS w/ @cubfan @ish & @SeaPeeKay — Philza VOD”, YouTube `cMcZ74IhcrI`, 2026-09-05 | Live HUD, plots, gallery podium, book, gold selection, completion, travel, kit | H (visual), M (interaction details) |
| 2 | Reddit r/MinecraftChampionship — MCC Hermit Takeover scores thread | Cross-check “usual 12 minutes; HT shortened to 8 and gold immediately” | M |
| 2 | Reddit — “One approach to floating in Build Mart” | Mention of the “selected … as a Blueprint” chat line and button spam | M |
| — | No fan recreations, third-party maps or plugin code used | — | — |

---

## 3. Findings

### 3.1 Format

**F1. Duration is 12:00 and teams are 4 players. (H)**
Wiki infobox says 12 minutes. The VOD timer starts at 12:00 and reads 11:39 twenty-one seconds in (`@40:58`). The Hermit Takeover exception (8 minutes, gold immediately) is explicitly described as non-standard. Team size 4 is visible in the roster and HUD.

**F2. The game starts with empty plots and no build preview. (H)**
At `@39:58` the sidebar shows `BUILD SLOTS: Empty Plot #1 / #2 / #3` and the room is empty; the centre shows `GAME STARTING IN: 00:20` under a “how to play” card. The first builds exist only after selection. This replaces the current plugin’s 10 s target preview.

**F3. Each team has three regular plots and one separate Gold plot. (H)**
Wiki, confirmed at `@40:26`: three side-by-side white plots with low separators, and a raised plot behind them under a yellow `GOLD BUILD` sign. The Gold plot is part of the room from the start but stays empty until a gold is selected.

**F4. The build room contains a portal, flight, five colour-coded chests, crafting tables, stonecutters and two fuelled smelting areas. (H)**
Wiki “Build Area”; P25 notes add the colour-coded chests. The portal is visible at `@42:06` (purple portal in a white/red frame at the end of a carpeted corridor). There are **no shop counters** in current MCC; the current plugin’s Glassworks/Mason/Kiln/Jeweller counters are an invention and should go.

### 3.2 Blueprint Gallery and selection

**F5. The gallery has seven normal slots plus a gold slot. (H for seven + gold; M for the exact 2/2/1/1/1 split)**
Wiki: 2×1★, 2×2★, 1×3★, 1×4★, 1×5★, plus the gold plot. The P25 notes describe seven displayed builds and a separate gold. The exact 2/2/1/1/1 split is wiki-only; no single frame shows all seven podiums, though it is stable across the revamped events.

**F6. The gallery rerolls every 90 seconds, independent of team completions. (H)**
Wiki and P25 notes; the VOD chat shows `[✓] Blueprints plots now have new builds!` at ~3:07 elapsed, matching the 1:30/3:00 cadence. It does **not** reroll per team completion.

**F7. A selected build’s slot does not reroll until that build is completed. (M)**
P25 notes: “Builds which have already been sent to team plots will not refresh until they are completed by a team.” This is the mechanism that lets a team repeat a build before the slot rotates. Not directly observable in the 6 minutes of footage.

**F8. A team works on at most three builds at once. (H)**
Wiki; the sidebar shows exactly three slot lines (`Whale ×3★`, `Game Set Match ×2★`, `Scatter ×2★` at `@40:58`). A fourth selection is refused.

**F9. Selection is a physical interaction at the blueprint podium. (H for the podium, M for the input)**
At `@44:30` each blueprint is a floating miniature over a gold pedestal with a red button; the floating label shows name, star rating and material counts (`Ruin ★★`, `Aqua Concrete 6×`, `Prismarine Bricks 12×`). Community reports describe a “You selected … as a Blueprint!” chat line when the button is pressed. The VOD does not show the press itself.

**F10. Gallery entries are not exclusive between teams. (H)**
At `@43:48` the chat shows `Yellow Yaks completed Whale` and `Green Geckos completed Whale` within seconds, and later `Blue Bats completed Whale`. Two or more teams can hold the same blueprint at the same time. The gallery is therefore a per-team view, not a shared claim board.

**F11. A completed build frees its slot; the team must select again. (H)**
At `@43:48` one build completes and the slot list is down to `Whale` and `Game Set Match`; by `@44:42` the third slot is `Ruin`. There is no auto-advance to the next build.

**F12. Repeat selection is allowed since P25. (M)**
Wiki trivia: “it is possible to repeat the build by selecting the same build before the build rotation happens.” One source, consistent with F7.

**F13. Selection delivers three things: a reference copy, a material list at the plot, and a page in the shopping-list book. (H)**
Wiki; P25 notes; the book at `@43:46` is titled with the build name and lists exact counts. The reference is described as a copy sent to the team area; the VOD never returns to base with an active reference, so its exact form is M.

### 3.3 Shopping list

**F14. The shopping list is a custom offhand GUI, not a vanilla book. (H)**
`@43:46`: a parchment-style GUI, header `Page 1 of 3`, build name at the top, `Materials:` with icon + name + quantity per line, and a `Done` button. The book sits in the offhand slot, visible in the inventory at `@41:48`.

**F15. One page per active build; pages show the exact material list with counts. (H)**
Whale page 1: Andesite 6×, Andesite Stairs 10×, Cobblestone 8×, Cobblestone Slab 2×, Cobblestone Stairs 2×. It is a checklist, not a shopping-cart UI: there is no purchase, cart, or currency.

**F16. The gallery label repeats the same information before selection. (H)**
Name, stars, and material counts are readable on the floating label at `@44:30`. This is how a team decides what to take without opening a menu.

### 3.4 Gold Builds

**F17. The first Gold Build appears at 4:00; a new one appears every 2:00 after that. (H)**
Wiki and P25 notes; the VOD top bar counts down `GOLDEN BUILD IN: 03:42` early in the game and reaches 0 at the four-minute mark, then chat says `[✓] A new golden build is now available!`.

**F18. The gold is manually selected from its own booth. (H)**
At `@44:42` the centre title reads `Golden Build selected!` two seconds after 4:00, and the top bar immediately resets to `01:58` for the next gold. The gold booth is separate, under a `GOLD BUILD` sign (`@44:34`). A team can therefore skip a gold.

**F19. Gold is 7 stars, has no reference in the team area, and must be copied from the gallery. (H)**
Wiki and P25 notes. The Gold plot is signed but has no reference model; the only copy is the miniature at the gallery. This is the intended memory/communication challenge.

**F20. A gold is available for two minutes; failure removes the blocks placed in the Gold plot; past golds cannot be completed retroactively. (H for the window, M for clear-on-failure and no-retro)**
Wiki. The two-minute window is consistent with the VOD countdown; the clear-on-failure and no-retroactive rules are wiki-only and were not captured on video (the clip ends before the first gold expires).

**F21. Gold stars count as stars for scoring. (H for value, M for EOTM inclusion)**
Official: “Golden Builds are worth seven!” at 20 coins/star, so 140. Whether those 7 stars also feed Entrepreneur and Quality Assurance is not stated; assume yes but flag it.

### 3.5 Completion and build checking

**F22. A build is complete only when it matches exactly, including block rotation for directional blocks. (H for the rule, M for current enforcement)**
MCC AS change: “The exact rotation of all blocks is now required … (e.g. stairs, open doors, slabs). Texture rotation is not needed … Double slabs are now counted as a full block.” The rule is not listed as reverted; current builds visibly use stairs and slabs. The current plugin ignores orientation and should stop.

**F23. Extra blocks invalidate the build. (H)**
Wiki “copy the builds exactly”; the VOD’s completion triggers only on a matching plot. The current plugin already counts wrong blocks; keep that, and reject all extras inside the plot volume.

**F24. Completion pays immediately and is broadcast to every team. (H)**
`@43:48`: in-world `BUILD COMPLETED!` above the plot; chat `[+1.00] [✓] Your team completed Coin (★★)`; other teams’ completions appear in the same chat. Coins are awarded at completion, not at the end.

### 3.6 Scoring

**F25. Current scoring is flat 20 coins per star. (H)**
Official page: “Stars award 20 coins throughout the game.” Wiki FS history: the P25 time-based multiplier (10 → 15 → 20 per star) was removed in Fall Stars and flattened at 20. Therefore:

| Build | Stars | Coins |
|---|---:|---:|
| 1★ | 1 | 20 |
| 2★ | 2 | 40 |
| 3★ | 3 | 60 |
| 4★ | 4 | 80 |
| 5★ | 5 | 100 |
| Gold | 7 | 140 |

No speed bonus, no first/second/third bonus, no most-builds end bonus.

**F26. Unfinished builds pay a percentage of their star value at the end of the game. (H)**
Official: “Extra coins are awarded at the end of the game based on the percentage completed of each remaining build. This calculation is based on the amount of stars the build would reward if completed.” So partial pay is end-of-game only, not per block. A build that is 100% at the whistle can pay both partial and full (the MCC BG double-award bug), so the implementation should snapshot completion once.

**F27. Partial completion of the final Golden Build is no longer awarded. (M)**
MCC BG change note. Interpret as: exclude gold from the end-of-game partial payout (or at least the last gold). Wiki-only, one line.

**F28. Three Employee of the Month bonuses pay 100/50/25. (H)**
Official and wiki:
- **Entrepreneur** — most total stars.
- **Line Cook** — most builds replicated.
- **Quality Assurance** — highest average star count for completed builds.
Top three teams in each category get 100/50/25. Ties and the zero-build case are unspecified.

**F29. Historical scoring, for context only. (H)**
- MCC 3 and MCC 14: 160 coins for first to complete a build, −8 per team afterwards.
- MCC 7: 120 −8.
- MCC 17: placement table 140/132/124/116/104/92/80/64/48/32.
- MCC 24 (Sands of Mart remix): 160/150/138/126/112/98/82/64/48/30.
- MCC P25: 10/15/20 coins per star by time band.
- MCC FS: flat 20 per star (current).
Do **not** implement the old placement tables; they are included so old VODs are not mistaken for current rules.

### 3.7 Movement, kit and the mart

**F30. The kit is diamond tools, elytra, Haste II and the offhand book. (H for the set, M for exact enchants)**
Inventory at `@41:48`: elytra in the chest slot, axe/pickaxe/shovel/shears in the hotbar, shopping book in the offhand, `Haste II` shown. The action bar names a `Diamond Pickaxe`. Enchantments are not legible in the frame; current MCC tools are heavily enchanted (Efficiency V and unbreakable-class behaviour). The current plugin’s iron tools should be upgraded.

**F31. Flight is allowed in the build room; the mart is crossed by elytra, a central fan, launch pads and speed boosts. (H for elytra, M for fan/pads)**
Wiki explicitly separates the two: “Players may also fly in the Build Area itself,” while the mart uses elytra + fan + pads. VOD confirms elytra flight in the mart; the fan and launch pads are from the wiki and the map spec, not clearly isolated in these frames.

**F32. The mart is a ring of resource islands: upper islands for ores/logs/stones, lower islands for colour families. (H)**
Wiki; VOD `@44:28` shows huge `RED / ORANGE / PINK / WHITE / LIME` labels on the lower ring; `@41:12` shows a multi-level log island. Event variants swap copper/coral, snow/sand, garden, etc. This replaces the current 27-stall ring.

**F33. Falling off the islands loses all carried materials and respawns the player. (H)**
Wiki. The current plugin already does this; keep it, but exclude the kit and the book from the wipe.

**F34. The targeted block’s name is shown in green above the hotbar. (H)**
Visible at `@44:28` (`Blue Concrete Powder`), `@41:12` (`Birch Log`), etc. This is the colourblind/identification aid introduced in MCC 7 and still present. Keep it.

**F35. Blocks on the islands regenerate. (M)**
The wiki does not state a timer, but four teams share one mart and community discussion assumes respawn; without it, teams could be locked out. Current plugin uses 3 s. Keep 3–5 s and mark as an adaptation until verified.

### 3.8 HUD, flow and end state

**F36. The HUD shows: 12:00 timer, gold countdown, team stars + builds, team coins, three build slots, block name. (H)**
`@40:58` and `@44:42`: top-centre timer and gold bar; top-right `0 ★ / 0 [pickaxe]`; sidebar `GAME COINS (x1.0)` with all ten teams, then `BUILD SLOTS` with name + `×N ★`; green action-bar block name. The current sidebar (builds completed only) is not the MCC layout.

**F37. The game ends at 0:00 with partial-completion coins and EOTM bonuses. (H)**
Official page and wiki. No sudden-death, no final “most builds” bonus. Ties in the final standings or in an EOTM category are not documented **(L)**; the current plugin shares ranks, which is a reasonable default but must be stated.

**F38. There is no pre-game build preview, no Big Sales, no shop currency, no fixed build order and no per-placement coin bonus in current MCC. (H)**
All five are absent from the official page, the P25/FS notes and the VOD. They are artifacts of the current plugin or of pre-P25 MCC.

### 3.9 What changed in P25, FS and AS

| Patch | Change | Still current? |
|---|---|---|
| **MCC P25** (Blueprint Update) | Gallery of 7 selectable builds; up to 3 at once; 90 s reroll; selected slots pinned; stars + materials shown; shopping-list book; colour-coded chests; gold moved to a timed, manually selected gallery booth with a dedicated plot and no team-area reference; star pay 10/15/20 by phase; EOTM bonuses | Yes, except the star pay |
| **MCC FS** | Star pay flattened to 20/star; new poplar wood and concrete stairs/slabs added | Yes |
| **MCC AS** | Exact block rotation required (stairs/doors/slabs); texture rotation ignored; double slabs count as full blocks | Yes, not reverted |

---

## 4. Frame index

- `frames.md` — table of every saved frame with VOD timestamp and what it proves.
- `REFERENCE_IMAGES.md` — the primary visual handoff: 13 full-resolution images with “what a builder should take from this”.
- `frames/` — the images themselves.
- `videos/` — the two targeted clips only (39:00–42:20 and 43:10–45:40).

---

## 5. Design implications for the rebuild

1. **Replace the sequence with a per-side gallery + slot model.** Each side owns `gallery[7]`, `gold`, `slots[3]`, `stars`, `builds`, `coins`. The gallery is not a global shared list; two sides can hold the same blueprint (F10).
2. **Make selection physical.** A blueprint podium with a floating preview, name, stars, materials and a button. Pressing it fills the first free slot; with three filled, deny with a clear message. This is the single most important interaction to get right.
3. **Pin selected slots.** A slot currently on a plot must survive the 90 s reroll (F7), otherwise a team can lose a build mid-way. Reroll only unselected slots.
4. **Replace the 10 s preview with a how-to-play card and countdown.** All three plots must start empty (F2). The first gallery is already populated at 0:00.
5. **Build the book as a custom 3-page GUI in the offhand**, one page per slot, exact material counts, a Done button, and a page that empties when the slot frees (F14/F15).
6. **Gold is a state machine:** `waiting(240s) → available → selected → deadline(+120s) → clear`. New gold every 120 s regardless of completion; no retroactive completion; no reference at the Gold plot; clear placed blocks on timeout (F17–F20).
7. **Score at completion** with `20 × stars`; **score partial at 0:00** as `percent × 20 × stars`; exclude gold from partial; then award EOTM 100/50/25 (F25–F28).
8. **Check orientation.** Compare `BlockData` for stairs, slabs, doors, trapdoors, logs and similar; ignore texture-only differences; treat double slabs as full blocks (F22).
9. **Rework the HUD** to timer + gold bar, stars/builds counters, coins sidebar, three slot lines, block name. Remove “builds completed” as the only sidebar metric (F36).
10. **Replace the mart economy.** Delete Big Sales and the four shop counters. Provide resource islands, crafting tables, stonecutters, two fuelled smelters, five labelled chests, elytra, fan and pads (F4/F31/F32).
11. **Keep the fall penalty** but protect the kit/book; keep the action-bar block name; keep 3–5 s regen (F33–F35).
12. **Expand the build pool and tag stars.** MCC runs 60 regular builds per event at 16/16/8/8/8 plus 4 gold. The plugin’s 27 presets and 5×5×5 cap are far below the real scale; the map/preset agents need to size plots from the map spec (open question).

---

## 6. Open questions

1. **Exact selection input.** Is it a button press, a right-click on the pedestal, or both? The red button on the gold pedestal is visible; the press is not. Resolve with a 20 s clip of a player selecting.
2. **Gold timer start.** Does the 2:00 run from global availability (4:00, 6:00…) or from the team’s selection? The top bar resets on selection, which suggests the former, but a late selection would then have less than 2:00.
3. **Gold stars in EOTM.** Do the 7 gold stars count toward Entrepreneur and Quality Assurance? Assume yes; verify from a results screen.
4. **Tie-breaks.** No documented rule for EOTM ties or final coin ties. Current code shares ranks; decide before shipping.
5. **Partial-completion scope.** Does it include the Gold plot, only the three regular plots, or every selected build? Official wording says “each remaining build”; BG excludes the final gold.
6. **Exact plot and build footprint.** Current 5×5×5 is too small. Measure a plot and a 5★ build from the map agent’s schematic rather than guessing.
7. **Regen timing.** 3 s is the current plugin value; MCC’s exact respawn delay was not measured.
8. **Chest semantics.** “Five colour-coded chests” — are they one per plot plus one shared, or five per team colour? The VOD shows a normal chest GUI, not the colours.
9. **Can players place blocks outside their plot?** Current plugin forbids it. MCC appears to restrict building to the plot; not explicitly documented.
10. **Tool enchantments.** The pickaxe is diamond; the full enchant list (Silk Touch? Fortune? Mending?) is not legible. Do not copy “Silk Touch” from a blurry tooltip without confirmation.

---

## 7. Implementation checklist (testable)

- [ ] Timer starts at 12:00; game ends at 0:00; no early end when all plots are empty.
- [ ] Every side starts with exactly 3 empty regular plots + 1 empty Gold plot, no reference, no target.
- [ ] Gallery has 7 normal podiums with the 2/2/1/1/1 star split + 1 gold booth; all labels show name, stars and material counts.
- [ ] Gallery rerolls every 90 s; a slot with an active build does not reroll; a chat line announces the reroll.
- [ ] A team can hold at most 3 regular builds; a 4th selection is refused with a clear message.
- [ ] Selecting a blueprint creates a reference at the plot, a material list, and the correct book page.
- [ ] The book is in the offhand, opens as a 3-page GUI, shows exact counts, and empties the page when its build completes.
- [ ] First gold becomes available at 4:00 and every 2:00 after; it is manually selected at the gold booth; no reference appears in the team room.
- [ ] A gold not completed within its window has its placed blocks removed; an expired gold cannot be completed later.
- [ ] Completion requires every required block in the right position and orientation; extra blocks invalidate; double slabs count as full; a completed build pays 20 × stars immediately, broadcasts, and frees its slot without auto-advancing.
- [ ] At 0:00, each unfinished regular build pays floor(percent × 20 × stars); gold partial is excluded.
- [ ] EOTM pays 100/50/25 for most stars, most builds, highest average stars; the tie rule is documented.
- [ ] The HUD shows timer, gold countdown, team stars/builds, coins, and three named slots with stars.
- [ ] The room has a portal, flight, five labelled chests, crafting tables, stonecutters and two fuelled smelters; no shop counters and no Big Sales.
- [ ] Falling off the mart wipes carried materials but not the kit/book and respawns the player; the action bar shows the targeted block’s name; island blocks regenerate in 3–5 s.

---

## 8. Builder rules (implementation contract)

1. **[OFFICIAL]** 12:00 game, 4-player teams; no early finish.
2. **[OFFICIAL]** Start with empty inventory + kit (tools, elytra, book, Haste II), three empty plots and no build preview.
3. **[OFFICIAL]** Three regular plots + one separate Gold plot per team.
4. **[OFFICIAL]** Gallery: 7 normal slots — 2×1★, 2×2★, 1×3★, 1×4★, 1×5★ — plus 1 Gold slot.
5. **[OFFICIAL]** Gallery rerolls every 90 s, independent of completions; announce each reroll.
6. **[OFFICIAL]** Press the blueprint’s button to send it to a free plot; maximum three active regular builds.
7. **[OFFICIAL]** A selection creates a reference at the plot, a material list, and a page in the offhand book.
8. **[OFFICIAL]** Completing a build frees its slot; the team must select again — no auto-advance.
9. **[OFFICIAL]** The same blueprint may be selected again before its slot rerolls (P25+).
10. **[OFFICIAL]** Gold: first at 4:00, then every 2:00, 7★, manually selected, separate plot, no team-area reference, 2:00 to finish.
11. **[OFFICIAL]** A failed or expired gold has its placed blocks removed; no retroactive completion.
12. **[OFFICIAL]** 20 coins per star, flat; gold = 140; no speed, order or most-builds bonus.
13. **[OFFICIAL]** At game end, unfinished regular builds pay their completion percentage × star value.
14. **[OFFICIAL]** Employee of the Month: most stars / most builds / highest average stars, each paying 100/50/25.
15. **[OFFICIAL]** Exact rotation is required for stairs, slabs and doors; texture rotation is ignored; double slabs count as full blocks.
16. **[OFFICIAL]** Every required block must match and no extra block may remain in the plot volume.
17. **[OFFICIAL]** Book: offhand custom GUI, one page per active build, exact material counts, Done button.
18. **[OFFICIAL]** Room: portal, flight, five colour-coded chests, crafting tables, stonecutters, two fuelled smelting areas.
19. **[OFFICIAL]** Mart: elytra + central fan + launch pads; falling loses carried materials and respawns the player.
20. **[OFFICIAL]** HUD: 12:00 timer, gold countdown, team stars/builds, team coins, three named build slots, block name.
21. **[ADAPT]** Give every side its own gallery state; the gallery is not a shared, exclusive claim board.
22. **[ADAPT]** Regenerate island blocks after 3–5 s so four sides can gather from one mart.
23. **[ADAPT]** Exclude the final gold (preferably all gold) from the end-of-game partial payout, and snapshot completion once to avoid the 100%-partial + full double-pay.
24. **[ADAPT]** Remove Big Sales, the four shop counters, the 10 s preview and the most-builds final bonus; they are not MCC.
25. **[UNKNOWN]** Gold timer start (availability vs selection), gold stars in EOTM, EOTM/final tie-breaks, and exact plot/build footprint — decide and document before coding.
