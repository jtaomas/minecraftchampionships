#!/usr/bin/env python3
import json, re
D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
sig = json.load(open(D + r"\signs.json", encoding="utf-8"))

def flat(js):
    out = []
    for m in js:
        try:
            o = json.loads(m)
            out.append(o.get("text", "") if isinstance(o, dict) else str(o))
        except Exception:
            out.append(str(m))
    return " / ".join(x for x in out if x.strip())

for s in sorted(sig, key=lambda r: (-r["y"], r["z"], r["x"])):
    f = flat(s["front"])
    b = flat(s["back"])
    print("%6d %5d %6d %-7s%s | %s%s" % (s["x"], s["y"], s["z"], s["color"], "*" if s["glow"] else " ", f, ("  BACK: " + b) if b else ""))
