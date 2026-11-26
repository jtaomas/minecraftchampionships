#!/usr/bin/env python3
"""Orthographic side elevation of an MCMDUMP (viewed from +x toward -x; z runs left to right, y up).
Usage: python side_view.py in.mcmdump.gz out.png [scale]"""
import sys
sys.path.insert(0, r"C:\Users\LeonS\Desktop\Mcc\tools")
import numpy as np
import render_preview as rp


def main():
    src, dst = sys.argv[1], sys.argv[2]
    scale = int(sys.argv[3]) if len(sys.argv) > 3 else 3
    grid, names, _ = rp.load_dump(src)
    lut = rp.build_luts(names)
    # render_top looks down axis 1 (later = nearer); axis 0 -> image columns, axis 2 -> image rows.
    g = np.ascontiguousarray(np.transpose(grid, (2, 0, 1))[:, :, ::-1])
    C, A, E = rp.render_top(g, lut, scale)
    img = rp.compose(C, A, E, 4)
    img.save(dst)
    print("wrote", dst, img.size)


if __name__ == "__main__":
    main()
