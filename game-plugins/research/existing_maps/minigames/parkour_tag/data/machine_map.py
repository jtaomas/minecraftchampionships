import csv, collections
rows = list(csv.DictReader(open('data/pt_commands.csv', encoding='utf-8')))
by = collections.defaultdict(list)
for r in rows:
    by[int(r['y'])].append(r)
for y in sorted(by):
    print(f'===== y={y} ({len(by[y])}) =====')
    grid = {}
    for r in by[y]:
        k = r['kind'][0].upper()
        if r['kind'].startswith('chain'):
            k = 'c'
        elif r['kind'].startswith('repeating'):
            k = 'r'
        elif r['kind'].startswith('command'):
            k = 'i'
        a = '*' if r['auto'] == '1' else ' '
        grid[(int(r['x']), int(r['z']))] = k + a
    xs = [x for x, z in grid]; zs = [z for x, z in grid]
    print('     ' + ''.join(str(x % 10) for x in range(min(xs), max(xs) + 1)))
    for z in range(min(zs), max(zs) + 1):
        line = ''.join(grid.get((x, z), ' .')[0:2].ljust(2) for x in range(min(xs), max(xs) + 1))
        # simpler: one char
        line = ''
        for x in range(min(xs), max(xs) + 1):
            line += grid.get((x, z), '.')[0]
        print(f'z{z:4d} {line}')
