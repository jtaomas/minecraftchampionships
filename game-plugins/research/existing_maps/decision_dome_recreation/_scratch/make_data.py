import csv, os, io, zlib, gzip, json, math
from collections import Counter, defaultdict
import nbtlib

ROOT=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation"
WORLD=r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Decision_Dome\Decision Dome - Recreation"
DATA=os.path.join(ROOT,"data")
os.makedirs(DATA,exist_ok=True)

rows=[]
for r in csv.DictReader(open(os.path.join(ROOT,"_scratch","blocks_raw.csv"),encoding="utf-8")):
    rows.append((int(r["x"]),int(r["y"]),int(r["z"]),r["block"].replace("minecraft:","")))
TOTAL=len(rows)
base=Counter(n.split("[")[0] for x,y,z,n in rows)
state=Counter(n for x,y,z,n in rows)

def family(n):
    if "concrete" in n: return "concrete"
    if "terracotta" in n: return "terracotta"
    if "glass" in n: return "glass"
    if "quartz" in n: return "quartz"
    if n.startswith("polished_diorite") or n in ("stone","stone_slab"): return "diorite/stone"
    if "wool" in n: return "wool"
    if "carpet" in n: return "carpet"
    if n=="gold_block": return "gold"
    if n in ("chain",): return "metal"
    if n=="sea_lantern": return "light"
    if "slab" in n or "stairs" in n: return "slabs/stairs"
    if "banner" in n: return "banner"
    if "head" in n: return "head"
    if n in ("barrier",): return "barrier"
    if n in ("command_block","chain_command_block","acacia_pressure_plate"): return "logic"
    if n in ("iron_door","iron_trapdoor"): return "door"
    if n in ("beehive","oak_leaves"): return "plant/apiary"
    if n in ("redstone_block","netherite_block"): return "accent"
    return "other"

USES={
"gray_concrete":("Drum shell (r26), flat base disk y26, pit wall, terrace inner ring, crown truss bands","H"),
"white_stained_glass":("Pod window band, y58-60: 198 per layer x 3 layers, all 10 pods","H"),
"white_concrete":("Terrace outer rim (y68), north platform, gate frames, NW annex floor","H"),
"red_concrete":("Red team pod; north red carpet; gatehouse red panels","H"),
"light_gray_terracotta":("Two drum stripes y33/y52 (160 each) + neutral band on all 10 pod crowns y72 (240)","H"),
"blue_concrete":("Blue team pod; NW annex/west gate trim","H"),
"yellow_concrete":("Yellow team pod; crown top ring y78-80","H"),
"light_blue_concrete":("Light-blue team pod; NW annex roof edge","H"),
"orange_concrete":("Orange team pod; minor trim","H"),
"lime_concrete":("Lime team pod","H"),
"pink_concrete":("Pink team pod","H"),
"purple_concrete":("Purple team pod","H"),
"green_concrete":("Green team pod","H"),
"cyan_concrete":("Cyan team pod","H"),
"smooth_quartz_slab":("Dial wedge plates y58 (184); NW annex roof y73 (79); north/gate trim","H"),
"white_stained_glass_pane":("North platform and NW annex glazing","H"),
"black_concrete":("Gatehouse frames/posts only, y61-72","H"),
"cyan_terracotta":("Cyan pod band (8 layers)","H"),
"light_blue_terracotta":("Light-blue pod band (8 layers)","H"),
"gold_block":("Crown: 8 gold-capped pillars, 3x3 x 4 tall (y69-72)","H"),
"barrier":("Invisible inner safety rail, r24-25, y71 (140 blocks)","H"),
"red_terracotta":("Red pod band + gatehouse band","H"),
"orange_terracotta":("Orange pod band","H"),
"yellow_terracotta":("Yellow pod band + crown accents","H"),
"pink_terracotta":("Pink pod band","H"),
"green_terracotta":("Green pod band","H"),
"blue_terracotta":("Blue pod band","H"),
"lime_terracotta":("Lime pod band","H"),
"purple_terracotta":("Purple pod band","H"),
"quartz_slab":("Tunnel floors y67; crown cross y73-74; gate trim","H"),
"polished_diorite":("Gatehouse pillars/frames (y66-72)","H"),
"lime_stained_glass_pane":("Lime pod crown window band y65/67/68","H"),
"orange_stained_glass_pane":("Orange pod crown window band","H"),
"yellow_stained_glass_pane":("Yellow pod crown window band","H"),
"red_stained_glass_pane":("Red pod crown window band","H"),
"pink_stained_glass_pane":("Pink pod crown window band","H"),
"purple_stained_glass_pane":("Purple pod crown window band","H"),
"green_stained_glass_pane":("Green pod crown window band","H"),
"cyan_stained_glass_pane":("Cyan pod crown window band","H"),
"light_blue_stained_glass_pane":("Light-blue pod crown window band","H"),
"blue_stained_glass_pane":("Blue pod crown window band","H"),
"light_gray_carpet":("NW annex floor y69 (62)","H"),
"chain":("Gatehouse hanging chains (56) + crown suspension (4)","H"),
"polished_andesite_slab":("Outer stepped lip y66 (50)","H"),
"polished_diorite_stairs":("Gatehouse pillar caps/steps y67-72","H"),
"iron_trapdoor":("Gatehouse/NW roof and awning detail (y71-73)","M"),
"stone_slab":("NW annex trim/steps (y69,72)","H"),
"light_gray_stained_glass_pane":("Gatehouse window strips y62-72","H"),
"smooth_red_sandstone_slab":("Crown top ring y78 (36)","H"),
"yellow_wool":("Crown top ring y79 (36)","H"),
"red_nether_brick_slab":("North carpet edging/steps y69-71","H"),
"polished_andesite_stairs":("Outer stepped lip y66 (26)","H"),
"lime_stained_glass":("Lime pod glass band y69","H"),
"orange_stained_glass":("Orange pod glass band y69","H"),
"yellow_stained_glass":("Yellow pod glass band y69","H"),
"red_stained_glass":("Red pod glass band y69","H"),
"pink_stained_glass":("Pink pod glass band y69","H"),
"purple_stained_glass":("Purple pod glass band y69","H"),
"green_stained_glass":("Green pod glass band y69","H"),
"cyan_stained_glass":("Cyan pod glass band y69","H"),
"light_blue_stained_glass":("Light-blue pod glass band y69","H"),
"blue_stained_glass":("Blue pod glass band y69","H"),
"smooth_quartz_stairs":("Tunnel-to-terrace steps y68; NW annex steps y69","H"),
"light_blue_wool":("NW annex roof trim y73/75","H"),
"sea_lantern":("Lighting: 16 on NW annex roof, 2 on north landing (18 total)","H"),
"player_head":("17 trophy heads in NW annex: 9 chicken, 5 zombie, 3 pale/unidentified","H"),
"white_wall_banner":("16 white banners with black patterns, NW annex diagonal wall","H"),
"quartz_stairs":("Crown cross/truss detail y73-74","H"),
"lime_wool":("Lime pod band accents y57/61","H"),
"purple_wool":("Purple pod band accents y57/61","H"),
"gray_carpet":("NW annex floor accent y70","H"),
"gray_wool":("NW annex floor/wall accent","H"),
"netherite_block":("NW annex wall accents (4 posts x 2)","H"),
"redstone_block":("8 red 'jewels' on crown top y82","H"),
"orange_wall_banner":("NW annex banners (4 at y72 + 1 at y70)","H"),
"oak_leaves":("NW annex apiary foliage","H"),
"iron_door":("2x2 SE service door, x19-20 z37-38 y69-70","H"),
"stone":("NW annex detail y69","H"),
"beehive":("NW annex apiary (2)","H"),
"chain_command_block":("Egg/glow station: chain block under the north plate","H"),
"command_block":("Egg/glow station: impulse block under the north plate","H"),
"acacia_pressure_plate":("Only input: plate at (-3,71,-19) on the red carpet","H"),
"yellow_wall_banner":("NW annex banner (y70)","H"),
"green_wall_banner":("NW annex banner (y70)","H"),
"cyan_wall_banner":("NW annex banner (y70)","H"),
}

with open(os.path.join(DATA,"blocks.csv"),"w",newline="",encoding="utf-8-sig") as fh:
    w=csv.writer(fh); w.writerow(["rank","block","count","share_percent","family","use","use_confidence"])
    for i,(n,c) in enumerate(sorted(base.items(),key=lambda kv:-kv[1]),1):
        use,conf=USES.get(n,("Decorative/detail block","M"))
        w.writerow([i,n,c,f"{c/TOTAL*100:.2f}",family(n),use,conf])

with open(os.path.join(DATA,"block_states.csv"),"w",newline="",encoding="utf-8-sig") as fh:
    w=csv.writer(fh); w.writerow(["rank","block_state","count","share_percent"])
    for i,(n,c) in enumerate(sorted(state.items(),key=lambda kv:-kv[1]),1):
        w.writerow([i,n,c,f"{c/TOTAL*100:.3f}"])

# layers
byy=defaultdict(list)
for x,y,z,n in rows: byy[y].append(n.split("[")[0])
with open(os.path.join(DATA,"layers.csv"),"w",newline="",encoding="utf-8-sig") as fh:
    w=csv.writer(fh); w.writerow(["y","block_count","top_materials"])
    for y in sorted(byy):
        c=Counter(byy[y])
        top="; ".join(f"{k}:{v}" for k,v in c.most_common(6))
        w.writerow([y,len(byy[y]),top])

# dimensions
D=[
("World block bounds","x -45..39; y 26..82; z -21..44","blocks","H","85 x 57 x 66 envelope"),
("Drum outer diameter","53","blocks","H","x -29..23, z -10..42; centre (-3,16)"),
("Drum outer radius","26","blocks","H","fitted from the 144-block ring at y40"),
("Drum wall thickness","1","blocks","H","radial line at z=16: x=-29 only"),
("Drum height","43","blocks","H","y26 base to y68 terrace"),
("Base disk","r25, y26, 1 thick","blocks","H","1993 gray concrete at y26"),
("Terrace level","y68","blocks","H","walking surface; gray inner + white outer"),
("Terrace radii / width","r25-28; 4 wide","blocks","H","gray r25-26, white r27-28"),
("Inner barrier rail","r24-25, y71, 140 barriers","blocks","H","invisible; full ring, no outer rail"),
("Pit inner/outer radius","r9.5 to r25","blocks","H","open annulus around the dial"),
("Pit depth","42","blocks","H","terrace y68 to base y26"),
("Dial floor","y58; r9.0 quartz, rim r9.5","blocks","H","10 below terrace, 32 above base"),
("Dial wedges","8 x 45 deg zones; 8 divider directions at 0/45/90/135/180/225/270/315","-","H","cardinal dividers continuous r0-9; diagonal dividers dashed (1-block gaps at r2/r5); smooth quartz plates"),
("Dial rim wall","r9, y53-60","blocks","H","gray concrete ring"),
("Dial visibility","0-17 of 25 sampled points from terrace; 0/25 from N carpet; 9/25 from y100","points","H","raycast, glass/panes/barriers transparent"),
("Crown footprint / height","17 x 17; y69-82 (14 tall)","blocks","H","x -11..5, z 8..24"),
("Crown parts","8 gold pillars (3x3, y69-72); white cross y73-74; yellow ring y78-80; 8 yellow caps y81; 8 redstone jewels y82","-","H","477 blocks total"),
("Pods","10","count","H","36 deg spacing at r15.81-16.00"),
("Pod angles","270, 305, 342, 18, 55, 90, 125, 162, 198, 235","degrees","H","clockwise from N; lime at 270"),
("Pod footprint","5x5 body; 7x7 crown; 3x3 caps","blocks","H","hollow 3x3 shaft"),
("Pod height","51","blocks","H","y27-77; top 9 above terrace"),
("Pod ring radius","15.81-16.00","blocks","H","centres measured from y68 glass rings"),
("Seats","0","count","H","no stairs/slabs used as seats; no pod rooms"),
("Spectator stands","0 tiers; 4-wide standing ring","count","H","terrace ring only"),
("Stairs into pit","0","count","H","no stair/step from terrace y68 to dial y58"),
("Functional steps","22 stair blocks (18 tunnel + 4 annex) + 3 stepped dais rises","blocks","H","1-block rises at y67-70; no stair into the pit"),
("West gatehouse","10 x 12 x 10 (x -45..-36, z11..22, y64-73)","blocks","H","334 blocks; mirror at x30..39"),
("North platform","13 x 14 x 6 (x -9..3, z -21..-8, y66-71)","blocks","H","red carpet head 11 x 5 at y70"),
("NW annex","11 x 15 x 12 (x -30..-20, z -15..-1, y64-75)","blocks","H","glass roof y73; 470 blocks"),
("SE service door","8 x 8 frame; 2x2 iron doors at x19-20 z37-38 y69-70","blocks","H","decorative; solid drum behind"),
("Command blocks","2 (1 impulse, 1 chain)","count","H","overworld only; -3,68,-19 and -3,69,-19"),
("Tile entities","45","count","H","24 banners, 17 skulls, 2 command blocks, 2 beehives"),
("Entities","2 armor stands","count","H","(-19.5,69,-7.5); gold + green leather chestplates"),
("Signs","0","count","H","block palette scan"),
("Light sources","18 sea lanterns","count","H","no glowstone/lantern/torch/froglight"),
("Copper blocks","0","count","H","palette scan; no copper family"),
("Chunks","1,344","count","H","318+339+327+360 across 4 region files"),
("Dimensions with blocks","Overworld only","-","H","DIM-1/DIM1 have no region folder"),
("Block budget","20,054","blocks","H","85 base types, 186 states"),
("Dome / roof","none","-","H","open drum, no glass dome or ceiling above y82"),
("Outer safety rail","none","-","H","only the inner barrier at r24-25 y71"),
("Pod gap at 90 deg","none","-","H","10 pods at 36 deg, including the south 90 deg pod"),
("Wedge axis facing","none","-","H","E/W and N/S axes are dividers; no wedge centred on an entrance"),
("World spawn","(0, 4, 0)","coords","H","22 blocks below the base disk, outside the drum"),
("Game rules","Creative; DayTime 6000 fixed; no damage/mobs; keepInventory","-","H","showcase config, not arena config"),
("Datapacks","11 active VanillaTweaks packs (+2 bundle entries)","count","H","no dome-specific functions/logic"),
("Banners","24 (16 patterned white + 8 coloured/plain)","count","H","all in the NW annex"),
("Heads","17 (9 chicken, 5 zombie, 3 pale)","count","H/M","3 textures; mounted in the NW annex"),
]
with open(os.path.join(DATA,"dimensions.csv"),"w",newline="",encoding="utf-8-sig") as fh:
    w=csv.writer(fh); w.writerow(["metric","value","unit","evidence","note"])
    for r in D: w.writerow(r)

# command blocks
with open(os.path.join(DATA,"command_blocks.csv"),"w",newline="",encoding="utf-8-sig") as fh:
    w=csv.writer(fh)
    w.writerow(["x","y","z","block","type","auto","conditional","facing","exact_command","trigger","effect","last_execution","success_count","powered_now","1_21_risk","evidence"])
    w.writerow([-3,69,-19,"command_block","impulse",0,"false","down","give @p egg","Plate at (-3,71,-19) on the red carpet y70; impulse at y69 directly below; no dust/repeater/lever in the palette. NBT LastExecution=286717 and SuccessCount=1 prove it has fired at least once; the exact plate-to-block coupling is not visually proven (M).","Nearest player receives 1 egg.",286717,1,0,"None: give @p egg is valid 1.21 syntax (minecraft:egg optional).","H (NBT + block state); trigger M"])
    w.writerow([-3,68,-19,"chain_command_block","chain",1,"false","down","effect give @p minecraft:glowing 999999 255 true","Chain block directly below the impulse, facing down (back faces up). NBT LastExecution=286717 and SuccessCount=1; auto=1 is odd for a chain block but is not a 1.21 failure.","Nearest player gets Glowing for 999999 s at amplifier 255, particles hidden.",286717,1,0,"Low: effect give syntax and glowing id are valid 1.21; 999999 s and amplifier 255 are legal.","H (NBT + block state)"])

# entities / tile entities
def read_chunks(path):
    with open(path,"rb") as fh: data=fh.read()
    if len(data)<8192: return
    for idx in range(1024):
        e=data[idx*4:idx*4+4]
        if len(e)<4: continue
        off=(e[0]<<16)|(e[1]<<8)|e[2]
        if not off: continue
        pos=off*4096
        if pos+5>len(data): continue
        ln=int.from_bytes(data[pos:pos+4],"big"); comp=data[pos+4]
        raw=data[pos+5:pos+4+ln]
        try:
            if comp==2: raw=zlib.decompress(raw)
            elif comp==1: raw=gzip.decompress(raw)
            elif comp!=3: continue
            yield nbtlib.File.parse(io.BytesIO(raw))
        except Exception: pass

def to_py(v):
    if isinstance(v,dict): return {str(k):to_py(x) for k,x in v.items()}
    if isinstance(v,(list,tuple)): return [to_py(x) for x in v]
    try:
        if hasattr(v,"item"): return v.item()
    except Exception: pass
    return str(v)

tes=[]; stands=[]
for fn in sorted(os.listdir(os.path.join(WORLD,"region"))):
    if not fn.endswith(".mca"): continue
    p=os.path.join(WORLD,"region",fn)
    if os.path.getsize(p)==0: continue
    for f in read_chunks(p):
        lvl=f.get("Level")
        if lvl is None: continue
        for te in lvl.get("TileEntities",[]):
            tes.append(to_py(te))
for fn in sorted(os.listdir(os.path.join(WORLD,"entities"))):
    if not fn.endswith(".mca"): continue
    p=os.path.join(WORLD,"entities",fn)
    if os.path.getsize(p)==0: continue
    for f in read_chunks(p):
        for e in f.get("Entities",[]):
            stands.append(to_py(e))

SKIN_NAMES={
"15582ed512a8cea9b17ffc31ca12b86f2abccf83a622fb7d15b5f171f51bb64":"zombie/green player skin",
"b26e38d8c1da8d451d7194f3a7fad90a141dfe6165b6667f7a8cba0919139":"pale/white head (chicken variant?)",
"1638469a599ceef7207537603248a9ab11ff591fd378bea4735b346a7fae893":"chicken head (white, yellow beak, red wattle)",
}
def head_kind(te):
    try:
        val=te["SkullOwner"]["Properties"]["textures"][0]["Value"]
        import base64
        d=json.loads(base64.b64decode(val))
        url=d["textures"]["SKIN"]["url"]
        h=url.rsplit("/",1)[-1]
        return SKIN_NAMES.get(h,"unknown skin "+h)
    except Exception:
        return "unknown"
import re as _re
def num(v):
    m=_re.search(r"-?\d+(?:\.\d+)?",str(v))
    return int(float(m.group(0))) if m else 0
def banner_desc(te):
    pats=te.get("Patterns") or []
    if not pats: return "plain/no patterns"
    return ",".join(f"{p.get('Pattern')}/{num(p.get('Color'))}" for p in pats)

with open(os.path.join(DATA,"entities.csv"),"w",newline="",encoding="utf-8-sig") as fh:
    w=csv.writer(fh); w.writerow(["kind","id","x","y","z","detail","evidence"])
    for te in sorted([t for t in tes if t.get("id")!="minecraft:command_block"],key=lambda t:(t.get("x",0),t.get("z",0),t.get("y",0))):
        tid=te.get("id","")
        if tid=="minecraft:banner":
            det=f"banner; patterns={banner_desc(te)}"
        elif tid=="minecraft:skull":
            det="head; "+head_kind(te)
        elif tid=="minecraft:beehive":
            det="beehive; apiary decoration"
        else:
            det="tile entity"
        w.writerow(["tile_entity",tid,num(te.get("x")),num(te.get("y")),num(te.get("z")),det,"H (NBT)"])
    for te in sorted([t for t in tes if t.get("id")=="minecraft:command_block"],key=lambda t:(num(t.get("y")),)):
        yy=num(te.get("y")); bt="chain_command_block" if yy==68 else "command_block"
        w.writerow(["tile_entity","minecraft:command_block",num(te.get("x")),yy,num(te.get("z")),f"block={bt}; command={te.get('Command')}","H (NBT)"])

    for e in stands:
        armor=e.get("ArmorItems",[])
        det="armor stand; "
        try:
            item=armor[2]
            det+=f"chest={item.get('id')} tag={item.get('tag')}; "
        except Exception: pass
        det+=f"rotation={e.get('Rotation')}; pos={e.get('Pos')}"
        import re
        nums=[float(v) for v in re.findall(r"-?\d+(?:\.\d+)?",str(e.get("Pos")))]
        px,py,pz=(nums+[0,0,0])[:3]
        w.writerow(["entity","minecraft:armor_stand",round(px,2),round(py,2),round(pz,2),det,"H (entity NBT)"])

with open(os.path.join(DATA,"signs.csv"),"w",newline="",encoding="utf-8-sig") as fh:
    w=csv.writer(fh); w.writerow(["status","x","y","z","front","back","colour","glow","click_event"])
    w.writerow(["NO SIGN BLOCKS","","","","Block palette contains 0 standing/wall signs; 24 banners are not signs.","","","",""])

with open(os.path.join(DATA,"checkpoints.csv"),"w",newline="",encoding="utf-8-sig") as fh:
    w=csv.writer(fh); w.writerow(["status","x","y","z","gate_axis","width","grants_or_clears","note"])
    w.writerow(["NONE","","","","","","","No checkpoint, gate, scoreboard, spawn or finish logic in the world. Only 2 command blocks (egg/glow) exist."])

# sightlines
B={(x,y,z):n.split("[")[0] for x,y,z,n in rows}
TR={"white_stained_glass","white_stained_glass_pane","light_gray_stained_glass_pane","barrier","light_gray_carpet","gray_carpet","chain"}
for c in ["lime","orange","yellow","red","pink","purple","green","cyan","light_blue","blue"]:
    TR.add(c+"_stained_glass"); TR.add(c+"_stained_glass_pane")
def solid(x,y,z):
    n=B.get((x,y,z)); return n is not None and n not in TR
def ray(a,b,step=0.03):
    dx=b[0]-a[0]; dy=b[1]-a[1]; dz=b[2]-a[2]
    d=math.sqrt(dx*dx+dy*dy+dz*dz); n=int(d/step)
    if n<2: return True
    for i in range(1,n-1):
        t=i/n
        if solid(math.floor(a[0]+dx*t),math.floor(a[1]+dy*t),math.floor(a[2]+dz*t)):
            return False
    return True
CX,CZ=-3,16
targets=[]
for k in range(8):
    a=math.radians(22.5+45*k)
    for r in (3,6,7.8):
        targets.append((f"w{k+1}r{r}",CX+r*math.cos(a),59.2,CZ+r*math.sin(a)))
targets.append(("centre",CX,59.2,CZ))
with open(os.path.join(DATA,"sightlines.csv"),"w",newline="",encoding="utf-8-sig") as fh:
    w=csv.writer(fh); w.writerow(["view","angle_deg","eye_x","eye_y","eye_z","visible_points","total_points","percent","visible_targets"])
    for ad in range(0,360,15):
        a=math.radians(ad); eye=(CX+26.5*math.cos(a),69.62,CZ+26.5*math.sin(a))
        vis=[n for n,x,y,z in targets if ray(eye,(x,y,z))]
        w.writerow(["terrace_ring",ad,f"{eye[0]:.2f}",f"{eye[1]:.2f}",f"{eye[2]:.2f}",len(vis),len(targets),f"{len(vis)/len(targets)*100:.0f}",";".join(vis)])
    for label,eye in [("north_red_carpet",(CX,71.62,-19)),("west_gate_mouth",(-28,69.62,16)),("above_y100",(0,100,0))]:
        vis=[n for n,x,y,z in targets if ray(eye,(x,y,z))]
        w.writerow([label,"",f"{eye[0]}",f"{eye[1]}",f"{eye[2]}",len(vis),len(targets),f"{len(vis)/len(targets)*100:.0f}",";".join(vis)])
print("wrote data files; base types",len(base),"states",len(state),"total",TOTAL)
print("command blocks",sum(1 for t in tes if t.get("id")=="minecraft:command_block"),"TEs",len(tes),"stands",len(stands))
