import csv,re,collections
rows=[r for r in csv.DictReader(open('data/commands_raw.csv')) if r['cluster']=='CB-14']
loot=[r for r in rows if r['command'].startswith('/item ')]
pat=re.compile(r'/item replace block (-?\d+) (-?\d+) (-?\d+) container\.(\d+) with (.+)')
chests=collections.defaultdict(dict)
cmdcount=collections.Counter()
for r in loot:
    m=pat.match(r['command'])
    if not m: print('NOMATCH',r['command']); continue
    x,y,z,slot,item=m.groups(); chests[(int(x),int(y),int(z))][int(slot)]=item
    cmdcount[(int(x),int(y),int(z))]+=1
print('loot cmds',len(loot),'chests',len(chests))
for c in sorted(chests, key=lambda t:(-t[1],t[0],t[2])):
    print('CHEST %s slots=%d cmds=%d' % (c,len(chests[c]),cmdcount[c]))
    for s in sorted(chests[c]): print('   slot %2d: %s' % (s,chests[c][s]))
