import csv
rows = list(csv.DictReader(open('data/pt_commands.csv', encoding='utf-8')))
for i, r in enumerate(rows):
    print(f"{i+1:3d} {r['x']:>4} {r['y']:>2} {r['z']:>4} {r['kind'][:12]:12} a{r['auto']} c{r['condition_met']}  {r['command']}")
