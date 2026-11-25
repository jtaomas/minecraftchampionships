# The old Build Mart shopping trolley: what the footage shows, and how we copy it

Scope: the "Shopping Cart" of the Classic Mall map (MCC 3 to 13). Confidence: **H** = plainly visible or two sources,
**M** = one source or a strong inference, **L** = guess.

Footage used (all under `game-plugins/research/`):
- `buildmart/cart/videos/mcc13_b6v_TuEUhkI_8910.mp4` (Illumina POV, MCC 13, 2:28:30 to 2:31:30). Times below are
  seconds into this clip (clip 0 s = VOD 2:28:30). Extra frames I pulled: contact sheets at 1 fps (60-180 s), 2 fps around
  the first ride (88-98 s) and the hop-out (106-114 s), and full-size crops of the block-title text (108-113 s).
- `buildmart/cart/frames/ref/11_cart_ride_thirdperson.png`, `12_cart_ride_firstperson.png`, `13_cart_rail_mart.png`
- `mcc_archive/build_mart/bm-1/images/wiki_original_map_overview.png`, `bm-1/frames/021550`, `021610`, `021640`,
  `021710`, `021730`, `zz_detail_orig_shopping_carts_zoom.jpg`

## What we found

| # | Finding | Evidence | Conf. |
|---|---|---|---|
| 1 | **It is a vanilla boat underneath.** The shape is the boat's flat tray, the sound subtitle reads "Rowing" (the boat paddle sound) and "Footsteps" stops while riding. | `ref/11` and `ref/13` subtitles; clip 97-113 s subtitles "Rowing" | H |
| 2 | **The floor is frosted ice, retextured.** With block titles on, looking at the yellow floor patch and at the red carpet runner both read **"Frosted Ice"**. Frosted ice has 4 ages, each with its own texture: that is how one slippery block shows white tiles, red carpet, yellow tiles and stripes. Boats glide on any ice at ice speed (slipperiness 0.98), which is why the trolley is "faster than running". | clip 108.5 s and 109 s, block title "Frosted Ice" over the yellow patch and the red runner | H (block) / M (all four looks) |
| 3 | **No rails.** Trolleys go anywhere on the floor, cross carpets and junctions, and sit side by side in the plaza. | `021710`, `021730`, overview still | H |
| 4 | **Grey wire basket look**: a light-grey hull with a darker grid pattern, no paddles visible. A resource-pack texture on the boat entity. | `ref/12` (first person, the basket rim), `zz_detail_orig_shopping_carts_zoom.jpg` | H (look) / M (paddles hidden) |
| 5 | **Item "Shopping Cart" in hotbar slot 5.** The held-item name above the hotbar reads "Shopping Cart" (clip 94-95 s). The item is a grey cart icon. Everyone has one from the start (it is already in slot 5 during the pre-game countdown in the build zone, 88-93 s). | clip 88-95 s; `ref/12`; `audit/frames/official_cart_item_hotbar.png` | H |
| 6 | **Use the item on the floor, you sit in it.** At 11:59 she arrives on foot, at 11:57 she is seated: the trolley is put down and boarded within about 2 s. | clip 94-97 s | H (place + ride) / M (auto-seat vs a second click) |
| 7 | **The item is used up while you ride and comes back when you hop out.** Slot 5 is empty during the whole ride (97-110 s) and holds the cart again once she is on foot in the COLOURS aisle (113 s). No empty trolley is left on the floor where she got out. | clip 97-113 s hotbar | H |
| 8 | **Steering is normal boat steering**: WASD, momentum, wide turns, long glides; "faster but with less control". Players can switch to running at any time. | Fandom map page via `cart/README.md` §3.1 (quote); clip 97-110 s camera drift | H |
| 9 | **Walls**: the trolley stops against shop fronts and shelves like a boat (no bounce damage). **Players**: trolleys bump and push each other like boats; they cluster at the start in the plaza. | `021710` (cluster), clip 100-106 s | M |
| 10 | **Pitfalls**: falling in a hole (on foot or in a trolley) loses all carried resources. The tutorial says it word for word. A "<name> was thrown out by security!" line appears in chat during the game. | `021610` tutorial text; `021950` chat line | H (rule) / M (message is the fall message) |
| 11 | **It carries the player, not blocks.** The gathered blocks stay in the hotbar. | `ref/11`, `ref/12` | H |
| 12 | The trolley only matters in the mall. In the build zone people walk (it was in slot 5 there but never used). | clip 60-93 s | M |

## How it is implemented (`src/main/java/com/mcm/games/buildmart/Trolley.java`)

- **A real vanilla boat, so the physics are exactly MCC's**: the boat is driven by the player's client, on frosted
  ice, so speed, drift, steering, wall stops and boat-to-boat pushing are identical to the original. This overrules the
  plan's "server-driven vehicle" idea (§4.9): the footage shows real boat behaviour (rowing sound, WASD boat handling,
  frosted ice floor), so a fake vehicle would feel different.
- **Boat type: mangrove.** No other game in `game-plugins/` uses mangrove boats (Race and TGTTOS spawn oak boats;
  Survival Games gives oak; Bingo lists birch, cherry chest and bamboo raft items). The pack retextures only the mangrove
  boat entity and item into the grey basket and the cart icon.
- **Floor: frosted ice everywhere you can ride** (`MallMap`): age 0 = white tile floor, age 1 = red carpet, age 2 =
  yellow tiles, age 3 = hazard stripes. Melting is switched off (random ticks 0, `BlockFadeEvent` cancelled). The pack
  retextures the four ages; without the pack the floor looks like plain frosted ice but rides the same.
- **Item**: "Shopping Cart" (mangrove boat, kit-tagged) in hotbar slot 5, given with the kit; cannot be dropped or put in
  chests.
- **Spawn**: use the item on the mall floor -> vanilla places the boat (and uses up the item) -> next tick you are
  seated. One trolley per player; nobody else can sit in yours; it cannot be broken.
- **Exit**: sneak -> the trolley disappears and the item returns to slot 5.
- **Where**: only inside the mall during play. In a build zone the item does nothing ("Your Shopping Cart only works in
  Build Mart!").
- **Pitfalls / portal / quit / game end**: the trolley is removed and the item given back; on a fall you also lose every
  carried resource, keep the kit, and go back to your build zone, with "<name> was thrown out by security!".

## Not certain / left as-is
- Whether MCC seated you automatically or needed a second right-click (clip shows only 2 s between): we auto-seat (M).
- The exact basket texture and the cart item icon are drawn by us from the frames, not copied.
- Top speed was not measured from footage (no fixed reference distance in frame); it is vanilla ice-boat speed by
  construction, which is what MCC used.
