import json
base = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data"
sel = json.load(open(base + r"\selected_blocks.json", encoding="utf-8"))
for st in sorted(set(x[3] for x in sel if "carpet" in x[3] or "concrete" in x[3])):
    pts=[x for x in sel if x[3]==st]
    print("==",st,len(pts))
    for p in sorted(pts,key=lambda a:(a[0],a[2],a[1])): print("  ",p[0],p[1],p[2])
print("\n== command block states ==")
for p in sorted([x for x in sel if "command_block" in x[3]], key=lambda a:(a[0],a[2],a[1])):
    print(p[0],p[1],p[2],p[3])
print("\n== buttons/redstone ==")
for p in sorted([x for x in sel if "button" in x[3] or "repeater" in x[3]], key=lambda a:(a[0],a[2],a[1])):
    print(p[0],p[1],p[2],p[3])
