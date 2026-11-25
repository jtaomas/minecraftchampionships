import csv, os

base = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs"
checks = {
    "inventory.csv": 168,
    "asset_map.csv": 108,
    "sounds.csv": 15,
    "item_mapping.csv": 5,
    "missing_refs.csv": 19,
    "block_usage.csv": None,
    "custom_items.csv": 2,
    "containers.csv": 387,
    "command_asset_refs.csv": 455,
    "sound_refs.csv": 44,
    "audio.csv": 13,
}
for name, expected in checks.items():
    p = os.path.join(base, "data", name)
    rows = list(csv.DictReader(open(p, encoding="utf-8")))
    print(f"{name:28} rows={len(rows):4}" + (f" expected={expected}" if expected else ""))
    if expected and len(rows) != expected:
        print("   !! MISMATCH")
    # detect ragged rows
    for i, r in enumerate(rows[:0]):
        pass

# package-level files
for d in ["packs", "textures", "frames", "data", "scripts"]:
    p = os.path.join(base, d)
    n = sum(len(fs) for _, _, fs in os.walk(p))
    print(f"{d:10} files={n}")

# key file sizes
for f in ["README.md", "STATUS.md", "REFERENCE_IMAGES.md", "data/compatibility.md", "data/licensing.md",
          "data/map_asset_matrix.md"]:
    p = os.path.join(base, f)
    print(f"{f:30} {os.path.getsize(p):>7} bytes")

# asset_map status counts
rows = list(csv.DictReader(open(os.path.join(base, "data", "asset_map.csv"), encoding="utf-8")))
from collections import Counter
print("status", Counter(r["status"] for r in rows))
print("types", Counter(r["asset_type"] for r in rows))
print("packs", Counter(r["pack"] for r in rows))
