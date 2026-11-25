import json
d=json.load(open(r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data\block_entities.json",encoding="utf-8"))
for x in d:
    if x["id"]=="minecraft:command_block" and x.get("auto")==0:
        print(x["x"],x["y"],x["z"],"last",x.get("LastExecution"),"succ",x.get("SuccessCount"),"cmd",x.get("Command"))
print("--- chain ---")
for x in d:
    if x["id"]=="minecraft:chain_command_block":
        print(x["x"],x["y"],x["z"],"last",x.get("LastExecution"),"succ",x.get("SuccessCount"),"cmd",x.get("Command"))
