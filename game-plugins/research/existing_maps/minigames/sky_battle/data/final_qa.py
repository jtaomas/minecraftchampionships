import csv,os,re,collections
print("=== containers ===")
c=list(csv.DictReader(open("data/containers.csv",encoding="utf-8")))
print("rows",len(c),"loot",sum(r["loot_container"]=="yes" for r in c),"empty",sum(r["empty"]=="yes" for r in c),"stacks",sum(int(r["item_stacks"]) for r in c))
print("blocks",dict(collections.Counter(r["block"] for r in c)))
print("source exists",os.path.exists("data/../../../_qa/data/minigames_container_by_arena.csv"))
print("=== alias ===")
a=list(csv.DictReader(open("data/commands.csv",encoding="utf-8"))); b=list(csv.DictReader(open("data/command_blocks.csv",encoding="utf-8")))
print(len(a),len(b),a==b)
print("=== statements ===")
stmt="19 container block entities = 13 loot containers (93 items) + 5 campfires + 1 lectern; all non-loot are empty."
for p in ["README.md","STATUS.md","data/containers.md"]:
    s=open(p,encoding="utf-8").read(); print(p,stmt in s,"ambiguous13",s.count("13 containers"))
print("=== images ===")
s=open("REFERENCE_IMAGES.md",encoding="utf-8").read(); paths=re.findall(r"`(frames/[^`]+\.png)`",s)
print("cited",len(paths),"exist",sum(os.path.exists(p) for p in paths),"disk",len([f for f in os.listdir("frames") if f.endswith(".png")]))
print("=== encoding ===")
bad=[]
for root,dirs,files in os.walk('.'):
    if root.startswith('.\\renders'): continue
    for f in files:
        if f.endswith(('.md','.csv','.txt')):
            p=os.path.join(root,f); b=open(p,'rb').read(3)
            if b==b'\xef\xbb\xbf': bad.append(('BOM',p))
            t=open(p,encoding='utf-8').read()
            if any(ch in t for ch in '\u00c3\u00e2\u00c2\ufeff'): bad.append(('moji',p))
print("bad",bad if bad else "none")
