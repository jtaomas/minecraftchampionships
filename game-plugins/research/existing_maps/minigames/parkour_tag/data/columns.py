import csv, collections
rows = list(csv.DictReader(open('data/pt_commands.csv', encoding='utf-8')))
by = collections.defaultdict(list)
for r in rows:
    if int(r['y']) > 9:
        continue
    by[(int(r['x']), int(r['z']))].append(r)
for k in sorted(by, key=lambda p: (p[1], p[0])):
    rs = sorted(by[k], key=lambda r: int(r['y']))
    print(f'== column x={k[0]} z={k[1]}  n={len(rs)}')
    for r in rs:
        print(f"   y{r['y']} {r['kind'][:9]:9} a{r['auto']} c{r['condition_met']}  {r['command']}")
