# Build Mart — reference image pack (primary handoff)

These are full-resolution frames from the only VOD used, kept as files so the builder can open them directly. The written dossier (`README.md`) explains the rules; these images are the visual evidence.

**Source VOD:** `MCC FALL STARS w/ @cubfan @ish & @SeaPeeKay - Philza VOD` — YouTube `cMcZ74IhcrI`, streamed 2026-09-05, event **MCC Fall Stars** (post-FS scoring flatten). Build Mart chapter starts at **39:07**.
**Local clips** (targeted windows, no full VOD):
- `videos/cMcZ74IhcrI_2340.mp4` — VOD 39:00–42:20 (game intro, build room, first gather run).
- `videos/hd_2620.mp4` — VOD 43:40–45:00 (shopping book, gallery, first Gold Build selection).

**Reading the frames:** Philza's facecam, Twitch chat, subscriber goal and stream alerts are overlays, not game UI. The game UI is the top-centre timer/gold bar, top-right star + build counters, right sidebar (coins + build slots), hotbar, and the green block-name text above the hotbar. Frames 01–02 and 05–13 are 1920×1080; 03 is a 1380×660 HUD crop; 04 is a 1620×990 crop of a 1920×1080 frame so the podium text is readable.

---

## 01 — Team build room: three plots + Gold plot
**File:** `frames/01_team_build_area_3plots_gold.png`
**Timestamp:** VOD 40:26 (pre-start, 14 s countdown)
**Shows:** The team room from the player's eye level: three regular plots in a row, a raised Gold Build plot behind them with a yellow `GOLD BUILD` sign, teammates, and the hotbar kit. The plots are empty white floors with dark borders.
**Takeaway:** The live room is 3 side-by-side regular plots + 1 Gold plot, not one plot + pedestal. The Gold plot is visibly separate and signed. No target build is displayed before the start.
**Confidence:** H.

## 02 — Pre-game: all three slots empty
**File:** `frames/02_pregame_empty_plots_hud.png`
**Timestamp:** VOD 39:58
**Shows:** Wide view of the three empty plots; sidebar reads `BUILD SLOTS: ↳ Empty Plot #1 / ↳ Empty Plot #2 / ↳ Empty Plot #3`; top-centre `GAME STARTING IN: 00:20`.
**Takeaway:** Teams do not start with builds. Selection is a gameplay action after the timer starts. The sidebar names the slots, not the players.
**Confidence:** H.

## 03 — Three selected builds in the sidebar
**File:** `frames/03_build_slots_three_selected.png`
**Timestamp:** VOD 40:58 (crop of the sidebar)
**Shows:** `BUILD SLOTS:` → `Whale (×3 ★)`, `Game Set Match (×2 ★)`, `Scatter (×2 ★)`.
**Takeaway:** Exactly three active builds, each with its star rating in the slot list. This is the model for the plot HUD: build name + `×N ★`, one line per plot.
**Confidence:** H.

## 04 — Blueprint podium (gallery)
**File:** `frames/04_gallery_blueprint_podium.png`
**Timestamp:** VOD 44:30 (crop of the gallery floor)
**Shows:** A floating miniature build above a small gold pedestal with a red button. Floating label: `Ruin`, `★★`, `Materials: Aqua Concrete 6× / Prismarine Bricks 12×`. Another podium is at the right edge.
**Takeaway:** Each gallery slot is a physical podium with (a) a 3D preview, (b) name + star rating, (c) material list with counts, and (d) a pressable button. The preview is small — this is a "walk up and choose" interaction, not a menu.
**Confidence:** H for the podium/label; M for the button being the selection input (see Open questions).

## 05 — Gold Build booth in the gallery
**File:** `frames/05_gallery_gold_booth.png`
**Timestamp:** VOD 44:34
**Shows:** A separate booth under a large `GOLD BUILD` sign, with the same gold pedestal + button in front. A normal podium beside it reads `Game Set Match ★★`.
**Takeaway:** The Gold Build is a distinct gallery slot, not one of the seven normal slots. It uses the same select-by-button language as normal blueprints.
**Confidence:** H for layout; M for button interaction.

## 06 — First Gold Build selected
**File:** `frames/06_gold_selected_broadcast.png`
**Timestamp:** VOD 44:42
**Shows:** Centre title `Golden Build selected!`; top bar `GOLDEN BUILD IN: 01:58`; sidebar slots `Whale (×3 ★)`, `Game Set Match (×2 ★)`, `Ruin (×2 ★)`; chat `[✓] A new golden build is now available!`.
**Takeaway:** The first gold appears at 4:00 and is **manually selected** (the team did it at ~4:02). Selecting it resets the top countdown to the next gold (2:00 later). Gold is not one of the three normal slots.
**Confidence:** H.

## 07 — Shopping list book, page 1 of 3
**File:** `frames/07_shopping_list_book_page1.png`
**Timestamp:** VOD 43:46
**Shows:** Custom book GUI titled by the build name `Whale`, header `Page 1 of 3`, `Materials:` with `Andesite 6×`, `Andesite Stairs 10×`, `Cobblestone 8×`, `Cobblestone Slab 2×`, `Cobblestone Stairs 2×`, and a `Done` button.
**Takeaway:** The book is a custom GUI, not a vanilla written book. One page per active build (always 3 pages), exact quantities, and a close button. This is the cleanest reference for the plugin's book UI.
**Confidence:** H.

## 08 — Team chest with sorted materials
**File:** `frames/08_team_chest_gui.png`
**Timestamp:** VOD 44:36
**Shows:** A chest GUI with 12-stacks of several log/plank types, plus small stacks of coloured blocks; the player is depositing/withdrawing. Sidebar shows `Whale ×3`, `Game Set Match ×2`, `Ruin ×2`.
**Takeaway:** Teams pool materials in chests. The chest is a normal container; "five colour-coded chests" is an organisation aid, not a special shop.
**Confidence:** H for chest use; H for five colour-coded chests from the P25 notes (not visible as colours in this frame).

## 09 — Build completed hologram
**File:** `frames/09_build_completed_hologram.png`
**Timestamp:** VOD 43:48
**Shows:** In-world `BUILD COMPLETED!` / `COIN` above the plot; chat `[+1.00] [✓] Your team completed Coin (★★)`; sidebar still lists the other two builds.
**Takeaway:** Completion is acknowledged at the plot and broadcast to all teams immediately; the completed build leaves the slot list (the player is already back at a plot). The `+1.00` is the game coin multiplier, not the build payout.
**Confidence:** H for the completion flow; M for exact coin message semantics.

## 10 — Mart lower ring: coloured-concrete islands
**File:** `frames/10_mart_colour_islands.png`
**Timestamp:** VOD 44:28
**Shows:** Elytra flight over the lower ring: islands labelled `RED`, `ORANGE`, `PINK`, `WHITE`, `LIME` with large block-letter signs; blue/white floor between them; action bar `Blue Concrete Powder`.
**Takeaway:** Lower islands are colour families, each with a huge readable label. Keep labels this legible in the rebuild; the action-bar block name is the colourblind fallback.
**Confidence:** H.

## 11 — Mart upper island: logs
**File:** `frames/11_mart_log_island.png`
**Timestamp:** VOD 41:12
**Shows:** A multi-level wooden/log island with several log types on display, item labels, and the green action-bar `Birch Log`; elytra wings visible at the right.
**Takeaway:** Upper islands are material categories (logs/ores/stones), built as walkable multi-level structures rather than flat stalls. Players mine the displayed blocks directly.
**Confidence:** H.

## 12 — Portal from the build room to the mart
**File:** `frames/12_portal_to_mart.png`
**Timestamp:** VOD 42:06
**Shows:** A purple nether-style portal in a white/red frame at the end of a short carpeted corridor, with a `BUILD` sign above; `MART` visible beyond.
**Takeaway:** The room-to-mart transition is a portal in front of the build area, not a teleport command or an open walkway. It reads as a threshold, which helps orient new players.
**Confidence:** H.

## 13 — Inventory: kit, elytra, offhand book
**File:** `frames/13_inventory_kit_elytra_book.png`
**Timestamp:** VOD 41:48
**Shows:** Custom red-tinted inventory (team colour). Elytra in the chest slot; pickaxe/axe/shovel/shears in the hotbar; gathered blocks in the storage rows; shopping-list book in the **offhand** slot; `Haste II` effect panel at the right.
**Takeaway:** The book lives in the offhand so it is always one right-click away. The kit is tool + elytra + book; there is no shop currency and no armour beyond the elytra. The GUI tint follows team colour.
**Confidence:** H for layout/elytra/book/Haste; M for exact tool tier and enchantments (the pickaxe action bar says "Diamond Pickaxe"; enchants not readable here).

---

## What is deliberately missing
- A clean shot of an active **reference build** standing in the team room (the VOD never returns to base while a build is up). The wiki describes it; this pack cannot prove its exact form.
- A clean shot of a **Gold Build failing** and its blocks being removed (the 2-minute window expires after the clip ends). Wiki-only.
- The **selection chat line** ("You selected … as a Blueprint!") is not in these frames; only the podium/button and the resulting slot are visible.
- The **game-end results screen** (partial-completion coins + Employee of the Month). Wiki/official page only.
