import csv
p = r'C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs\data\missing_refs.csv'
rows = list(csv.DictReader(open(p, encoding='utf-8')))
for r in rows:
    if r['ref'] == 'sounds.json':
        r['impact'] = "The pack's sound index is rejected; all six acerace.* events fail to register (vanilla events remain if the loader keeps them)."
        r['confidence'] = 'H (parse) / M (effect)'
with open(p, 'w', newline='', encoding='utf-8') as f:
    w = csv.DictWriter(f, fieldnames=rows[0].keys())
    w.writeheader()
    w.writerows(rows)
print('patched')
