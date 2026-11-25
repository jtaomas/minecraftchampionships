import csv, collections
rows=list(csv.DictReader(open(r'C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs\data\inventory.csv',encoding='utf-8')))
for pack in ['clouds2','space_race','yeti_set_go']:
    sub=[r for r in rows if r['pack']==pack]
    print('###',pack,len(sub),'files',sum(int(r['bytes']) for r in sub),'bytes')
    for r in sub:
        if r['kind'] in ('texture','pack_icon'):
            print('  %sx%s %-5s %8s %s' % (r['width'],r['height'],r['mode'],r['bytes'],r['path']))
