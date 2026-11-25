import csv,os,collections
ok=True
c=list(csv.DictReader(open("data/containers.csv",encoding="utf-8")))
print("containers rows",len(c),"loot",sum(r["loot_container"]=="yes" for r in c),"empty",sum(r["empty"]=="yes" for r in c),"stacks",sum(int(r["item_stacks"]) for r in c))
print("blocks",collections.Counter(r["block"] for r in c))
q={(int(r["x"]),int(r["y"]),int(r["z"]),("white_shulker_box" if r["block"].replace("minecraft:","")=="shulker_box" else r["block"].replace("minecraft:",""))) for r in csv.DictReader(open(r"..\..\_qa\data\minigames_container_by_arena.csv",encoding="utf-8")) if r["arena"]=="MG-SB-01"}
o={(int(r["x"]),int(r["y"]),int(r["z"]),r["block"]) for r in c}
print("QA set match",q==o,len(q),len(o))
a=list(csv.DictReader(open("data/commands.csv",encoding="utf-8")))
b=list(csv.DictReader(open("data/command_blocks.csv",encoding="utf-8")))
print("commands alias",len(a),len(b),a==b)
for p in ["README.md","STATUS.md","data/containers.md"]:
    s=open(p,encoding="utf-8").read()
    print(p,"statement",("19 container block entities = 13 loot containers (93 items) + 5 campfires + 1 lectern; all non-loot are empty." in s))
for p in ["README.md","STATUS.md"]:
    s=open(p,encoding="utf-8").read()
    print(p,"ambiguous 13 containers count",s.count("13 containers"))
