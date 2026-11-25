import csv, collections, heapq
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

def clear_column(x, y, z):
    # feet and head free
    return (x, y, z) not in block and (x, y + 1, z) not in block

def can_move(a, b):
    ax, ay, az = a; bx, by, bz = b
    dx = bx - ax; dz = bz - az; dy = by - ay
    if dx == 0 and dz == 0:
        return dy == 0
    hd = max(abs(dx), abs(dz))
    if hd > 4 or dy > 1 or dy < -8:
        return False
    # path clearance at source feet and destination feet (approximate)
    if not clear_column(ax, ay, az):
        return False
    # check intermediate columns at the higher of the two levels
    steps = max(abs(dx), abs(dz))
    for i in range(1, steps):
        px = ax + round(dx * i / steps)
        pz = az + round(dz * i / steps)
        if not clear_column(px, max(ay, by), pz):
            return False
    return True

# build neighbours with 26-direction limited
def neighbours(c):
    x, y, z = c
    out = []
    for dx in range(-4, 5):
        for dz in range(-4, 5):
            if dx == 0 and dz == 0:
                continue
            d2 = dx * dx + dz * dz
            if d2 > 16:
                continue
            for dy in range(-8, 2):
                n = (x + dx, y + dy, z + dz)
                if n in cells and can_move(c, n):
                    out.append(n)
    return out

# BFS from runner spawn (standable cell below spawn)
def bfs(start):
    dist = {start: 0}
    prev = {}
    q = collections.deque([start])
    while q:
        c = q.popleft()
        for n in neighbours(c):
            if n not in dist:
                dist[n] = dist[c] + 1
                prev[n] = c
                q.append(n)
    return dist, prev

runner = (1061, 16, 982)
hunter = (1060, 16, 1007)
print('runner standable?', runner in cells, 'hunter?', hunter in cells)
dist, prev = bfs(runner)
print('reachable from runner:', len(dist), 'of', len(cells))
for tgt, label in [((1060, 16, 1007), 'hunter spawn'),
                   ((1061, 11, 984), 'runner door floor'),
                   ((1060, 11, 1005), 'hunter door floor'),
                   ((1057, 25, 991), 'NW top balcony'),
                   ((1058, 25, 992), 'centre plus'),
                   ((1069, 23, 988), 'NE balcony'),
                   ((1053, 25, 1002), 'SW balcony'),
                   ((1067, 25, 1001), 'SE roof')]:
    if tgt in dist:
        print(f'{label}: {dist[tgt]} moves')
    else:
        print(f'{label}: UNREACHABLE')

# path to hunter spawn
if hunter in dist:
    path = [hunter]
    while path[-1] != runner:
        path.append(prev[path[-1]])
    path.reverse()
    print('\nshortest runner->hunter path ({} cells):'.format(len(path)))
    print(path)
