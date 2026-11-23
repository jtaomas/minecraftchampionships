# Railroad Rush (`/mcm play railroadrush`)

Plugin `McmRailroadRush`, game id `railroadrush`. This is a team puzzle game where each side builds the longest railroad it can through its own canyon course. It is based on the MCC wiki's description of Railroad Rush. The builds and code are original.

## Rules
- Each side (team or solo player) gets its own walled lane in the world `mcc_railroadrush`. The course runs: start pad, then the tutorial room, copper door 1, room 2, door 2, room 3, door 3, and finally the golden station.
- Everyone starts with an unbreakable iron pickaxe, unlimited **weak planks** and 8 rails. Weak planks crumble after 8 seconds, and rails can't be placed on them.
- Place rails starting from the fixed start rail. Curves and slopes form the vanilla way.
- **Gold** is a shared team pool. Earn it by mining gold ore in the lane walls and rock piles, or with dynamite.
  - Small sides get more gold per ore: x3 for a solo player, x2 for 2–3 players, x1 for 4 or more.
- Spend gold at the **Gold Trader** villager on the pad, and at each new room:
  - rails x8 (4 gold) and powered rail (3 gold), available from the start
  - team-concrete track bed x4 (4), sand x4 (2), dynamite x2 (4) and shears (4), all unlocked after checkpoint 1
- **Checkpoint 1** opens when your connected track reaches the copper gate.
- From then on, a **TNT cart** spawns on your track at the start of the current room every 11 s, up to 2 at a time.
  - It moves about 4 blocks/s and loses 1 power per block, and also loses power while stuck at the end of the track.
  - A powered rail adds +18 power, up to a cap of 48.
  - It explodes when its power runs out, when it derails, or when it reaches the next copper door. Exploding at the door blasts it open (doors 2 and 3).
- **Explosions** (TNT carts and dynamite) affect blocks like this:
  - stone goes down one level per blast: stone bricks → cracked → cobblestone → gravel → gone
  - leaves, sand and weak planks are destroyed
  - gold ore is auto-collected
  - rails, concrete, terrain and copper are never broken (only a cart can open a door)
- Leaves need shears or an explosion. Cobblestone and gravel can be mined.
- **Gold Rush** happens at 0:45 (40 s), 3:20 (35 s) and 5:40 (35 s).
  - Everyone is teleported to a shared mine. Gold ore gives 1 gold and deepslate gold ore gives 2.
  - Each gold ore has a 6% chance to spawn a "Fool's Gold" skeleton, the only thing that can hurt you. If it would kill you, you respawn instead.
  - TNT carts pause during the rush, and everyone returns to where they were afterwards.
- Falling into a chasm or the void sends you back to your latest checkpoint.
- The game lasts 8 minutes. Then a **golden cart** runs every track from the start under the same power rules. Its *reach* (forward blocks, loops don't count) is the final result.

## Scoring (per player, all members of a side get the same)
- Checkpoint opened: **15** each, awarded live (3 doors).
- Emerald ore mined: **5** to the miner. These are rare and sit in the lane walls of rooms 2 and 3.
- End of game: **1 point per 5 blocks** of golden-cart reach.
- Placement by reach: **40 / 32 / 27 / 23 / 19 / 16 / 13 / 10 / 8 / 6**, then 4.
  - Ties are broken by checkpoints opened, then raw track length, then fewer rails used.
- A top player gets about 100–130 points.

## Known limitations
- Carts are scripted displays (a TNT block and a gold block) that follow rail shapes, not vanilla minecart physics. Powered rails don't need redstone, as in MCC.
- Caches, creepers and the end-of-game score breakdown screen from MCC are not included.
- The sidebar shows the top 5 by golden-cart reach. Per-player gold and checkpoint info is on the action bar.
- Every side gets its own lane, 24 blocks apart. With 40 solo players the build is about 2M blocks, written over roughly 3 seconds before the game starts.
- The arena is cleared over a few ticks after the game ends. It is also fully overwritten at the next start, using a lane count stored in the world.
- Late joiners get the default spectator mode.
