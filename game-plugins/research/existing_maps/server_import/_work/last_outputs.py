import json
from pathlib import Path

s = json.load(open(Path(__file__).parent / "scan.json", encoding="utf-8"))
for w in ["mcc_clouds2", "mcc_citymarathon", "mcc_spacerace", "mcc_yetisetgo", "mcc_decisiondome", "mcc_minigames"]:
    print("=" * 20, w)
    for c in s[w]["command_blocks"]:
        lo = c.get("last_output", "")
        if lo and str(lo).strip() not in ("", '""'):
            print(f"({c['x']},{c['y']},{c['z']}) {c.get('block')} lastExec={c.get('last_execution')} out={str(lo)[:300]}")
