import csv, collections
rows = list(csv.DictReader(open(r'C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs\data\inventory.csv', encoding='utf-8')))
by = collections.defaultdict(list)
for r in rows:
    if r['pack'] == 'yeti_zipped_map_dup':
        continue
    by[r['sha256']].append((r['pack'], r['path'], r['bytes']))
print('## files shared byte-for-byte across packs')
for h, items in by.items():
    packs = {i[0] for i in items}
    if len(packs) > 1:
        print(h[:12], items)
print('## Yeti files not identical to a Clouds 2 file')
cl = {r['sha256'] for r in rows if r['pack'] == 'clouds2'}
for r in rows:
    if r['pack'] == 'yeti_set_go' and r['sha256'] not in cl:
        print(r['path'], r['bytes'])
print('## Space Race sea lantern vs Clouds 2')
for r in rows:
    if r['path'].endswith('sea_lantern.png') and r['pack'] in ('clouds2','space_race'):
        print(r['pack'], r['sha256'][:12], r['bytes'])
