import csv, collections
rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
X0, X1, Z0, Z1 = 1048, 1071, 978, 1010
solid = set()
for r in rows:
    solid.add((int(r['x']), int(r['y']), int(r['z'])))

PASSABLE = {'barrier'}  # barrier is solid, but mark separate

def standable(x, y, z):
    return (x, y, z) not in solid and (x, y - 1, z) in solid and (x, y + 1, z) not in solid

for y in range(10, 27):
    print(f'--- standable at y={y} ---')
    print('     ' + ''.join(str(x % 10) for x in range(X0, X1 + 1)))
    for z in range(Z0, Z1 + 1):
        line = ''
        for x in range(X0, X1 + 1):
            if (x, y, z) in solid:
                line += '#'
            elif standable(x, y, z):
                line += '='
            else:
                line += '.'
        print(f'z{z:3d} {line}')
