for p in ["README.md","STATUS.md"]:
    s=open(p,encoding="utf-8").read()
    s=s.replace("A6's current `README.md` records Sky Battle as **13 containers** (4 white shulkers, 5 barrels, 4 chests)",
                "A6's current `README.md` records Sky Battle as **13 pre-placed containers** (4 white shulkers, 5 barrels, 4 chests)")
    open(p,"w",encoding="utf-8").write(s)
print("ok")
