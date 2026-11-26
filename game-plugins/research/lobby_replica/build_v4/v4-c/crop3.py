#!/usr/bin/env python3
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REF = os.path.abspath(os.path.join(HERE, "..", "reference"))
OUT = os.path.join(HERE, "crops")

im = Image.open(os.path.join(REF, "StageAreaIntro.png")).convert("RGB")
# chat word "Kaos21a_Faniel49" around x 700..900, y 905..945
for i, box in enumerate([(690, 900, 900, 950), (860, 900, 1050, 950), (0, 895, 400, 950)]):
    c = im.crop(box)
    c = c.resize((c.width * 12, c.height * 12), Image.LANCZOS)
    c.save(os.path.join(OUT, f"word{i}.png"))
    print(c.size)
