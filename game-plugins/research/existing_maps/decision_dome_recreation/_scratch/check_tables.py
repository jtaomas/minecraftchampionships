import re
s=open("README.md",encoding="utf-8").read().splitlines()
block=[]; start=0
for i,l in enumerate(s+[""]):
    if l.startswith("|"):
        if not block: start=i
        block.append((i+1,l))
    else:
        if block:
            counts={l.count("|") for _,l in block}
            if len(counts)>1:
                print("TABLE MISMATCH at",start+1,"counts",counts)
                for ln,l in block[:4]: print(" ",ln,l[:160])
            block=[]
