import csv
p=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data\blocks.csv"
for i,x in enumerate(csv.DictReader(open(p,encoding="utf-8"))):
    if i>=32: break
    print("| {block} | {count} | {share_percent}% | {category} | {inferred_use} |".format(**x))
