import os,re
R=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation"
missing=[]
for fn in ["README.md","REFERENCE_IMAGES.md"]:
    txt=open(os.path.join(R,fn),encoding="utf-8").read()
    for m in re.findall(r"!\[[^\]]*\]\(([^)]+)\)",txt):
        if not os.path.exists(os.path.join(R,m)): missing.append((fn,m))
    for m in re.findall(r"`(?:(?:\.\.\./)?(?:renders|frames)/[^`]+)`",txt):
        mm=m.replace(".../","")
        if "*" in mm or "\u2013" in mm: continue
        if not os.path.exists(os.path.join(R,mm)): missing.append((fn,mm))
print("missing",missing)
from PIL import Image
for f in ["renders/plan_labelled.png","renders/top_raw.png","renders/iso_exterior_a0_labelled.png","renders/iso_exterior_a2_labelled.png","renders/iso_interior_labelled.png","renders/elevation_labelled.png","renders/dial_plan_labelled.png","renders/contact_sheet.png"]:
    im=Image.open(os.path.join(R,f)); print(f,im.size)
