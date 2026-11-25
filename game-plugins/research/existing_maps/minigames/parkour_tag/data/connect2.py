import csv
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

def neighbours(c):
    x, y, z = c
    out = set()
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for dy in (0, 1, -1, -2, -3, -4, -5):
            n = (x + dx, y + dy, z + dz)
            if n in cells:
                out.add(n)
    return out

seen = set(); comps = []
for c in sorted(cells):
    if c in seen: continue
    q = [c]; seen.add(c); comp = []
    while q:
        p = q.pop(); comp.append(p)
        for n in neighbours(p):
            if n not in seen:
                seen.add(n); q.append(n)
    comps.append(comp)

comps.sort(key=lambda c: -len(c))
print('components', len(comps), 'cells', len(cells))
for i, comp in enumerate(comps[:25]):
    xs = [p[0] for p in comp]; ys = [p[1] for p in comp]; zs = [p[2] for p in comp]
    tag = []
    if (1061, 17, 982) in comp: tag.append('RUNNER')
    if (1060, 17, 1007) in comp: tag.append('HUNTER')
    if (1061, 28, 995) in comp: tag.append('ENTRY')
    print(f'{i:2d} n={len(comp):4d} x{min(xs)}..{max(xs)} y{min(ys)}..{max(ys)} z{min(zs)}..{max(zs)} {" ".join(tag)}')
