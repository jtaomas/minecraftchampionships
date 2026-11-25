import csv, collections
rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
X0, X1, Z0, Z1 = 1050, 1071, 984, 1005
Y0, Y1 = 10, 27
block = {(int(r['x']), int(r['y']), int(r['z'])): r['block'].split(':')[1] for r in rows}

def solid(x, y, z):
    if not (X0 <= x <= X1 and Y0 <= y <= Y1 and Z0 <= z <= Z1):
        return True
    return (x, y, z) in block

air = set()
for x in range(X0, X1 + 1):
    for y in range(Y0, Y1 + 1):
        for z in range(Z0, Z1 + 1):
            if not solid(x, y, z):
                air.add((x, y, z))

# open region flood from center
start = (1060, 20, 995)
open_cells = set()
q = [start]
if start in air:
    open_cells.add(start)
while q:
    x, y, z = q.pop()
    for dx, dy, dz in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)):
        n = (x+dx, y+dy, z+dz)
        if n in air and n not in open_cells:
            open_cells.add(n); q.append(n)

closed = air - open_cells
# cluster closed pockets
seen = set(); pockets = []
for c in sorted(closed):
    if c in seen: continue
    q = [c]; seen.add(c); comp = []
    while q:
        p = q.pop(); comp.append(p)
        x, y, z = p
        for dx, dy, dz in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)):
            n = (x+dx, y+dy, z+dz)
            if n in closed and n not in seen:
                seen.add(n); q.append(n)
    pockets.append(comp)
pockets.sort(key=lambda c: -len(c))
print('open air', len(open_cells), 'closed pockets', len(pockets))
for i, comp in enumerate(pockets[:20]):
    xs=[p[0] for p in comp]; ys=[p[1] for p in comp]; zs=[p[2] for p in comp]
    print(f'{i:2d} n={len(comp):4d} x{min(xs)}..{max(xs)} y{min(ys)}..{max(ys)} z{min(zs)}..{max(zs)}')
