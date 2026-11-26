#!/usr/bin/env python3
"""Find the bounding box of a colour class inside an image region (measure refs)."""
import sys
import numpy as np
from PIL import Image

def main(path, x0, y0, x1, y1, kind):
    im = np.asarray(Image.open(path).convert("RGB")).astype(np.int16)
    sub = im[y0:y1, x0:x1]
    r, g, b = sub[..., 0], sub[..., 1], sub[..., 2]
    if kind == "cyan":
        m = (b > 150) & (g > 110) & (r < 110) & (b > r + 60)
    elif kind == "red":
        m = (r > 110) & (g < 70) & (b < 70)
    elif kind == "white":
        m = (r > 200) & (g > 200) & (b > 200)
    else:
        raise SystemExit("kind?")
    ys, xs = np.nonzero(m)
    print(f"{kind}: {len(xs)} px, bbox x {x0+xs.min()}..{x0+xs.max()} "
          f"({xs.max()-xs.min()+1} px)  y {y0+ys.min()}..{y0+ys.max()} "
          f"({ys.max()-ys.min()+1} px)")
    # column profile (width of mask per row) at a few rows
    for frac in (0.1, 0.25, 0.5, 0.75, 0.9):
        yy = int(ys.min() + frac * (ys.max() - ys.min()))
        row = np.nonzero(m[yy - y0])[0]
        if len(row):
            print(f"   row y={y0+yy:4d}  x {x0+row.min()}..{x0+row.max()}  w={row.max()-row.min()+1}")

if __name__ == "__main__":
    a = sys.argv
    main(a[1], int(a[2]), int(a[3]), int(a[4]), int(a[5]), a[6])
