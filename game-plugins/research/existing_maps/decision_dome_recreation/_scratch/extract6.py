s=open("README.md",encoding="utf-8").read().splitlines()
start=[i for i,l in enumerate(s) if l.startswith("## 6.")][0]
end=[i for i,l in enumerate(s) if l.startswith("## 7.")][0]
open("_scratch/section6.md","w",encoding="utf-8").write("\n".join(s[start:end]))
print(start,end)
