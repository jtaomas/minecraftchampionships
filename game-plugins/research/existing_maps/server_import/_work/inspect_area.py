import json, math, re, sys
from pathlib import Path
import block_lookup as bl
import scan_worlds as sw

s = json.load(open(Path(__file__).parent / "scan.json", encoding="utf-8"))

INTERACTIVE = ("button", "lever", "pressure_plate", "tripwire_hook", "daylight_detector", "observer", "redstone_block", "repeater", "comparator")


def explain(world, radius=48):
    d = s[world]
    sx, sy, sz = d["level"]["spawn"]
    print("=" * 20, world, "spawn", (sx, sy, sz), "radius", radius)
    cbs = {c["coordinate"] if "coordinate" in c else (c["x"], c["y"], c["z"]): c for c in d["command_blocks"]}
    for c in d["command_blocks"]:
        dist = math.dist((c["x"], c["y"], c["z"]), (sx, sy, sz))
        if dist <= radius:
            print(f"  CB d={dist:5.1f} ({c['x']},{c['y']},{c['z']}) {c.get('block')} auto={c.get('auto')} powered={c.get('powered')}")
            print(f"     {c['command']}")
    print("  signs within radius:")
    for sg in d.get("signs", []):
        dist = math.dist((sg["x"], sg["y"], sg["z"]), (sx, sy, sz))
        if dist <= radius:
            msgs = []
            ft = sg.get("front_text", {})
            if isinstance(ft, dict):
                msgs += list(ft.get("messages", []))
            bt = sg.get("back_text", {})
            if isinstance(bt, dict):
                msgs += list(bt.get("messages", []))
            print(f"  SIGN d={dist:5.1f} ({sg['x']},{sg['y']},{sg['z']}) {' | '.join(str(m) for m in msgs)}")
    print("  interactive blocks within radius:")
    found = 0
    for x in range(sx - radius, sx + radius + 1):
        for z in range(sz - radius, sz + radius + 1):
            for y in range(sy - 20, sy + 20 + 1):
                name, props = bl.get_block(world, "overworld", x, y, z)
                if any(k in name for k in INTERACTIVE):
                    dist = math.dist((x, y, z), (sx, sy, sz))
                    print(f"    d={dist:5.1f} ({x},{y},{z}) {name} {props}")
                    found += 1
                    if found > 120:
                        return
    print("  done")


if __name__ == "__main__":
    explain(sys.argv[1], int(sys.argv[2]) if len(sys.argv) > 2 else 48)
