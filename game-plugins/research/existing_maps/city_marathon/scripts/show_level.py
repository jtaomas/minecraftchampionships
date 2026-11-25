import json, collections
base = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data"
lev = json.load(open(base + r"\level.json", encoding="utf-8"))
d = lev.get("Data", lev)
for k in ["LevelName", "SpawnX", "SpawnY", "SpawnZ", "SpawnAngle", "GameType", "Difficulty", "hardcore", "DayTime", "Time", "raining", "thundering", "clearWeatherTime", "DataVersion", "Version", "initialized", "allowCommands"]:
    print(k, "=", d.get(k))
print("\n-- gamerules --")
for k, v in sorted(d.get("GameRules", {}).items()):
    print(k, "=", v)
print("\n-- playerdata --")
pd = json.load(open(base + r"\playerdata.json", encoding="utf-8"))
for p in pd:
    print(p.get("_file"), "pos", p.get("Pos"), "dim", p.get("Dimension"), "gm", p.get("playerGameType"), "food", p.get("foodLevel"), "health", p.get("Health"), "inv", len(p.get("Inventory", [])))
    for it in p.get("Inventory", []):
        print("   slot", it.get("Slot"), it.get("id"), it.get("Count"), it.get("tag", {}))
