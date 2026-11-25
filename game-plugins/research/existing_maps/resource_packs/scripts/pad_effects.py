import csv, collections, re
rows = [r for r in csv.DictReader(open(r'C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs\data\command_asset_refs.csv', encoding='utf-8')) if r['map'] == 'clouds2']
by = collections.defaultdict(list)
pat = re.compile(r'if block ~ (~-?\d+) ~ (minecraft:[a-z_]+) run (.+)$')
for r in rows:
    m = pat.search(r['command'])
    if not m:
        continue
    dy, block, action = m.groups()
    action = re.sub(r'^(effect give|effect clear|item replace entity|playsound|setworldspawn).*', lambda mm: mm.group(0), action)
    by[(block, dy)].append(action)
print('block | dy | n | actions')
for (block, dy), acts in sorted(by.items()):
    c = collections.Counter()
    for a in acts:
        if a.startswith('effect give'): c['effect ' + a.split()[3]] += 1
        elif a.startswith('effect clear'): c['clear ' + a.split()[2]] += 1
        elif a.startswith('item replace'): c['item ' + a.split()[-2]] += 1
        elif a.startswith('playsound'): c['sound ' + a.split()[1]] += 1
        elif a.startswith('setworldspawn'): c['setworldspawn'] += 1
        else: c[a[:40]] += 1
    print(block, dy, len(acts), dict(c))
