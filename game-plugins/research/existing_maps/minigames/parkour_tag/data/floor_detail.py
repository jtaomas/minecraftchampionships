import csv, collections
rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
for Y in (10, 11, 12, 13, 14, 15):
    print(f'===== y={Y} =====')
    by = collections.defaultdict(list)
    for r in rows:
        if int(r['y']) == Y:
            by[r['block'].split(':')[1]].append((int(r['x']), int(r['z'])))
    for b in sorted(by):
        pts = sorted(by[b])
        # compact: group rows
        print(f'-- {b} ({len(pts)})')
        rows_by_z = collections.defaultdict(list)
        for x, z in pts:
            rows_by_z[z].append(x)
        for z in sorted(rows_by_z):
            xs = sorted(rows_by_z[z])
            # ranges
            rng = []
            s = xs[0]; p = xs[0]
            for x in xs[1:]:
                if x == p + 1:
                    p = x
                else:
                    rng.append((s, p)); s = p = x
            rng.append((s, p))
            print(f'   z{z}: ' + ','.join(f'{a}' if a == b_ else f'{a}-{b_}' for a, b_ in rng))
