# Build Mart interaction checklist — 15 testable bullets

1. **Gallery stock:** exactly 7 pedestals spawn with 2×1★, 2×2★, 1×3★, 1×4★, 1×5★; each shows a 3D diorama, name, stars and material count.
2. **Select:** right-clicking a select pad fills the next free plot, spawns the reference, adds a sidebar slot and a book page; a fourth selection is denied with `All 3 plots busy`.
3. **Preview / deliberate input:** right-clicking a diorama previews it without selecting; walking across the pad does **not** select a build.
4. **Rotation:** every 90 s only unselected gallery slots restock; active builds are untouched; a completed build can be repeated only while its blueprint is still displayed.
5. **Sidebar:** empty state reads `Empty Plot #1/#2/#3`; selected state shows name + star count for all three; it updates on select, complete and cancel.
6. **Book layout:** cover + paginated material pages; `Page N of M`; the build title appears only on the first page of that build; continuation pages say `(cont.)`; long names wrap, never truncate.
7. **Book contents:** every line has a block icon, full name, count and source badge (`I/S/C/G`); the badge tooltip shows the recipe/source; no colour-only cue.
8. **Book lifecycle:** the book is in slot 8, survives void, is reissued on join/respawn, is never duplicated, and refreshes on selection/completion/gold without closing an open book.
9. **Plot reference:** each active plot has a physical 3D reference + floating name/stars/materials; the plot is empty in front; the reference is removed when the slot is freed.
10. **Completion:** exact block + rotation match completes; extra/wrong blocks prevent completion and show red particles + `N wrong`; double slabs count as a full block.
11. **Partial:** incomplete builds track best percentage and pay at game end; the results line names the build and percentage.
12. **Gold:** first gold at 4:00 elapsed, then every 2:00; 7 stars; separate plot; no team-area reference; 2:00 to complete from memory; failure clears the gold plot; no retroactive score.
13. **Gold HUD:** top-centre countdown uses elapsed time (`GOLDEN BUILD IN: 03:11` at 11:11 remaining) and switches to `GOLD — 01:42 left` when active.
14. **Cart:** team-only 27-slot chest minecart at the plot, not rideable; accepts only manifest materials; hologram `delivered/required`; contents survive void; surplus is gray and retrievable; cleared at game end.
15. **Edge paths:** solo, late join, disconnect/rejoin, three simultaneous builds, build rotation, gold expiry, full inventory and game end all have explicit, tested behaviour with no orphan books/entities.

**Done means** all 15 pass on a dedicated test server, the book survives a void death, a three-build state can complete out of order, and a gold expiry at 0:00 cannot be scored afterwards.
