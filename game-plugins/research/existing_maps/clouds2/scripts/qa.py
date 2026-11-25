#!/usr/bin/env python3
"""Final QA of the A1 deliverable folder."""
import csv, os, glob

D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2"

def rows(p):
    with open(os.path.join(D, p), encoding="utf-8-sig") as fh:
        return list(csv.DictReader(fh))

cb = rows("data/command_blocks.csv")
sg = rows("data/signs.csv")
ct = rows("data/containers.csv")
bl = rows("data/blocks.csv")
cp = rows("data/checkpoints.csv")
lg = rows("data/route_legs.csv")
pm = rows("data/pack_mapping.csv")
en = rows("data/entities.csv")
fl = rows("data/command_failures.csv")

print("command_blocks", len(cb), "unique xyz", len({(r["x"], r["y"], r["z"]) for r in cb}))
print("signs", len(sg), "unique", len({(r["x"], r["y"], r["z"]) for r in sg}))
print("containers", len(ct), "unique", len({(r["x"], r["y"], r["z"]) for r in ct}))
print("blocks", len(bl), "sum", sum(int(r["count"]) for r in bl))
print("checkpoints", len(cp), "route_legs", len(lg), "pack_mapping", len(pm), "entities", len(en))
print("failures", len(fl), "item-nbt", sum(1 for r in fl if "{" in r["command"]),
      "setblock", sum(1 for r in fl if r["command"].strip() == "setblock 5 4 22"))
print("empty fields in cb:", sum(1 for r in cb for k in ("x","y","z","block_type","command","trigger","effect","reset","multiplayer","syntax_1_21") if not str(r[k]).strip()))
print("bank counts:", {b: sum(1 for r in cb if r["bank"] == b) for b in "ABCDEF"})
print("block types:", len({r["block_type"] for r in cb}))
print("sign colors:", sorted({r["color"] for r in sg}))
print("container contents:", sorted({r["contents"] for r in ct}))
print("shulker variants:", sorted({r["block"] for r in ct}))
print()
print("files:")
for p in sorted(glob.glob(D + r"\**\*", recursive=True)):
    if os.path.isfile(p):
        print(f"  {os.path.getsize(p):9d}  {os.path.relpath(p, D)}")
