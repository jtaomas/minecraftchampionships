import csv
rows=list(csv.DictReader(open(r'C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs\data\sound_refs.csv',encoding='utf-8')))
cmds={(r['map'],r['x'],r['y'],r['z']):r for r in csv.DictReader(open(r'C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs\data\command_asset_refs.csv',encoding='utf-8'))}
for r in rows:
    c=cmds.get((r['map'],r['x'],r['y'],r['z']),{})
    print('---',r['map'],r['x'],r['y'],r['z'],r['sound'])
    print('   ',c.get('command','')[:400])
