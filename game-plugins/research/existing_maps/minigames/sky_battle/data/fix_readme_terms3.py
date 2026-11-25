p="README.md"
s=open(p,encoding="utf-8").read()
s=s.replace("13 `fill \u2026 destroy`","13 `fill <loot container> destroy`")
open(p,"w",encoding="utf-8").write(s)
print("ok")
