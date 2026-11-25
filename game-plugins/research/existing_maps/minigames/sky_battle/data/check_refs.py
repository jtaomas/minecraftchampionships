import re,os
from PIL import Image
s=open("REFERENCE_IMAGES.md",encoding="utf-8").read()
paths=re.findall(r"`(frames/[^`]+\.png)`",s)
print("cited",len(paths))
for p in paths:
    ok=os.path.exists(p)
    dim=Image.open(p).size if ok else None
    print(("OK " if ok else "MISSING "),p,dim)
allf=sorted(os.listdir("frames"))
print("frames on disk",len(allf),"uncited",[f for f in allf if "frames/"+f not in paths])
