import csv, gzip, sys

rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
# cutaway: arena box only, remove south wall (z=1005) and east wall (x=1071)
keep = []
for r in rows:
    x, y, z = int(r['x']), int(r['y']), int(r['z'])
    if not (1050 <= x <= 1071 and 984 <= z <= 1005 and 9 <= y <= 27):
        continue
    if z == 1005 or x == 1071:
        continue
    keep.append((x, y, z, r['block']))

x0 = min(k[0] for k in keep); y0 = min(k[1] for k in keep); z0 = min(k[2] for k in keep)
x1 = max(k[0] for k in keep); y1 = max(k[1] for k in keep); z1 = max(k[2] for k in keep)
out = 'renders/pt_cutaway.mcmdump.gz'
with gzip.open(out, 'wt', encoding='utf-8') as f:
    f.write(f'MCMDUMP 1 {x1-x0+1} {y1-y0+1} {z1-z0+1}\n')
    for x, y, z, b in keep:
        f.write(f'{x-x0} {y-y0} {z-z0} {b}\n')
print(out, len(keep), x0, y0, z0, '->', x1, y1, z1)
