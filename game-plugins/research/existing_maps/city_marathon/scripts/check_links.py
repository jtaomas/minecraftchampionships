import re, os
base=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon"
for fn in ["README.md","REFERENCE_IMAGES.md","STATUS.md"]:
    p=os.path.join(base,fn); s=open(p,encoding="utf-8").read()
    links=re.findall(r"\]\(([^)]+)\)",s)
    for l in links:
        if l.startswith("http"): continue
        q=os.path.join(base,l)
        if not os.path.exists(q):
            print("MISSING",fn,l)
print("link check done")
