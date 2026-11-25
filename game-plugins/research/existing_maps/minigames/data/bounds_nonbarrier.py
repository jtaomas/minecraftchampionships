import csv, sys
from collections import Counter
isl = int(sys.argv[1])
rows = [r for r in csv.DictReader(open("data/blocks_positions.csv", encoding="utf-8")) if int(r["island"]) == isl]
pts = [(int(r["x"]), int(r["y"]), int(r["z"]), r["block"].replace("minecraft:", "")) for r in rows]
for label, sel in (
    ("ALL", pts),
    ("NON-BARRIER", [p for p in pts if p[3] != "barrier"]),
    ("NON-BARRIER-NON-LIGHT", [p for p in pts if p[3] not in ("barrier", "light")]),
):
    if not sel:
        continue
    xs = [p[0] for p in sel]; ys = [p[1] for p in sel]; zs = [p[2] for p in sel]
    print(f"{label}: n={len(sel)} x {min(xs)}..{max(xs)} y {min(ys)}..{max(ys)} z {min(zs)}..{max(zs)}")
    # bounding area per y
    print("  y bands:")
    for a, b in ((0, 9), (10, 26), (27, 39), (40, 47), (48, 58), (59, 90)):
        s2 = [p for p in sel if a <= p[1] <= b]
        if s2:
            print(f"   {a}..{b}: n={len(s2)} x {min(p[0] for p in s2)}..{max(p[0] for p in s2)} z {min(p[2] for p in s2)}..{max(p[2] for p in s2)}  " +
                  ", ".join(f"{k}={v}" for k, v in Counter(p[3] for p in s2).most_common(5)))
