import csv, collections
rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
block = {(int(r['x']), int(r['y']), int(r['z'])): r['block'].split(':')[1] for r in rows}

def solid(x, y, z):
    return (x, y, z) in block

def stand(x, y, z):
    return (not solid(x, y, z)) and (not solid(x, y + 1, z)) and solid(x, y - 1, z)

cells = set()
for x in range(1050, 1072):
    for z in range(984, 1006):
        for y in range(10, 27):
            if stand(x, y, z):
                cells.add((x, y, z))
print('arena standable cells', len(cells))
per = collections.Counter(y for x, y, z in cells)
for y in sorted(per):
    print(y, per[y])
print('ground y11', per[11], 'of 400')
