import json, collections
base=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data"
ents=json.load(open(base+r"\entities.json",encoding="utf-8"))
print("entities",len(ents))
for e in ents:
    print(e.get("id"), e.get("Pos"), "UUID", e.get("UUID"), {k:v for k,v in e.items() if k not in ("Pos","UUID","id","Motion","Rotation","_cx","_cz","_region","TileX","TileY","TileZ")} )
print()
be=json.load(open(base+r"\block_entities.json",encoding="utf-8"))
bn=[b for b in be if b["id"]=="minecraft:banner"]
print("banners",len(bn))
print("banner keys", sorted(bn[0].keys()))
for b in bn[:8]: print(b)
print()
sh=[b for b in be if b["id"]=="minecraft:shulker_box"]
print("shulker keys", sorted(sh[0].keys()))
print("shulkers with anything",[b for b in sh if len(b)>7][:5])
print()
print("campfires",[b for b in be if b["id"]=="minecraft:campfire"])
print("blast",[b for b in be if b["id"]=="minecraft:blast_furnace"])
