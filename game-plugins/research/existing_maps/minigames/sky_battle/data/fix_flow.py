p="data/flow.md"
s=open(p,encoding="utf-8").read()
s=s.replace("13 containers, 93 `item replace block` commands. Full tables: `data/chests.csv`, `data/loot.csv`.",
            "13 loot containers (19 container block entities total: 5 campfires + 1 lectern, all empty), 93 `item replace block` commands. Full tables: `data/chests.csv`, `data/loot.csv`, `data/containers.csv`.")
s=s.replace("Refill the 13 containers with the 93 `item replace` commands.",
            "Refill the 13 loot containers with the 93 `item replace` commands.")
open(p,"w",encoding="utf-8").write(s)
print("ok")
