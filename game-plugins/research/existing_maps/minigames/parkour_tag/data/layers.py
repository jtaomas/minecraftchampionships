import csv, collections
rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
by = collections.defaultdict(list)
for r in rows:
    by[int(r['y'])].append(r)
for y in sorted(by):
    rs = by[y]
    xs = [int(r['x']) for r in rs]
    zs = [int(r['z']) for r in rs]
    c = collections.Counter(r['block'] for r in rs)
    top = ';'.join(b.split(':')[1] + '=' + str(n) for b, n in c.most_common(4))
    print(f'y={y:2d} n={len(rs):4d} x{min(xs)}..{max(xs)} z{min(zs)}..{max(zs)}  {top}')
