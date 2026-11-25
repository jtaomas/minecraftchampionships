import csv
p=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data\containers.csv"
rows=list(csv.DictReader(open(p,encoding="utf-8")))
for r in rows:
    if r["type"]!="shulker_box": continue
    cl=r["cluster"]
    if "-12..-12, 0..20" in cl:
        r["cluster"]="vertical-column (x-12, y0..20, z49)"
        r["inferred_use"]="Vertical stack of 21 empty shulkers at x-12,z49; decorative column / possible elevator marker (M)."
    elif cl.startswith("roof-AC-2") and any(s in cl for s in ["(2..2, 0..0","(2..2, 6..6","(14..14, 0..0","(14..14, 6..6"]):
        r["cluster"]="start-gate post (x2/14, z10)"
        r["inferred_use"]="Empty shulker used as a white start-gate post at x2 or x14, z10 (H)."
w=csv.DictWriter(open(p,"w",newline="",encoding="utf-8"),fieldnames=list(rows[0].keys()))
w.writeheader(); w.writerows(rows)
print("patched")
