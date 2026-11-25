import json, collections, os
p = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data\block_entities.json"
d = json.load(open(p, encoding="utf-8"))
print("count", len(d))
print(collections.Counter(x.get("id") for x in d))
print("keys", sorted(d[0].keys()))
cmds = [x for x in d if str(x.get("id", "")).startswith("minecraft:command_block") or "Command" in x]
print("commands", len(cmds))
for x in cmds:
    print(x.get("x"), x.get("y"), x.get("z"), x.get("id"), repr(x.get("Command")), "auto", x.get("auto"), "cond", x.get("conditionMet"), "powered", x.get("powered"))
