import csv
rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
for (x, z) in [(1061, 982), (1060, 982), (1060, 1007), (1061, 1007), (1061, 983), (1060, 983)]:
    print('column', x, z)
    for r in rows:
        if int(r['x']) == x and int(r['z']) == z and 14 <= int(r['y']) <= 21:
            print('  ', r['y'], r['block'])
