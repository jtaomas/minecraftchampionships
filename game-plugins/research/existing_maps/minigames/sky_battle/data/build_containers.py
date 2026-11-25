import csv
qa=list(csv.DictReader(open(r"..\..\_qa\data\minigames_container_by_arena.csv",encoding="utf-8")))
rows=[r for r in qa if r["arena"]=="MG-SB-01"]
print("QA rows",len(rows))
idmap={
 (1965,24,0):"SB-C01",(2035,24,0):"SB-C02",(2000,24,35):"SB-C03",(2000,24,-35):"SB-C04",
 (1974,26,-26):"SB-C05",(1974,26,26):"SB-C06",(2026,26,-26):"SB-C07",(2026,26,26):"SB-C08",
 (1974,14,-26):"SB-C09",(1974,14,26):"SB-C10",(2026,14,-26):"SB-C11",(2026,14,26):"SB-C12",
 (2000,29,0):"SB-C13",
 (1974,29,-26):"SB-E01",(1974,29,26):"SB-E02",(2000,63,0):"SB-E03",(2026,29,-26):"SB-E04",(2026,29,26):"SB-E05",
 (2000,77,-1):"SB-E06",
}
roles={
 "SB-C01":"Red team spawn shulker", "SB-C02":"Green team spawn shulker", "SB-C03":"Blue team spawn shulker", "SB-C04":"Yellow team spawn shulker",
 "SB-C05":"Orange balloon crate chest", "SB-C06":"Cyan balloon crate chest", "SB-C07":"Lime balloon crate chest", "SB-C08":"Purple balloon crate chest",
 "SB-C09":"Orange balloon bottom barrel", "SB-C10":"Cyan balloon bottom barrel", "SB-C11":"Lime balloon bottom barrel", "SB-C12":"Purple balloon bottom barrel",
 "SB-C13":"Mid platform barrel",
 "SB-E01":"Orange balloon campfire", "SB-E02":"Cyan balloon campfire", "SB-E03":"Sky-lobby stem campfire", "SB-E04":"Lime balloon campfire", "SB-E05":"Purple balloon campfire",
 "SB-E06":"Sky-lobby lectern",
}
out=[]
for r in rows:
    x,y,z=int(r["x"]),int(r["y"]),int(r["z"])
    cid=idmap.get((x,y,z))
    if not cid:
        print("UNMAPPED",x,y,z,r["block"]); continue
    block=r["block"].replace("minecraft:","")
    be=block
    if block=="shulker_box": block="white_shulker_box"; be="shulker_box"
    if block=="campfire": be="campfire"
    if block=="lectern": be="lectern"
    stacks=int(r["items"]) if r["items"] else 0
    loot="yes" if stacks>0 else "no"
    empty="no" if stacks>0 else "yes"
    out.append(dict(id=cid,block=block,block_entity_type="minecraft:"+be,x=x,y=y,z=z,loot_container=loot,empty=empty,item_stacks=stacks,contents=r["contents"] if r["contents"] else "empty",role=roles[cid],source="../../_qa/data/minigames_container_by_arena.csv",confidence="H"))
for r in out: print(r["id"],r["block"],r["x"],r["y"],r["z"],r["loot_container"],r["empty"],r["item_stacks"])
with open("data/containers.csv","w",newline="",encoding="utf-8") as f:
    w=csv.DictWriter(f,fieldnames=["id","block","block_entity_type","x","y","z","loot_container","empty","item_stacks","contents","role","source","confidence"])
    w.writeheader(); w.writerows(out)
print("wrote",len(out))
