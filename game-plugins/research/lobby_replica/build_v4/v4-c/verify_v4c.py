#!/usr/bin/env python3
"""
Independent check of V4-C's blocks.csv.

Re-derives everything from the CSV alone - it does not import the generator, so
a bug in gen_v4c.py cannot hide behind a matching bug in its own report.

Run:  python verify_v4c.py
"""
import csv
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))

TEAMS = [
    ("red", "red"), ("orange", "orange"), ("yellow", "yellow"),
    ("lime", "lime"), ("green", "green"), ("cyan", "cyan"),
    ("aqua", "light_blue"), ("blue", "blue"), ("purple", "purple"),
    ("pink", "pink"),
]
ZC0, PITCH = -42, 16
COL_WEST_X, COL_EAST_X = -7, 5
WEST_BACK_X, EAST_BACK_X = -22, 22
PLOT_W, PLOT_D = 13, 13


def side(i):
    return "W" if i < 5 else "E"


def zc(i):
    return ZC0 - PITCH * i if i < 5 else ZC0 - PITCH * (9 - i)


def plot_plate(i):
    a = WEST_BACK_X if side(i) == "W" else EAST_BACK_X
    out = {}
    for u in range(PLOT_W):
        for v in range(PLOT_D):
            x = a + v if side(i) == "W" else a - v
            out[(u, v)] = (x, zc(i) - PLOT_W // 2 + u)
    return out


def cell(plate, u, v, y):
    """plot-local (u,v,y) -> world key (x,y,z)."""
    x, z = plate[(u, v)]
    return (x, y, z)


def main():
    rows = []
    with open(os.path.join(HERE, "blocks.csv"), newline="", encoding="utf-8") as fh:
        rd = csv.reader(fh)
        head = next(rd)
        assert head == ["x", "y", "z", "block"], head
        for x, y, z, b in rd:
            rows.append((int(x), int(y), int(z), b))
    B = {(x, y, z): b for x, y, z, b in rows}
    fails, notes = [], []

    def need(cond, msg):
        (notes if cond else fails).append(msg)

    need(len(B) == len(rows), f"{len(rows)} rows, no duplicate coordinates")

    # --- global envelope ---------------------------------------------------
    need(min(y for _, y, _, _ in rows) >= 121, "nothing below y=121")
    need(max(y for _, y, _, _ in rows) == 133, "top block at y=133 (tallest head)")
    need(all(abs(x) <= 150 and abs(z) <= 150 for x, _, z, _ in rows),
         "inside the x/z +-150 build area")
    rmin = min(math.hypot(x, z) for x, _, z, _ in rows)
    need(rmin > 30, f"outside the dome radius 30 (closest block {rmin:.1f})")
    need(not any(abs(x) <= 4 and -114 <= z <= -30 for x, _, z, _ in rows),
         "avenue x -4..4 left empty for V4-A")

    # --- the ten columns ---------------------------------------------------
    for i, (team, dye) in enumerate(TEAMS):
        cx = (COL_WEST_X + 1) if side(i) == "W" else (COL_EAST_X + 1)
        cz = zc(i)
        conc = f"minecraft:{dye}_concrete"
        powd = f"minecraft:{dye}_concrete_powder"
        shaft = [(cx + dx, y, cz + dz)
                 for dx in (-1, 0, 1) for dz in (-1, 0, 1) for y in range(122, 129)]
        ok_shaft = sum(1 for k in shaft if B.get(k) in (conc, powd))
        cap = [(cx + dx, 128, cz + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1)]
        ok_cap = all(B.get(k) == powd for k in cap)
        pad = [(cx + dx, 121, cz + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1)]
        ok_pad = all(B.get(k) == "minecraft:smooth_stone" for k in pad)
        heads = {}
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                for y in range(129, 136):
                    if B.get((cx + dx, y, cz + dz), "").startswith("minecraft:player_head"):
                        heads[(dx, dz)] = y
        ok_heads = len(heads) == 4
        desc = [heads.get(c) for c in ((-1, 1), (1, 1), (1, -1), (-1, -1))]
        ok_desc = ok_heads and desc == sorted(desc, reverse=True) and len(set(desc)) == 4
        need(ok_shaft == 63 and ok_pad and ok_cap,
             f"{team}: 3x3 column, pad + 7-course shaft + powder cap ({ok_shaft}/63)")
        need(ok_heads, f"{team}: four pedestal heads ({len(heads)})")
        need(ok_desc, f"{team}: pedestal heights descending SW..NW {desc}")
        need(B.get((cx, 125, cz + 2), "").startswith("minecraft:oak_wall_sign"),
             f"{team}: column nameplate on the south face")

    # --- the ten plots -----------------------------------------------------
    for i, (team, dye) in enumerate(TEAMS):
        plate = plot_plate(i)
        conc = f"minecraft:{dye}_concrete"
        carp = f"minecraft:{dye}_carpet"
        terr = f"minecraft:{dye}_terracotta"
        powd = f"minecraft:{dye}_concrete_powder"

        deck = [cell(plate, u, v, 123) for u in range(PLOT_W) for v in range(PLOT_D - 1)]
        need(all(B.get(k) == conc for k in deck),
             f"{team}: 13x12 team-concrete deck")
        need(all(B.get(cell(plate, u, 12, 121)) == "minecraft:smooth_quartz"
                 and B.get(cell(plate, u, 12, 122)) == "minecraft:smooth_quartz"
                 for u in range(PLOT_W)),
             f"{team}: 13-wide quartz front step")

        structs = {(u, 4) for u in (2, 5, 8, 11)}   # statue plinths stand on the field
        carpet = [cell(plate, u, v, 124) for u in range(1, PLOT_W - 1)
                  for v in range(2, PLOT_D - 1)]
        rim_n = 0
        bad = []
        for u in range(1, PLOT_W - 1):
            for v in range(2, PLOT_D - 1):
                k = cell(plate, u, v, 124)
                if (u, v) in structs:
                    exp = "minecraft:smooth_stone"
                elif u in (1, PLOT_W - 2) or v in (2, PLOT_D - 2):
                    exp = terr
                    rim_n += 1
                else:
                    exp = carp
                if B.get(k) != exp:
                    bad.append((u, v, B.get(k)))
        need(len(carpet) == 11 * 10 and rim_n == 37,
             f"{team}: carpet field 11x10, one-block rim (37 visible cells; the "
             f"right-hand statue plinth replaces the 38th)")
        need(not bad, f"{team}: carpet/rim/plinth layers exact ({len(bad)} wrong)")

        wall = [cell(plate, u, v, y) for u in range(PLOT_W) for v in (0, 1)
                for y in range(124, 129)]
        solid = sum(1 for k in wall if B.get(k) in (conc, powd, "minecraft:light_gray_concrete"))
        need(solid + 27 == len(wall), f"{team}: backdrop wall with a 9x3 recess ({solid}/{len(wall)})")
        need(all(B.get(cell(plate, u, 1, y)) is None
                 for u in range(2, PLOT_W - 2) for y in range(125, 128)),
             f"{team}: panel opening is air in the front wall layer")
        need(all(B.get(cell(plate, u, 0, y)) == "minecraft:light_gray_concrete"
                 for u in range(2, PLOT_W - 2) for y in range(125, 128)),
             f"{team}: recessed light-grey panel behind the opening")

        heads = 0
        for u in (2, 5, 8, 11):
            x, z = plate[(u, 4)]
            if B.get((x, 124, z)) == "minecraft:smooth_stone" \
               and B.get((x, 127, z)) == "minecraft:oak_fence" \
               and B.get((x, 128, z), "").startswith("minecraft:player_head"):
                heads += 1
        need(heads == 4, f"{team}: four statue heads at u 2/5/8/11 ({heads})")
        need(B.get(cell(plate, PLOT_W // 2, 12, 123), "").startswith("minecraft:oak_wall_sign"),
             f"{team}: team nameplate on the front step")

    # --- roster order along the walk --------------------------------------
    order = [t for t, _ in TEAMS]
    dz = [zc(i) for i in range(5)]
    need(dz == sorted(dz, reverse=True), "west row runs red -> green south to north")
    de = [zc(i) for i in range(5, 10)]
    need(de == sorted(de), "east row runs cyan -> pink north to south")
    need(all(abs(zc(i) - zc(9 - i)) == 0 for i in range(5)),
         "pairs across the avenue share a z (red/pink, orange/purple, ...)")
    need(len(set(order)) == 10, "ten distinct teams")

    # --- heads / signs ----------------------------------------------------
    named = [b for b in B.values() if b.startswith("minecraft:player_head{profile")]
    bare = [b for b in B.values() if b == "minecraft:player_head"]
    names = sorted({b.split('"')[1] for b in named})
    need(len(named) == 6, f"six named heads ({len(named)})")
    need(len(bare) == 74, f"74 placeholder heads ({len(bare)})")
    need(names == ["Chocolate_Orange", "HeerTimothy", "Kaos21la_Faniel49"],
         f"only the three legible Lime Llamas names are used: {names}")
    signs = [b for b in B.values() if "oak_wall_sign" in b]
    need(len(signs) == 20, f"twenty team labels ({len(signs)})")

    print(f"blocks {len(B):,}   closest to dome centre {rmin:.1f}")
    for m in notes:
        print("  ok   " + m)
    for m in fails:
        print("  FAIL " + m)
    print(f"\n{len(notes)} checks passed, {len(fails)} failed")
    print("RESULT: " + ("PASS" if not fails else "FAIL"))
    return 0 if not fails else 1


if __name__ == "__main__":
    sys.exit(main())
