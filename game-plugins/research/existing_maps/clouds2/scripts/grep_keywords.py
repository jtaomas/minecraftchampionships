#!/usr/bin/env python3
import json, re
D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
sig = json.load(open(D + r"\signs.json", encoding="utf-8"))
cb = json.load(open(D + r"\command_blocks.json", encoding="utf-8"))
kw = re.compile(r"start|finish|check|lap|reset|course|spawn|gate|win|goal|time|count|teleport|tp |title|sound|music|elytra|trident|boost|kill|void|spectat|press|button|plate|lever", re.I)

def flat(js):
    out = []
    for m in js:
        try:
            o = json.loads(m); out.append(o.get("text", ""))
        except Exception: out.append(str(m))
    return " ".join(x for x in out if x.strip())

print("== SIGN matches ==")
for s in sorted(sig, key=lambda r: (r["z"], r["x"])):
    t = flat(s["front"]) + " " + flat(s["back"])
    if kw.search(t):
        print("%5d %4d %5d  %s" % (s["x"], s["y"], s["z"], t))

print("\n== COMMAND matches ==")
for c in cb:
    if kw.search(c["command"]):
        print("%5d %4d %5d  %s" % (c["x"], c["y"], c["z"], c["command"]))
