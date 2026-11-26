#!/usr/bin/env python3
"""Sample a vertical colour strip from a reference image to read band order."""
import sys
from PIL import Image

def strip(path, x, y0, y1, step=1):
    im = Image.open(path).convert("RGB")
    print(f"{path}  x={x}  y {y0}..{y1}")
    for y in range(y0, y1 + 1, step):
        r, g, b = im.getpixel((x, y))
        tag = ""
        mx = max(r, g, b)
        if mx < 40: tag = "black"
        elif r > 140 and g < 80 and b < 80: tag = "RED"
        elif r > 170 and 90 <= g <= 165 and b < 80: tag = "ORANGE"
        elif r > 170 and g > 165 and b < 110: tag = "YELLOW"
        elif g > 120 and r < 140 and b < 110: tag = "GREEN"
        elif g > 140 and b > 140 and r < 120: tag = "CYAN"
        elif b > 130 and r < 90: tag = "BLUE/PURPLE"
        elif mx > 200 and min(r,g,b) > 180: tag = "WHITE"
        print(f"  y={y:4d}  {r:3d},{g:3d},{b:3d}  {tag}")

if __name__ == "__main__":
    p = sys.argv[1]
    x = int(sys.argv[2]); y0 = int(sys.argv[3]); y1 = int(sys.argv[4])
    step = int(sys.argv[5]) if len(sys.argv) > 5 else 1
    strip(p, x, y0, y1, step)
