import csv, os, re
ROOT=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation"
print("== files ==")
for root,dirs,files in os.walk(ROOT):
    if "_scratch" in root: continue
    rel=os.path.relpath(root,ROOT)
    print(rel, len(files))
print("\n== blocks ==")
rows=list(csv.DictReader(open(os.path.join(ROOT,"data","blocks.csv"),encoding="utf-8-sig")))
print("rows",len(rows),"total",sum(int(r["count"]) for r in rows),"share",sum(float(r["share_percent"]) for r in rows))
print("top",[(r["block"],r["count"]) for r in rows[:5]])
print("\n== entities ==")
erows=list(csv.DictReader(open(os.path.join(ROOT,"data","entities.csv"),encoding="utf-8-sig")))
print("rows",len(erows),"kinds",{k:sum(1 for r in erows if r["kind"]==k) for k in set(r["kind"] for r in erows)})
print("command rows",sum(1 for r in erows if "command" in r["detail"]))
print("\n== command blocks ==")
crows=list(csv.DictReader(open(os.path.join(ROOT,"data","command_blocks.csv"),encoding="utf-8-sig")))
print(len(crows),[(r["x"],r["y"],r["z"],r["exact_command"]) for r in crows])
print("\n== image links ==")
missing=[]
for md in ["README.md","REFERENCE_IMAGES.md"]:
    txt=open(os.path.join(ROOT,md),encoding="utf-8").read()
    for m in re.findall(r"\((renders/[^)]+|frames/[^)]+)\)",txt):
        if not os.path.exists(os.path.join(ROOT,m)): missing.append((md,m))
    for m in re.findall(r"`(renders/[^`]+|frames/[^`]+)`",txt):
        if not os.path.exists(os.path.join(ROOT,m)): missing.append((md,m))
print("missing",missing)
print("\n== render sizes ==")
from PIL import Image
for f in sorted(os.listdir(os.path.join(ROOT,"renders"))):
    if f.endswith(".png"):
        im=Image.open(os.path.join(ROOT,"renders",f))
        print(f,im.size)
