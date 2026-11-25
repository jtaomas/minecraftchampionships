import json
from pathlib import Path

s = json.load(open(Path(__file__).parent / "scan.json", encoding="utf-8"))
for w in ["mcc_clouds2", "mcc_citymarathon", "mcc_spacerace", "mcc_yetisetgo"]:
    print("=" * 20, w)
    for c in s[w]["command_blocks"]:
        if not c.get("auto"):
            print(f"({c['x']},{c['y']},{c['z']}) {c.get('block')} :: {c['command'][:170]}")
