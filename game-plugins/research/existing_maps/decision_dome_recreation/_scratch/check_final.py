import csv, os, re
R=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation"
read=lambda p: open(os.path.join(R,p),encoding="utf-8").read()
print("== required files ==")
for p in ["README.md","REFERENCE_IMAGES.md","STATUS.md","data/blocks.csv","data/command_blocks.csv","data/entities.csv","data/palette.md","data/dimensions.csv","data/route.md","data/signs.csv","data/checkpoints.csv"]:
    print(p, os.path.exists(os.path.join(R,p)))
print("== csv parse ==")
for p in ["data/blocks.csv","data/block_states.csv","data/layers.csv","data/dimensions.csv","data/command_blocks.csv","data/entities.csv","data/sightlines.csv","data/dimension_scan.csv","data/signs.csv","data/checkpoints.csv"]:
    rows=list(csv.DictReader(open(os.path.join(R,p),encoding="utf-8-sig")))
    print(p, len(rows), "cols", len(rows[0]) if rows else 0)
print("== README checks ==")
t=read("README.md")
for s in ["2 command blocks","give @p egg","effect give @p minecraft:glowing","0 seats","0 stands","no dome","Nocturne Orrery","Adopt later","Do not adopt","sightline","20,054","53 wide","42 deep","r16"]:
    print(s, s.lower() in t.lower())
print("== bad strings ==")
for s in ["13 generic","6 / 8","y81-82","2-block jump","south 90"]:
    print(s, s in t)
print("== images cited (README) ==")
for m in re.findall(r"!\[[^\]]*\]\(([^)]+)\)",t):
    print(m, os.path.exists(os.path.join(R,m)))
print("== folder counts ==")
for d in ["renders","frames","data"]:
    print(d, len(os.listdir(os.path.join(R,d))))
