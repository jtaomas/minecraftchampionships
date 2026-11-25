import csv
from collections import defaultdict
rows = list(csv.DictReader(open("data/blocks_positions.csv", encoding="utf-8")))
isl_chunks = defaultdict(set)
for r in rows:
    x, z = int(r["x"]), int(r["z"])
    isl_chunks[int(r["island"])].add((x // 16, z // 16))
for isl in sorted(isl_chunks):
    regions = sorted({(cx // 32, cz // 32) for cx, cz in isl_chunks[isl]})
    chunks = sorted(isl_chunks[isl])
    print(f"ISL {isl}: {len(chunks)} chunks; regions: " + ", ".join(f"r.{a}.{b}" for a, b in regions))
