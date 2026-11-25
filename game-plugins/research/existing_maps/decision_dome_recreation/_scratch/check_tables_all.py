import os
R=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation"
for fn in ["README.md","REFERENCE_IMAGES.md","STATUS.md","data/palette.md","data/route.md"]:
    p=os.path.join(R,fn); s=open(p,encoding="utf-8").read().splitlines()
    block=[]; start=0; bad=0
    for i,l in enumerate(s+[""]):
        if l.startswith("|"):
            if not block: start=i
            block.append((i+1,l))
        else:
            if block:
                counts={l.count("|") for _,l in block}
                if len(counts)>1:
                    bad+=1; print(fn,"mismatch",start+1,counts)
                block=[]
    print(fn,"ok" if bad==0 else f"{bad} bad")
