import csv, collections
rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
X0, X1, Z0, Z1 = 1048, 1071, 978, 1010
block = {(int(r['x']), int(r['y']), int(r['z'])): r['block'].split(':')[1] for r in rows}

def solid(x, y, z):
    return (x, y, z) in block

def stand(x, y, z):
    return (not solid(x, y, z)) and (not solid(x, y + 1, z)) and solid(x, y - 1, z)

cells = set()
for x in range(X0, X1 + 1):
    for z in range(Z0, Z1 + 1):
        for y in range(1, 28):
            if stand(x, y, z):
                cells.add((x, y, z))

# movement graph, junction-aware: walk on same level with step 0/1, gap jumps up to 3, drops down up to 6
def neighbours(c):
    x, y, z = c
    out = set()
    for dx in range(-4, 5):
        for dz in range(-4, 5):
            if dx == 0 and dz == 0:
                continue
            d2 = dx * dx + dz * dz
            if d2 > 16:
                continue
            for dy in range(-1, 7):
                if dy == -1 and d2 > 1:
                    continue
                n = (x + dx, y + dy, z + dz)
                if n in cells:
                    # don't allow jumping through a wall for horizontal moves: quick check midpoint column
                    if dy >= 0 and d2 > 1:
                        # must have clear line roughly: check feet-level along path
                        blocked = False
                        steps = max(abs(dx), abs(dz))
                        for i in range(1, steps):
                            px = x + round(dx * i / steps)
                            pz = z + round(dz * i / steps)
                            if solid(px, y + 1, pz) or solid(px, y + 2, pz):
                                blocked = True
                                break
                        if blocked:
                            continue
                    out.add(n)
    return out

seen = set()
comps = []
for c in sorted(cells):
    if c in seen:
        continue
    q = [c]; seen.add(c); comp = []
    while q:
        p = q.pop(); comp.append(p)
        for n in neighbours(p):
            if n not in seen:
                seen.add(n); q.append(n)
    comps.append(comp)

comps.sort(key=lambda c: -len(c))
print('components', len(comps))
for i, comp in enumerate(comps[:15]):
    xs = [p[0] for p in comp]; ys = [p[1] for p in comp]; zs = [p[2] for p in comp]
    print(f'{i:2d} n={len(comp):4d} x{min(xs)}..{max(xs)} y{min(ys)}..{max(ys)} z{min(zs)}..{max(zs)}')
    print('     sample', sorted(comp)[:6])
    print('     has runner spawn:', (1061, 17, 982) in comp, ' hunter spawn:', (1060, 17, 1007) in comp)
