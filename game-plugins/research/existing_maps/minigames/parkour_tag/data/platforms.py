import csv, collections
rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
X0, X1, Z0, Z1 = 1048, 1071, 978, 1010
block = {}
for r in rows:
    block[(int(r['x']), int(r['y']), int(r['z']))] = r['block'].split(':')[1]

# solid = any non-air block. Air = absent.
def solid(x, y, z):
    return (x, y, z) in block

def stand(x, y, z):
    # player feet at y, needs solid below, 2 air above
    return (not solid(x, y, z)) and (not solid(x, y + 1, z)) and solid(x, y - 1, z)

cells = set()
for x in range(X0, X1 + 1):
    for z in range(Z0, Z1 + 1):
        for y in range(10, 27):
            if stand(x, y, z):
                cells.add((x, y, z))

# cluster same-y 4-neighbour surfaces
seen = set()
plats = []
for c in sorted(cells):
    if c in seen:
        continue
    q = [c]
    seen.add(c)
    comp = []
    while q:
        x, y, z = q.pop()
        comp.append((x, y, z))
        for nx, nz in ((x+1,z),(x-1,z),(x,z+1),(x,z-1)):
            n = (nx, y, nz)
            if n in cells and n not in seen:
                seen.add(n); q.append(n)
    if len(comp) >= 2:
        xs = [p[0] for p in comp]; zs = [p[2] for p in comp]
        plats.append((y, len(comp), min(xs), max(xs), min(zs), max(zs), comp))

plats.sort(key=lambda p: (p[0], -p[1]))
print('y  area  x-range      z-range      floor material (below)')
for y, n, x0, x1, z0, z1, comp in plats:
    mats = collections.Counter()
    for x, _, z in comp:
        mats[block.get((x, y - 1, z), '?')] += 1
    mats = ','.join(f'{b}:{k}' for b, k in mats.most_common(3))
    print(f'{y:2d} {n:4d}  {x0:4d}..{x1:<4d}  {z0:4d}..{z1:<4d}  {mats}')

print('\ntotal standable cells', len(cells), 'platforms', len(plats))
# per level standable count
print('per y:', {y: sum(1 for c in cells if c[1] == y) for y in range(10, 27)})
