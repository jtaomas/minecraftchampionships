import csv,os
files=["data/blocks.csv","data/blocks_by_region.csv","data/chests.csv","data/loot.csv","data/commands.csv","data/layout.csv","data/signs.csv","data/entities.csv"]
for f in files:
    rows=list(csv.DictReader(open(f,encoding="utf-8")))
    print(f"{f:30s} rows={len(rows):5d} cols={len(rows[0].keys()) if rows else 0}")
print("\nTop-level:")
for f in ["README.md","REFERENCE_IMAGES.md","STATUS.md"]:
    print(f, os.path.getsize(f))
print("\nData files:")
for f in sorted(os.listdir("data")):
    print(" ",f, os.path.getsize("data/"+f))
print("\nFrames:",len(os.listdir("frames")))
print("Renders:",len(os.listdir("renders")))
