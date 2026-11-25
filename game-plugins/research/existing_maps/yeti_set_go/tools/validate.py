import csv, json, gzip
from collections import Counter
def check(p):
    rows = list(csv.DictReader(open(p, encoding='utf-8')))
    print(p, len(rows), 'cols', len(rows[0]) if rows else 0)
    return rows
cb = check('data/command_blocks.csv'); sg = check('data/signs.csv'); ct = check('data/containers.csv')
en = check('data/entities.csv'); cp = check('data/checkpoints.csv'); bl = check('data/blocks.csv')
bs = check('data/block_states.csv'); pc = check('data/pad_clusters.csv'); pm = check('data/pack_mapping.csv')
print('cmd types', Counter(r['block'] for r in cb))
empty = sum(1 for r in sg if all(r['text%d' % i] == '{"text":""}' for i in (1,2,3,4)))
print('signs', len(sg), 'empty', empty)
print('containers items', sum(int(r['slots_used']) for r in ct))
print('pad types', Counter(r['type'] for r in pc))
print('blocks total', sum(int(r['count']) for r in bl), 'states total', sum(int(r['count']) for r in bs))
d = json.load(open('data/scan.json', encoding='utf-8'))
print('chunks', d['chunks'], 'entities', len(d['entities']))
ch = set()
for line in gzip.open('data/yeti_full.mcmdump.gz', 'rt'):
    if line.startswith('MCMDUMP'): continue
    x, y, z, n = line.split(' ', 3); ch.add((int(x) >> 4, int(z) >> 4))
print('nonempty chunks', len(ch))
