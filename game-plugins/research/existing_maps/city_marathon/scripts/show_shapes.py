import json, collections
p = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data\block_entities.json"
d = json.load(open(p, encoding="utf-8"))
for kind in ["minecraft:command_block", "minecraft:sign", "minecraft:shulker_box", "minecraft:barrel", "minecraft:banner", "minecraft:campfire", "minecraft:blast_furnace"]:
    xs = [x for x in d if x.get("id") == kind]
    if not xs: continue
    print(kind, len(xs), "bbox", min(x["x"] for x in xs), min(x["y"] for x in xs), min(x["z"] for x in xs), "->", max(x["x"] for x in xs), max(x["y"] for x in xs), max(x["z"] for x in xs))
    print("  keys", sorted(xs[0].keys()))
print()
# command block raw sample
c = [x for x in d if x.get("id") == "minecraft:command_block"][0]
for k, v in c.items():
    print(k, "=", repr(v)[:300])
print()
# sign sample
s = [x for x in d if x.get("id") == "minecraft:sign"][0]
for k, v in s.items():
    print(k, "=", repr(v)[:500])
print()
# shulker sample
sh = [x for x in d if x.get("id") == "minecraft:shulker_box"]
for x in sh[:2]:
    print(json.dumps(x, indent=1)[:3000])
