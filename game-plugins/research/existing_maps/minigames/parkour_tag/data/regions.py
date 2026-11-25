import csv, collections
rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))

def region(x, y, z):
    if 1050 <= x <= 1071 and 984 <= z <= 1005 and 10 <= y <= 26:
        return 'arena-box'
    if z < 984 and 977 <= z <= 983:
        return 'north-vestibule'
    if z > 1005 and 1006 <= z <= 1012:
        return 'south-vestibule'
    if y <= 9:
        return 'command-machine'
    if 27 <= y <= 39:
        return 'lobby'
    if 40 <= y <= 47:
        return 'above-lobby'
    if y >= 48:
        return 'banner'
    return 'other'

agg = collections.defaultdict(collections.Counter)
for r in rows:
    x, y, z = int(r['x']), int(r['y']), int(r['z'])
    agg[region(x, y, z)][r['block'].split(':')[1]] += 1
for k in sorted(agg):
    total = sum(agg[k].values())
    print(f'== {k}: {total}')
    for b, n in agg[k].most_common(12):
        print(f'   {n:5d} {b}')

print('\n-- other detail --')
for r in rows:
    x, y, z = int(r['x']), int(r['y']), int(r['z'])
    if region(x, y, z) == 'other':
        print(x, y, z, r['block'])
