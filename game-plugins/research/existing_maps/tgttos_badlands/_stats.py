import csv
rows = list(csv.DictReader(open('data/blocks.csv', encoding='utf-8')))
tot = sum(int(r['count']) for r in rows)
for r in rows[:32]:
    print('%-34s %7d %6.2f%%  %s' % (r['block_id'], int(r['count']), float(r['share_pct']), r['role']))
print('---')
print('terrain-ish total', sum(int(r['count']) for r in rows if r['role'].startswith('terrain')))
print('build total', sum(int(r['count']) for r in rows if r['role'].startswith('build')))
print('accent total', sum(int(r['count']) for r in rows if r['role'].startswith(('sign', 'accent', 'industrial', 'light', 'fence', 'roof', 'hanging', 'solar'))))
