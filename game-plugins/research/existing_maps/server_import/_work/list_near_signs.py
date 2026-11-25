import json, re, math, sys
from pathlib import Path

s = json.load(open(Path(__file__).parent / "scan.json", encoding="utf-8"))
for w in ["mcc_clouds2", "mcc_citymarathon", "mcc_spacerace", "mcc_yetisetgo",
          "mcc_decisiondome", "mcc_minigames", "mcc_tgttos_badlands",
          "mcc_tgttos_industry", "mcc_tgttos_pits"]:
    d = s[w]
    sp = d["level"]["spawn"]
    print("=" * 15, w, "spawn", sp)
    out = []
    for sg in d.get("signs", []):
        msgs = []
        ft = sg.get("front_text", {})
        if isinstance(ft, dict):
            for m in ft.get("messages", []):
                msgs.append(m)
        for i in range(1, 5):
            if sg.get(f"text{i}"):
                msgs.append(sg[f"text{i}"])
        txt = " | ".join(str(m) for m in msgs)
        plain = re.sub(r'[{}\"\[\]]', "", txt)
        dist = math.dist([sg["x"], sg["y"], sg["z"]], sp)
        out.append((dist, sg["x"], sg["y"], sg["z"], plain[:160]))
    out.sort()
    for dist, x, y, z, t in out[:10]:
        print(f"  d={dist:7.1f} ({x},{y},{z}) {t}")
