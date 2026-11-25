#!/usr/bin/env python3
"""Full player inventory / effects dump."""
import nbtlib

W = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Space_Race\Space Race"
lv = nbtlib.load(W + r"\level.dat")
p = lv["Data"]["Player"]
print("Inventory:")
for it in p["Inventory"]:
    print(" ", it)
print("\nActiveEffects:")
for e in p.get("ActiveEffects", []):
    print(" ", e)
print("\nEnderItems:", len(p.get("EnderItems", [])))
print("\nDataPacks:", lv["Data"]["DataPacks"])
print("GameRules:")
for k in lv["Data"]["GameRules"]:
    print("  ", k, "=", lv["Data"]["GameRules"][k])
