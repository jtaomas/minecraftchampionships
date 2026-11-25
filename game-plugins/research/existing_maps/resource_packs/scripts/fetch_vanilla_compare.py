#!/usr/bin/env python3
"""Fetch only the vanilla comparison files needed from Mojang's public asset indexes.

Downloads into data/vanilla_compare/ (research reference only; never shipped).
"""
import json
import os
import urllib.request

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(HERE, "data", "vanilla_compare")
os.makedirs(OUT, exist_ok=True)
MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"


def get(url):
    with urllib.request.urlopen(url, timeout=60) as r:
        return r.read()


def main():
    man = json.loads(get(MANIFEST))
    versions = {v["id"]: v["url"] for v in man["versions"]}
    for ver in ("1.19.2", "1.20.1", "1.21.1"):
        meta = json.loads(get(versions[ver]))
        idx = json.loads(get(meta["assetIndex"]["url"]))
        objs = idx["objects"]
        wanted = [
            "minecraft/lang/en_us.json",
            "minecraft/sounds.json",
            "minecraft/sounds/block/bell/bell_use01.ogg",
            "minecraft/sounds/block/candle/extinguish1.ogg",
            "minecraft/sounds/block/cherrywood_trapdoor/toggle1.ogg",
            "minecraft/sounds/block/cherry_wood_trapdoor/toggle1.ogg",
            "minecraft/sounds/records/far.ogg",
            "minecraft/sounds/block/azalea/break1.ogg",
            "minecraft/sounds/block/brewing_stand/brew1.ogg",
            "minecraft/sounds/block/cave_vines/break1.ogg",
            "minecraft/sounds/block/chain/break1.ogg",
            "minecraft/sounds/block/chain/step1.ogg",
            "minecraft/textures/block/yellow_glazed_terracotta.png",
            "minecraft/textures/block/magenta_glazed_terracotta.png",
            "minecraft/textures/block/sea_lantern.png",
            "minecraft/textures/item/rotten_flesh.png",
            "minecraft/textures/block/blackstone.png",
            "minecraft/textures/block/acacia_planks.png",
            "minecraft/textures/block/glowstone.png",
        ]
        dest = os.path.join(OUT, ver)
        os.makedirs(dest, exist_ok=True)
        print("##", ver)
        for name in wanted:
            if name not in objs:
                print("  absent", name)
                continue
            h = objs[name]["hash"]
            url = f"https://resources.download.minecraft.net/{h[:2]}/{h}"
            path = os.path.join(dest, name.replace("/", "_"))
            if not os.path.exists(path):
                data = get(url)
                with open(path, "wb") as f:
                    f.write(data)
            print("  ", name, objs[name]["size"])


if __name__ == "__main__":
    main()
