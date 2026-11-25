# Video palette and semantic colour — Clouds 2 (X4l2WSQv0Wk)

Observed from the stills, not from the pack files. A10 owns the actual texture inventory.
Confidence: **H** = seen in multiple frames, **M** = seen once, **L** = inferred.

| Colour / material | Where it appears | Meaning the video teaches | Conf |
|---|---|---|---|
| White / off-white cloud, quartz, calcite | Floating islands, start apron, cave walls, finish apron | Safe ground / “cloud” | H |
| Green grass / hedge / lime concrete | Start hedge, castle, garden, lily pads | Garden / settlement / safe land | H |
| Blue water / blue ice / blue glass | Water tunnel, pools, blue causeway, tube | Water / ice / “go through” | H |
| Brown wood / dirt / mud brick | Castle bridge, fences, paths, arch legs | Wood / structure / bridge | H |
| Red spiral on black or magenta | Launch pads at 1:29, 1:38, 2:24, 2:32 | Jump / launch | H |
| Yellow/orange chevron | Start straight, castle, water garden, final slope | Speed / forward | H |
| Magenta / pink | Launch-pad frames, giant flowers, lily pads | Launch frame / decorative landmark | H |
| Black + green | Floor arrows, floating arrow panels, creeper statue | Direction / checkpoint / “look here” | H |
| Cyan | `Starting In` title | Countdown / UI accent | H |
| White | `You are in 1st place`, HUD text | UI text | H |
| Rainbow | Arch at 1:54 and 3:20 | Signature landmark / transition | H |

## What this palette does well

- **One colour, one meaning** for the route-critical surfaces: green = land, blue = water, yellow =
  speed, red = launch. The player can read the next surface before landing. **H**
- **The pads are the highest-saturation objects** in every frame; they survive fog, distance and
  the busy decoration. **H**
- **The landmarks are unique silhouettes**, not just colour: the ring, the rainbow, the castle
  roofs, the green arch. Colour + shape together. **H**

## Where it fails

- **White on white:** cloud, quartz, calcite and sky collapse into one value range; the next
  platform can disappear (frame `12`). **H**
- **Blue on blue:** water tunnel walls, fog and floor are all blue; only the red gates and the air
  bubbles keep orientation (frames `05`, `06`). **H**
- **Decoration saturation:** the giant creeper, flowers, cacti and checkerboard walls sometimes
  out-shout the pad; the route line is lost in the noise (frames `03`, `10`, `14`). **H**
- **No semantic HUD colour:** the action bar is white and static; there is no green = ahead / red =
  behind, no gold = leader. **H**

## Rebuild rule

Use the same **semantic** palette, not the same block-for-block art: one colour per meaning, pads
at maximum saturation, landmarks unique in silhouette, and a deliberate contrast break at every
transition (white cloud → blue water → green garden → dark cave). **M**
