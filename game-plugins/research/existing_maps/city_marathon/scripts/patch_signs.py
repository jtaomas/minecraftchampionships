import csv, json
p=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data\signs.csv"
states=json.load(open(r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data\sign_states.json",encoding="utf-8"))
rows=list(csv.DictReader(open(p,encoding="utf-8")))
for r in rows:
    key=f"{r['x']},{r['y']},{r['z']}"
    st=states.get(key,"")
    if "rotation=" in st: r["rotation_facing"]="rotation="+st.split("rotation=")[1].split("]")[0].split(",")[0]
    elif "facing=" in st: r["rotation_facing"]="facing="+st.split("facing=")[1].split("]")[0].split(",")[0]
w=csv.DictWriter(open(p,"w",newline="",encoding="utf-8"),fieldnames=list(rows[0].keys()))
w.writeheader(); w.writerows(rows)
print("patched signs")
