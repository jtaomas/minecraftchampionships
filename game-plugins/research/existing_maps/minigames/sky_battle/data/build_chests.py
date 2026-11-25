import csv,re,collections,math
rows=list(csv.DictReader(open("data/commands_raw.csv")))
def parse_item(s):
    s=s.strip(); nbt=None
    if "{" in s:
        i=s.index("{"); j=s.rfind("}"); nbt=s[i:j+1]; item=s[:i]; rest=s[j+1:].strip()
    else: item=s; rest=""
    count=1
    if rest.strip() and re.match(r"(\d+)$",rest.strip()):
        count=int(rest.strip())
    elif "{" not in s:
        m=re.match(r"(.+?)\s+(\d+)$",s)
        if m:
            item=m.group(1); count=int(m.group(2))
    return item.replace("minecraft:",""),count,nbt
def effect_of(nbt):
    e=[]
    if not nbt: return "-"
    if "CanPlaceOn" in nbt:
        ins=nbt[nbt.index("[")+1:nbt.rindex("]")]; blocks=[b.strip().replace("minecraft:","") for b in ins.split(",") if b.strip()]
        e.append(f"CanPlaceOn {len(blocks)} blocks")
    if "CanDestroy" in nbt:
        ins=nbt[nbt.index("[")+1:nbt.rindex("]")]; blocks=[b.strip().replace("minecraft:","") for b in ins.split(",") if b.strip()]
        e.append("CanDestroy "+",".join(blocks))
    if "Unbreakable" in nbt: e.append("Unbreakable")
    if "Enchantments" in nbt: e.append("Loyalty I")
    m=re.search(r"Potion:'minecraft:([a-z_]+)'",nbt)
    if m: e.append("potion="+m.group(1))
    return "; ".join(e)
byc=collections.defaultdict(list)
for r in rows:
    if not r["command"].startswith("/item replace block "): continue
    m=re.match(r"/item replace block (-?\d+) (-?\d+) (-?\d+) container\.(\d+) with (.+)$",r["command"])
    x,y,z,slot,spec=m.groups(); item,count,nbt=parse_item(spec)
    byc[(int(x),int(y),int(z))].append((int(slot),item,count,nbt))
order=[
 ((1965,24,0),"white_shulker_box","Red team island (west)","Red spawn shulker","island top, next to crafting table"),
 ((2035,24,0),"white_shulker_box","Green team island (east)","Green spawn shulker","island top, next to crafting table"),
 ((2000,24,35),"white_shulker_box","Blue team island (south)","Blue spawn shulker","island top, next to crafting table"),
 ((2000,24,-35),"white_shulker_box","Yellow team island (north)","Yellow spawn shulker","island top, next to crafting table"),
 ((1974,26,-26),"chest","Orange balloon (NW) crate","crate chest inside balloon envelope","break oak trapdoor crate at y26"),
 ((1974,26,26),"chest","Cyan balloon (SW) crate","crate chest inside balloon envelope","break oak trapdoor crate at y26"),
 ((2026,26,-26),"chest","Lime balloon (NE) crate","crate chest inside balloon envelope","break oak trapdoor crate at y26"),
 ((2026,26,26),"chest","Purple balloon (SE) crate","crate chest inside balloon envelope","break oak trapdoor crate at y26"),
 ((1974,14,-26),"barrel","Orange balloon (NW) bottom","hanging barrel under balloon","bridge to y14 below balloon"),
 ((1974,14,26),"barrel","Cyan balloon (SW) bottom","hanging barrel under balloon","bridge to y14 below balloon"),
 ((2026,14,-26),"barrel","Lime balloon (NE) bottom","hanging barrel under balloon","bridge to y14 below balloon"),
 ((2026,14,26),"barrel","Purple balloon (SE) bottom","hanging barrel under balloon","bridge to y14 below balloon"),
 ((2000,29,0),"barrel","Mid platform","mid barrel","bridge to centre plus platform at y28"),
]
chests=[]; key2id={}
for i,(key,typ,place,role,access) in enumerate(order,1):
    items=byc[key]; cid=f"SB-C{i:02d}"; key2id[key]=cid
    summary="; ".join(f"{it}"+(f" x{c}" if c>1 else "")+(f" [{effect_of(nbt)}]" if nbt else "") for _,it,c,nbt in sorted(items))
    chests.append(dict(id=cid,x=key[0],y=key[1],z=key[2],type=typ,place=place,role=role,access=access,dist_from_centre=round(math.hypot(key[0]-2000,key[2]),1),slots_used=len(items),slot_list=",".join(str(s) for s,_,_,_ in sorted(items)),contents=summary,reset="93 item replace commands (CB-14 x949..977); destroyed by x978 chain",confidence="H"))
with open("data/chests.csv","w",newline="",encoding="utf-8") as f:
    w=csv.DictWriter(f,fieldnames=list(chests[0].keys())); w.writeheader(); w.writerows(chests)
loot=[]
for key,items in byc.items():
    c=[c for c in chests if c["id"]==key2id[key]][0]
    for slot,item,count,nbt in sorted(items):
        loot.append(dict(container_id=key2id[key],container_type=c["type"],place=c["place"],x=key[0],y=key[1],z=key[2],slot=slot,item=item,count=count,effect=effect_of(nbt),nbt=nbt or ""))
with open("data/loot.csv","w",newline="",encoding="utf-8") as f:
    w=csv.DictWriter(f,fieldnames=["container_id","container_type","place","x","y","z","slot","item","count","effect","nbt"]); w.writeheader()
    for l in sorted(loot,key=lambda a:(a["container_id"],a["slot"])): w.writerow(l)
print("ok",len(chests),len(loot))
for c in chests: print(c["id"],c["type"],c["place"],c["slots_used"],c["contents"][:120])
