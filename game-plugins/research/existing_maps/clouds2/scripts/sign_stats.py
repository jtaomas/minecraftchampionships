#!/usr/bin/env python3
import json
from collections import Counter
D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
sig = json.load(open(D + r"\signs.json", encoding="utf-8"))
print("signs", len(sig))
print("colors", Counter(s["color"] for s in sig))
print("glow", Counter(s["glow"] for s in sig))
print("waxed", Counter(s["waxed"] for s in sig))
raw = json.dumps(sig)
print("clickEvent mentions:", raw.count("clickEvent"), "click_event:", raw.count("click_event"))
print("hoverEvent:", raw.count("hoverEvent"))
# blocks
import gzip
O = (-258, -64, -316)
variants = Counter()
for line in gzip.open(D + r"\clouds2_full.mcmdump.gz", "rt", encoding="utf-8"):
    p = line.split(" ", 3)
    if len(p) < 4 or p[0] == "MCMDUMP":
        continue
    n = p[3].strip().split("[")[0]
    if "sign" in n:
        variants[n] += 1
print("sign blocks", variants)
# distinct texts
texts = Counter()
for s in sig:
    t = " | ".join(x for x in s["front"] if x.strip())
    texts[t] += 1
print("distinct front texts:", len(texts), "of", len(sig))
print("most repeated:", texts.most_common(3))
