import json, collections
base = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data"
sel = json.load(open(base + r"\selected_blocks.json", encoding="utf-8"))
be = json.load(open(base + r"\block_entities.json", encoding="utf-8"))

def show(name):
    pts = [(x,y,z,st) for x,y,z,st,_ in sel if st.split("[")[0].endswith(name)]
    print(name, len(pts), "bbox", (min(p[0] for p in pts), min(p[1] for p in pts), min(p[2] for p in pts)), "->", (max(p[0] for p in pts), max(p[1] for p in pts), max(p[2] for p in pts)))
    # print all if <100
    if len(pts) <= 80:
        for p in sorted(pts, key=lambda q:(q[0],q[2],q[1])):
            print("  ", p)
    else:
        print("  x hist", collections.Counter(p[0] for p in pts).most_common(20))
        print("  z hist", collections.Counter(p[2] for p in pts).most_common(20))
        print("  y hist", collections.Counter(p[1] for p in pts).most_common(20))

for n in ["polished_diorite","yellow_glazed_terracotta","orange_wool","lime_wool","red_glazed_terracotta","pink_glazed_terracotta","magenta_glazed_terracotta","yellow_carpet","green_carpet","yellow_concrete","gray_wool"]:
    show(n)
    print()

print("=== banners ===")
bn = [x for x in be if x["id"]=="minecraft:banner"]
print(len(bn), "bbox", min(x["x"] for x in bn), min(x["y"] for x in bn), min(x["z"] for x in bn), max(x["x"] for x in bn), max(x["y"] for x in bn), max(x["z"] for x in bn))
print("x groups", collections.Counter(x["x"] for x in bn).most_common())

print("=== signs ===")
for x in be:
    if x["id"]=="minecraft:sign":
        print(x["x"],x["y"],x["z"], x.get("Color"), x.get("GlowingText"), x.get("Text1"), x.get("Text2"), x.get("Text3"), x.get("Text4"))

print("=== shulker clusters ===")
sh = [x for x in be if x["id"]=="minecraft:shulker_box"]
for x in sorted(sh, key=lambda a:(a["x"],a["z"],a["y"])):
    print(x["x"],x["y"],x["z"])
