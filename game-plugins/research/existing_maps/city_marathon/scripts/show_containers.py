import json, collections
p = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data\block_entities.json"
d = json.load(open(p, encoding="utf-8"))
sh = [x for x in d if x.get("id") == "minecraft:shulker_box"]
print("shulkers", len(sh), "with Items", sum(1 for x in sh if "Items" in x))
print("positions sample", [(x["x"],x["y"],x["z"]) for x in sh[:20]])
print("y distribution", collections.Counter(x["y"] for x in sh))
print("x range dist", collections.Counter(x["x"] for x in sh).most_common(10))
# group clusters by proximity
pts = [(x["x"],x["y"],x["z"]) for x in sh]
print("unique", len(set(pts)))
print("min/max", min(pts), max(pts))

def itemname(it):
    return it.get("id"), it.get("Count"), it.get("tag", {})

for kind in ["minecraft:barrel", "minecraft:campfire", "minecraft:blast_furnace"]:
    xs = [x for x in d if x.get("id") == kind]
    print("\n==", kind, len(xs))
    for x in xs:
        print(x["x"], x["y"], x["z"], "Items:", json.dumps(x.get("Items", []))[:600])
