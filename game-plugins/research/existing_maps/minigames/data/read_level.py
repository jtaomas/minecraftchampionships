import nbtlib
p = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3\level.dat"
f = nbtlib.load(p)
d = f["Data"]
print("DataVersion", d.get("DataVersion"))
for k in ["LevelName", "SpawnX", "SpawnY", "SpawnZ", "SpawnAngle", "GameType", "Difficulty",
          "hardcore", "initialized", "Time", "DayTime", "LastPlayed", "BorderCenterX", "BorderCenterZ",
          "BorderSize", "BorderSizeLerpTarget", "allowCommands", "WasModded"]:
    if k in d:
        print(k, d.get(k))
print("--- GameRules ---")
for k, v in d.get("GameRules", {}).items():
    print(k, v)
print("--- DataPacks ---")
print(d.get("DataPacks"))
