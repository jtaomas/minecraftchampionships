#!/usr/bin/env python3
"""LBR-1 datapack lint: every command must be a well-formed 1.21.1 fill/setblock
inside the site box, and no single fill may exceed the 32768-block engine limit."""

from __future__ import annotations

import glob
import os
import re
import sys

SITE = (4860, 88, 4860, 5140, 130, 5140)
CMD = re.compile(
    r"^(fill|setblock) (-?\d+) (-?\d+) (-?\d+)"
    r"(?: (-?\d+) (-?\d+) (-?\d+))? (minecraft:[a-z0-9_]+(?:\[[^\]]*\])?)$")
LIMIT = 32768


def main() -> int:
    root = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                        "datapack", "data", "mcchub", "function", "lbr1")
    files = sorted(glob.glob(os.path.join(root, "*.mcfunction")))
    total = 0
    fail = 0
    biggest = 0
    for path in files:
        n = 0
        for ln, raw in enumerate(open(path, encoding="utf-8"), 1):
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            if line.startswith("function "):
                n += 1
                continue
            n += 1
            m = CMD.match(line)
            if not m:
                print(f"{os.path.basename(path)}:{ln}: unparsable: {line[:90]}")
                fail += 1
                continue
            kind = m.group(1)
            if kind == "fill":
                x0, y0, z0 = int(m.group(2)), int(m.group(3)), int(m.group(4))
                x1, y1, z1 = int(m.group(5)), int(m.group(6)), int(m.group(7))
                vol = (abs(x1 - x0) + 1) * (abs(y1 - y0) + 1) * (abs(z1 - z0) + 1)
                biggest = max(biggest, vol)
                if vol > LIMIT:
                    print(f"{os.path.basename(path)}:{ln}: fill {vol} > {LIMIT}")
                    fail += 1
                for (v, lo, hi) in ((x0, SITE[0], SITE[3]), (x1, SITE[0], SITE[3]),
                                    (y0, SITE[1], SITE[4]), (y1, SITE[1], SITE[4]),
                                    (z0, SITE[2], SITE[5]), (z1, SITE[2], SITE[5])):
                    if not lo <= v <= hi:
                        print(f"{os.path.basename(path)}:{ln}: coord {v} outside site")
                        fail += 1
            else:
                x, y, z = int(m.group(2)), int(m.group(3)), int(m.group(4))
                if not (SITE[0] <= x <= SITE[3] and SITE[1] <= y <= SITE[4]
                        and SITE[2] <= z <= SITE[5]):
                    print(f"{os.path.basename(path)}:{ln}: coord outside site")
                    fail += 1
        total += n
        print(f"{os.path.basename(path):22s} {n:6d} commands")
    print(f"\n{len(files)} files, {total:,} commands, largest fill {biggest:,} blocks")
    if fail:
        print(f"FAIL: {fail} problems")
        return 1
    print("PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
