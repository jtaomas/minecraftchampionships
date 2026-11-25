p="README.md"
s=open(p,encoding="utf-8").read()
s=s.replace("13 `fill <container> destroy` + `kill @e[type=item]`. It **does not recreate the containers**",
            "13 `fill <loot container> destroy` + `kill @e[type=item]`. It **does not recreate the 13 loot containers**")
s=s.replace("| 6 | **Bug reset destroys containers** | 13 `fill . destroy` and no recreate; the next round has no loot.",
            "| 6 | **Bug reset destroys loot containers** | 13 `fill <loot container> destroy` and no recreate; the next round has no loot.")
open(p,"w",encoding="utf-8").write(s)
print("ok")
