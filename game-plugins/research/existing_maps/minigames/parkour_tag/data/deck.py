import csv, collections
rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
grid = {(int(r['x']), int(r['y']), int(r['z'])): r['block'].split(':')[1] for r in rows}

print('--- deck around runner sign 1050 29 991 ---')
for y in (28, 29, 30, 31):
    print('y', y)
    for z in range(986, 997):
        line = ''
        for x in range(1045, 1056):
            b = grid.get((x, y, z))
            line += '.' if b is None else b[0].upper()
        print(f'z{z}: {line}')
print('--- deck around hunter sign 1050 29 1000 ---')
for y in (28, 29, 30, 31):
    print('y', y)
    for z in range(996, 1007):
        line = ''
        for x in range(1045, 1056):
            b = grid.get((x, y, z))
            line += '.' if b is None else b[0].upper()
        print(f'z{z}: {line}')

print('\n--- exact blocks near pads ---')
for (x, y, z), b in sorted(grid.items()):
    if 1048 <= x <= 1053 and 28 <= y <= 30 and 988 <= z <= 1003:
        print(x, y, z, b)
