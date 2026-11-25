#!/usr/bin/env python3
"""Spatial clusters of command blocks (3D union-find) + role text.

Writes data/command_clusters.csv and prints a summary.
"""
import csv, os, re, math
from collections import Counter, defaultdict

D = os.path.dirname(__file__)
rows = list(csv.DictReader(open(os.path.join(D, "command_blocks.csv"), encoding="utf-8")))
pts = [(int(r["island"]), int(r["x"]), int(r["y"]), int(r["z"]), r) for r in rows]
N = len(pts)
R = 8.0

parent = list(range(N))


def find(a):
    while parent[a] != a:
        parent[a] = parent[parent[a]]
        a = parent[a]
    return a


def union(a, b):
    ra, rb = find(a), find(b)
    if ra != rb:
        parent[rb] = ra


# spatial hash: bucket by 8-block cells, compare neighbouring cells
buckets = defaultdict(list)
for i, (isl, x, y, z, r) in enumerate(pts):
    buckets[(x // 8, y // 8, z // 8)].append(i)
for (bx, by, bz), ids in buckets.items():
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for dz in (-1, 0, 1):
                other = buckets.get((bx + dx, by + dy, bz + dz))
                if not other:
                    continue
                for i in ids:
                    for j in other:
                        if i >= j:
                            continue
                        a, b = pts[i], pts[j]
                        if a[0] != b[0]:
                            continue
                        if (a[1]-b[1])**2 + (a[2]-b[2])**2 + (a[3]-b[3])**2 <= R*R:
                            union(i, j)

groups = defaultdict(list)
for i in range(N):
    groups[find(i)].append(i)

ISL_ID = {0: "MG-REB-02", 1: "MG-REB-01", 2: "MG-HUB-01", 3: "MG-CMD-01", 4: "MG-PT-01",
          5: "MG-SB-01", 6: "MG-SC-01", 7: "MG-SR-HUB-01", 8: "MG-SR-RED-01", 9: "MG-SR-ORG-01"}

clusters = []
for ids in groups.values():
    isl = Counter(pts[i][0] for i in ids).most_common(1)[0][0]
    xs = [pts[i][1] for i in ids]; ys = [pts[i][2] for i in ids]; zs = [pts[i][3] for i in ids]
    cmds = [pts[i][4]["command"] for i in ids]
    kinds = Counter(pts[i][4]["kind"] for i in ids)
    teams = Counter()
    verbs = Counter()
    for c in cmds:
        for m in re.finditer(r"team[= ]([A-Za-z_]+)", c):
            teams[m.group(1)] += 1
        v = c.strip().lstrip("/").split()
        if v:
            verbs[v[0]] += 1
    clusters.append(dict(isl=isl, n=len(ids),
                         xmin=min(xs), xmax=max(xs), ymin=min(ys), ymax=max(ys),
                         zmin=min(zs), zmax=max(zs), kinds=kinds, teams=teams, verbs=verbs,
                         sample=cmds[0][:160] if cmds else ""))

clusters.sort(key=lambda c: (c["isl"], -c["n"]))
os.makedirs(D, exist_ok=True)
with open(os.path.join(D, "command_clusters.csv"), "w", encoding="utf-8", newline="") as fh:
    fh.write("cluster_id,arena_id,island,command_blocks,xmin,xmax,ymin,ymax,zmin,zmax,"
             "impulse,chain,repeating,top_teams,top_verbs,sample_command\n")
    for i, c in enumerate(clusters):
        cid = f"CB-{i+1:02d}"
        fh.write(f"{cid},{ISL_ID[c['isl']]},{c['isl']},{c['n']},{c['xmin']},{c['xmax']},"
                 f"{c['ymin']},{c['ymax']},{c['zmin']},{c['zmax']},"
                 f"{c['kinds'].get('command_block',0)},{c['kinds'].get('chain_command_block',0)},"
                 f"{c['kinds'].get('repeating_command_block',0)},"
                 f"\"{';'.join(f'{k}={v}' for k, v in c['teams'].most_common(4))}\","
                 f"\"{';'.join(f'{k}={v}' for k, v in c['verbs'].most_common(4))}\","
                 f"\"{c['sample'].replace(chr(34), chr(39))}\"\n")
        print(cid, ISL_ID[c["isl"]], "n=", c["n"],
              f"x {c['xmin']}..{c['xmax']} y {c['ymin']}..{c['ymax']} z {c['zmin']}..{c['zmax']}",
              "teams", c["teams"].most_common(3), "verbs", c["verbs"].most_common(3))
print("clusters", len(clusters))
