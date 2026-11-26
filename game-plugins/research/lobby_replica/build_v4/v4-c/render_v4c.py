#!/usr/bin/env python3
"""
Render the V4-C previews with the shared tools/render_preview.py.

The shared renderer has no entry for minecraft:player_head (it would fall back
to a hash colour), so this wrapper imports it, registers a skin-like colour for
heads and for the pedestal post, and then calls its own main().  Every pixel is
still produced by tools/render_preview.py.

Run:  python render_v4c.py
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, "..", "..", "..", "..", "..", "tools"))
sys.path.insert(0, TOOLS)

import render_preview as rp  # noqa: E402

# (top, side, alpha, emit, solid, strip)
rp.BLOCKS["player_head"] = ((52, 38, 30), (226, 184, 148), 1.0, 0.0, True, None)
rp.BLOCKS["oak_fence"] = ((150, 116, 74), (168, 132, 84), 0.85, 0.0, False, None)
rp.BLOCKS["oak_wall_sign"] = ((196, 160, 106), (176, 140, 92), 0.9, 0.0, False, None)
rp.BLOCKS["smooth_stone"] = ((162, 162, 162), (158, 158, 158), 1.0, 0.0, True, None)
rp.BLOCKS["smooth_quartz"] = ((238, 233, 226), (235, 229, 222), 1.0, 0.0, True, None)
rp.BLOCKS["stone_bricks"] = ((124, 123, 124), (122, 121, 122), 1.0, 0.0, True, None)
rp.BLOCKS["dirt_path"] = ((148, 122, 65), (134, 96, 67), 1.0, 0.0, True, None)
rp.BLOCKS["grass_block"] = ((95, 159, 53), (134, 96, 67), 1.0, 0.0, True, (95, 159, 53))

JOBS = [
    ("preview.mcmdump.gz", "preview_top.png", ["--top", "--scale", "10"]),
    ("preview.mcmdump.gz", "preview_iso.png", ["--angle", "0", "--scale", "8"]),
    ("preview.mcmdump.gz", "preview_iso_north.png", ["--angle", "2", "--scale", "8"]),
    ("detail_columns.mcmdump.gz", "preview_columns.png", ["--angle", "0", "--scale", "10"]),
    ("detail_plot_red.mcmdump.gz", "preview_plot_red.png", ["--angle", "0", "--scale", "16"]),
    ("detail_plot_pink.mcmdump.gz", "preview_plot_pink.png", ["--angle", "2", "--scale", "16"]),
]


def main():
    for src, out, args in JOBS:
        path = os.path.join(HERE, src)
        if not os.path.exists(path):
            print("missing", src)
            continue
        print(f"--- {src} -> {out}")
        rp.main([path, os.path.join(HERE, out)] + args)
    return 0


if __name__ == "__main__":
    sys.exit(main())
