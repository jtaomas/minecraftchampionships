import csv, collections
rows=list(csv.DictReader(open(r'C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs\data\block_usage.csv',encoding='utf-8')))
by=collections.defaultdict(dict)
for r in rows: by[r['block']][r['map']]=int(r['count'])
maps=['clouds2','yeti_set_go','space_race','city_marathon','tgttos_badlands','tgttos_industry','tgttos_pits']
print('block'.ljust(46), ' '.join(m[:6].rjust(6) for m in maps))
for b in sorted(by):
    print(b.ljust(46), ' '.join(str(by[b].get(m,0)).rjust(6) for m in maps))
