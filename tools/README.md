# Build preview renderer

`render_preview.py` turns an `MCMDUMP` block dump (written by the server plugin) into a PNG preview.
By default it draws an isometric view. It can also draw a top-down map.

## Setup

```
python -m pip install --user pillow numpy
```

## Usage

```
python render_preview.py in.mcmdump.gz out.png [--angle 0..3] [--scale N] [--top]
```

| Option | Meaning |
|---|---|
| `--angle 0..3` | Rotate the view by 90° steps. Use this to see the other sides of a build. |
| `--scale N` | Sets the pixels per block. In isometric mode N is half the tile width and is rounded up to an even number (min 2). In `--top` mode N is the block size in pixels. Without this flag, the image is scaled to about 2000 px wide (the iso view is rendered at ~2x and then downsampled for smooth edges). |
| `--top` | Draws an orthographic top-down map instead. It uses height shading and outlines where the height changes. |
| `--no-ao`, `--no-shadows`, `--no-bloom` | Turn off ambient occlusion, cast shadows, or the glow around light-emitting blocks. |
| `--show-barriers` | Draws barrier blocks as translucent red. By default they are invisible, like air. |

Examples:

```
python render_preview.py arena.mcmdump.gz arena.png
python render_preview.py arena.mcmdump.gz arena_back.png --angle 2
python render_preview.py arena.mcmdump.gz arena_map.png --top
```

## Input format

The file is gzipped text. The first line is `MCMDUMP 1 <sx> <sy> <sz>`. Each following line is one block:
`x y z namespace:id[states]`. Coordinates are relative to the region minimum, and air is left out.
Block states are ignored. Empty space around the content is trimmed automatically.

## Rendering notes

- **Projection:** 2:1 isometric. Each block has three shaded faces (top light, left medium, right dark).
- **Visible faces only:** a face is drawn only when the neighbouring block on that side is air or a different non-occluding block.
- **Draw order:** blocks are painted back to front by diagonal slices, so translucent blocks (glass, water, ice, leaves, plants) blend correctly.
- **Lighting:** soft per-vertex ambient occlusion and directional cast shadows.
- **Light blocks:** glowstone, sea lanterns, froglights, lava, etc. are drawn brighter and get a bloom glow.
- **Background:** sky gradient and a soft drop shadow. The image is cropped automatically with a margin.
- **Colours:** there is a built-in table of average colours (all dyed families, stones, woods, ores, nether/end blocks, copper, plants and more).
  - Stairs, slabs, walls, fences, panes, doors and similar blocks use their base material's colour.
  - Unknown ids get a colour from keywords (e.g. `red`, `oak`, `stone`, `copper`). If no keyword matches, the colour comes from a hash of the name, so the same id always gets the same colour.

## Test data

```
python make_test_dump.py sample.mcmdump.gz          # floating island with towers, glass, leaves, water
python make_test_dump.py big.mcmdump.gz --big       # 400x200x400 stress test (~7M blocks)
python render_preview.py sample.mcmdump.gz sample_preview.png
```

Measured speed on this machine:

| Test | Blocks | Load | Render | Total |
|---|---|---|---|---|
| Sample island | ~101k | 0.07 s | ~3 s | ~3 s |
| Big stress test | ~7.1M | 4.3 s | 3.7 s | ~8 s |
