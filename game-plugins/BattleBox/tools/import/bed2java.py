import numpy as np, sys, collections, json
COL={"white":"white","orange":"orange","magenta":"magenta","light_blue":"light_blue","yellow":"yellow","lime":"lime","pink":"pink","gray":"gray","silver":"light_gray","cyan":"cyan","purple":"purple","blue":"blue","brown":"brown","green":"green","red":"red","black":"black"}
WOOD={"oak","spruce","birch","jungle","acacia","dark_oak"}
DIR4={0:"east",1:"west",2:"south",3:"north"}
FACE6={0:"down",1:"up",2:"north",3:"south",4:"west",5:"east"}
SLAB={ # (bedrock block, type) -> java slab name
 ("stone_slab","stone"):"smooth_stone_slab",("stone_slab","smooth_stone"):"smooth_stone_slab",("stone_slab","sandstone"):"sandstone_slab",
 ("stone_slab","wood"):"petrified_oak_slab",("stone_slab","cobblestone"):"cobblestone_slab",("stone_slab","brick"):"brick_slab",
 ("stone_slab","stone_brick"):"stone_brick_slab",("stone_slab","quartz"):"quartz_slab",("stone_slab","nether_brick"):"nether_brick_slab",
 ("stone_slab2","red_sandstone"):"red_sandstone_slab",("stone_slab2","purpur"):"purpur_slab",("stone_slab2","prismarine_rough"):"prismarine_slab",
 ("stone_slab2","prismarine_dark"):"dark_prismarine_slab",("stone_slab2","prismarine_brick"):"prismarine_brick_slab",
 ("stone_slab2","mossy_cobblestone"):"mossy_cobblestone_slab",("stone_slab2","smooth_sandstone"):"smooth_sandstone_slab",("stone_slab2","red_nether_brick"):"red_nether_brick_slab",
 ("stone_slab3","end_stone_brick"):"end_stone_brick_slab",("stone_slab3","smooth_red_sandstone"):"smooth_red_sandstone_slab",("stone_slab3","polished_andesite"):"polished_andesite_slab",
 ("stone_slab3","andesite"):"andesite_slab",("stone_slab3","diorite"):"diorite_slab",("stone_slab3","polished_diorite"):"polished_diorite_slab",("stone_slab3","granite"):"granite_slab",("stone_slab3","polished_granite"):"polished_granite_slab",
 ("stone_slab4","mossy_stone_brick"):"mossy_stone_brick_slab",("stone_slab4","smooth_quartz"):"smooth_quartz_slab",("stone_slab4","stone"):"stone_slab",
 ("stone_slab4","cut_sandstone"):"cut_sandstone_slab",("stone_slab4","cut_red_sandstone"):"cut_red_sandstone_slab"}
STONE={"stone":"stone","granite":"granite","granite_smooth":"polished_granite","diorite":"diorite","diorite_smooth":"polished_diorite","andesite":"andesite","andesite_smooth":"polished_andesite"}
STAIRS_SIMPLE={"normal_stone_stairs":"cobblestone_stairs","stone_stairs":"cobblestone_stairs","stone_brick_stairs":"stone_brick_stairs","brick_stairs":"brick_stairs","sandstone_stairs":"sandstone_stairs","quartz_stairs":"quartz_stairs","nether_brick_stairs":"nether_brick_stairs","red_nether_brick_stairs":"red_nether_brick_stairs"}
STRIP_PREFIX=("command_block","chain_command_block","repeating_command_block","structure_block","structure_void","jigsaw","allow","deny","border_block")
MACHINE={"redstone_wire","unpowered_repeater","powered_repeater","unpowered_comparator","powered_comparator","redstone_torch","unlit_redstone_torch","observer","piston","sticky_piston","pistonArmCollision","stickyPistonArmCollision","movingBlock","lever","tripwire","trip_wire","tripWire","daylight_detector","daylight_detector_inverted","hopper","dropper","dispenser","note_block","noteblock","target","tnt","chest","trapped_chest","barrel","frame","glow_frame","skull","standing_banner","wall_banner","bell","lectern","jukebox","portal","end_portal","end_gateway","mob_spawner","sculk_sensor"}
def is_machine(n):
    return n in MACHINE or n.endswith(("_button","_pressure_plate","_standing_sign","_wall_sign","_hanging_sign")) or n in ("standing_sign","wall_sign","heavy_weighted_pressure_plate","light_weighted_pressure_plate","stone_pressure_plate","wooden_pressure_plate")
def st(s):
    d={}
    for kv in s.split(","):
        if kv:
            k,_,v=kv.partition("="); d[k]=v
    return d
def props(**kw): return "["+",".join(f"{k}={v}" for k,v in kw.items())+"]"
def pillar(axis): return props(axis=axis)
def conv(p):
    """returns (java_state or None, status) status: ok|strip|unmapped"""
    name,_,s=p.partition("|"); n=name.replace("minecraft:",""); d=st(s)
    if n=="air": return None,"air"
    if n.startswith(STRIP_PREFIX) or is_machine(n):
        jn={"unpowered_repeater":"repeater","powered_repeater":"repeater","unpowered_comparator":"comparator","powered_comparator":"comparator","stickyPistonArmCollision":"piston_head","pistonArmCollision":"piston_head","movingBlock":"moving_piston","frame":"item_frame_block","glow_frame":"item_frame_block","unlit_redstone_torch":"redstone_torch","wooden_button":"oak_button","wooden_pressure_plate":"oak_pressure_plate","standing_banner":"white_banner","wall_banner":"white_wall_banner","skull":"skeleton_skull","noteblock":"note_block","trip_wire":"tripwire","tripWire":"tripwire","standing_sign":"oak_sign","wall_sign":"oak_wall_sign","mob_spawner":"spawner","portal":"nether_portal","birch_standing_sign":"birch_sign"}.get(n,n)
        return "minecraft:"+jn,"strip"
    if n=="water" or n=="flowing_water": 
        dep=int(d.get("liquid_depth","0")); return "minecraft:water"+props(level=min(dep,15)),"ok"
    if n in("lava","flowing_lava"):
        dep=int(d.get("liquid_depth","0")); return "minecraft:lava"+props(level=min(dep,15)),"ok"
    if n in("dirt",): return {"normal":"minecraft:dirt","coarse":"minecraft:coarse_dirt"}.get(d.get("dirt_type","normal"),"minecraft:dirt"),"ok"
    if n=="grass": return "minecraft:grass_block","ok"
    if n=="bedrock": return "minecraft:bedrock","ok"
    if n=="stone": return "minecraft:"+STONE.get(d.get("stone_type","stone"),"stone"),"ok"
    if n in("concrete","wool","carpet","stained_glass","stained_glass_pane","concretePowder","stained_hardened_clay"):
        c=COL[d["color"]]
        jn={"concrete":"concrete","wool":"wool","carpet":"carpet","stained_glass":"stained_glass","stained_glass_pane":"stained_glass_pane","concretePowder":"concrete_powder","stained_hardened_clay":"terracotta"}[n]
        return f"minecraft:{c}_{jn}","ok"
    if n.endswith("_glazed_terracotta"):
        f=FACE6.get(int(d.get("facing_direction","2")),"north")
        if f in("up","down"): f="north"
        return f"minecraft:{n.replace('silver','light_gray')}"+props(facing=f),"ok"
    if n=="planks": return f"minecraft:{d['wood_type']}_planks","ok"
    if n in("log","log2"):
        t=d.get("old_log_type") or d.get("new_log_type"); return f"minecraft:{t}_log"+pillar(d.get("pillar_axis","y")),"ok"
    if n.startswith("stripped_") and n.endswith("_log"): return f"minecraft:{n}"+pillar(d.get("pillar_axis","y")),"ok"
    if n.startswith("stripped_") and n.endswith("_wood"): return f"minecraft:{n}"+pillar(d.get("pillar_axis","y")),"ok"
    if n=="wood": return f"minecraft:{d.get('wood_type','oak')}_wood"+pillar(d.get("pillar_axis","y")),"ok"
    if n in("leaves","leaves2"):
        t=d.get("old_leaf_type") or d.get("new_leaf_type"); return f"minecraft:{t}_leaves"+props(distance=1,persistent="true",waterlogged="false"),"ok"
    if n in("wooden_slab","double_wooden_slab"):
        t="double" if n.startswith("double") else ("top" if d.get("top_slot_bit")=="1" else "bottom"); return f"minecraft:{d['wood_type']}_slab"+props(type=t,waterlogged="false"),"ok"
    m=None
    base=n[len("double_"):] if n.startswith("double_") else n
    if base in("stone_slab","stone_slab2","stone_slab3","stone_slab4"):
        key=d.get("stone_slab_type") or d.get("stone_slab_type_2") or d.get("stone_slab_type_3") or d.get("stone_slab_type_4")
        j=SLAB.get((base,key))
        if j is None: return None,"unmapped"
        t="double" if n.startswith("double_") else ("top" if d.get("top_slot_bit")=="1" else "bottom")
        return f"minecraft:{j}"+props(type=t,waterlogged="false"),"ok"
    if n.endswith("_stairs"):
        j=STAIRS_SIMPLE.get(n, n)
        facing=DIR4[int(d.get("weirdo_direction","0"))]; half="top" if d.get("upside_down_bit")=="1" else "bottom"
        return f"minecraft:{j}"+props(facing=facing,half=half,shape="straight",waterlogged="false"),"ok"
    if n.endswith("_trapdoor"):
        direction=int(d.get("direction","0")); facing={0:"east",1:"west",2:"south",3:"north"}[direction]
        return f"minecraft:{n}"+props(facing=facing,half="top" if d.get("upside_down_bit")=="1" else "bottom",open="true" if d.get("open_bit")=="1" else "false",powered="false",waterlogged="false"),"ok"
    if n=="quartz_block":
        ct=d.get("chisel_type","default"); ax=d.get("pillar_axis","y")
        if ct=="default": return "minecraft:quartz_block","ok"
        if ct=="chiseled": return "minecraft:chiseled_quartz_block","ok"
        if ct=="lines": return "minecraft:quartz_pillar"+pillar(ax),"ok"
        if ct=="smooth": return "minecraft:smooth_quartz","ok"
    if n=="bone_block": return "minecraft:bone_block"+pillar(d.get("pillar_axis","y")),"ok"
    if n=="chain": return "minecraft:chain"+props(axis=d.get("pillar_axis","y"),waterlogged="false"),"ok"
    if n=="nether_brick": return "minecraft:nether_bricks","ok"
    if n=="red_nether_brick": return "minecraft:red_nether_bricks","ok"
    if n=="seaLantern": return "minecraft:sea_lantern","ok"
    if n=="waterlily": return "minecraft:lily_pad","ok"
    if n=="tallgrass": return {"tall":"minecraft:short_grass","fern":"minecraft:fern"}.get(d.get("tall_grass_type","tall"),"minecraft:short_grass"),"ok"
    if n=="red_flower": return "minecraft:"+{"poppy":"poppy","orchid":"blue_orchid","allium":"allium","houstonia":"azure_bluet","tulip_red":"red_tulip","tulip_orange":"orange_tulip","tulip_white":"white_tulip","tulip_pink":"pink_tulip","oxeye":"oxeye_daisy","cornflower":"cornflower","lily_of_the_valley":"lily_of_the_valley"}.get(d.get("flower_type","poppy"),"poppy"),"ok"
    if n=="yellow_flower": return "minecraft:dandelion","ok"
    if n=="barrier": return "minecraft:barrier[waterlogged=false]","ok"
    if n in("iron_bars","glass","gold_block","iron_block","lapis_block","shroomlight","beacon","crying_obsidian","slime","redstone_block","redstone_lamp","lit_redstone_lamp","glowstone","obsidian","sea_lantern","warped_planks","crimson_planks"):
        return {"lit_redstone_lamp":"minecraft:redstone_lamp"}.get(n,f"minecraft:{n}")+("[lit=false]" if n in("redstone_lamp","lit_redstone_lamp") else ""),"ok"
    if n=="iron_bars": return "minecraft:iron_bars","ok"
    if n=="light_block": return "minecraft:light"+props(level=d.get("block_light_level","15"),waterlogged="false"),"ok"
    if n=="lantern" or n=="soul_lantern": return f"minecraft:{n}"+props(hanging="true" if d.get("hanging")=="1" else "false",waterlogged="false"),"ok"
    if n=="prismarine": return {"default":"minecraft:prismarine","dark":"minecraft:dark_prismarine","bricks":"minecraft:prismarine_bricks"}.get(d.get("prismarine_block_type","default")),"ok"
    if n=="sand": return {"normal":"minecraft:sand","red":"minecraft:red_sand"}.get(d.get("sand_type","normal")),"ok"
    if n in("weeping_vines","twisting_vines"):
        return ("minecraft:weeping_vines" if n.startswith("weep") else "minecraft:twisting_vines")+props(age=min(25,int(d.get(n+"_age","0")))),"ok"
    if n=="cobblestone_wall":
        t=d.get("wall_block_type","cobblestone"); j={"cobblestone":"cobblestone_wall","mossy_cobblestone":"mossy_cobblestone_wall"}.get(t,t+"_wall")
        def w(k): return {"none":"none","short":"low","tall":"tall"}[d.get("wall_connection_type_"+k,"none")]
        return f"minecraft:{j}"+props(east=w("east"),north=w("north"),south=w("south"),up="true" if d.get("wall_post_bit")=="1" else "false",waterlogged="false",west=w("west")),"ok"
    if n.endswith("_wall") and "wall_connection_type_east" in d:
        def w(k): return {"none":"none","short":"low","tall":"tall"}[d.get("wall_connection_type_"+k,"none")]
        return f"minecraft:{n}"+props(east=w("east"),north=w("north"),south=w("south"),up="true" if d.get("wall_post_bit")=="1" else "false",waterlogged="false",west=w("west")),"ok"
    if n=="ladder":
        return "minecraft:ladder"+props(facing=FACE6.get(int(d["facing_direction"]),"north"),waterlogged="false"),"ok"
    if n=="cauldron": return "minecraft:water_cauldron"+props(level=max(1,int(d.get("fill_level","6"))//2)) if d.get("fill_level","0")!="0" and d.get("cauldron_liquid")=="water" else "minecraft:cauldron","ok"
    if n=="campfire": return "minecraft:campfire","ok"
    if n=="anvil": return "minecraft:anvil"+props(facing=["south","west","north","east"][int(d.get("direction","0"))%4]),"ok"
    if n=="coral_block": return None,"unmapped"
    return None,"unmapped"
if __name__=="__main__":
    d=np.load(sys.argv[1],allow_pickle=True)
    X,Y,Z,P,pal=d["X"],d["Y"],d["Z"],d["P"],list(d["pal"])
    cnt=collections.Counter(P.tolist())
    jp=[];jmap={};status=collections.defaultdict(collections.Counter)
    newidx=np.full(len(pal),-1,dtype=np.int32)
    for i,p in enumerate(pal):
        j,s=conv(p)
        nm=p.partition("|")[0]
        status[s][nm]+=cnt.get(i,0)
        if j is None: continue
        if j not in jmap: jmap[j]=len(jp); jp.append(j)
        newidx[i]=jmap[j]
    keep=newidx[P]>=0
    # Bedrock keeps water of waterlogged blocks in a second storage layer: fold it into waterlogged=true on layer 0
    if "L" in d.files:
        L=d["L"]
        pos0={(int(x),int(y),int(z)):i for i,(x,y,z,l) in enumerate(zip(X,Y,Z,L)) if l==0}
        drop=np.zeros(len(P),dtype=bool); folded=0
        newP=newidx.copy()
        rowp=newidx[P].copy()
        for i in np.nonzero(L>0)[0]:
            drop[i]=True
            j=pos0.get((int(X[i]),int(Y[i]),int(Z[i])))
            if j is None or rowp[j]<0: drop[i]=False; continue
            s=jp[rowp[j]]
            if "waterlogged=false" in s:
                s2=s.replace("waterlogged=false","waterlogged=true")
                if s2 not in jmap: jmap[s2]=len(jp); jp.append(s2)
                rowp[j]=jmap[s2]; folded+=1
        print("waterlogged layer-1 entries folded:",folded,"dropped:",int(drop.sum()))
        keep &= ~drop
        P=P.copy(); newidx_rows=rowp
        np.savez_compressed(sys.argv[2],X=X[keep],Y=Y[keep],Z=Z[keep],P=newidx_rows[keep],pal=np.array(jp,dtype=object))
        json.dump({k:dict(v) for k,v in status.items()},open(sys.argv[2]+".status.json","w"),indent=0)
        print("kept",keep.sum(),"of",len(X)); sys.exit(0)
    np.savez_compressed(sys.argv[2],X=X[keep],Y=Y[keep],Z=Z[keep],P=newidx[P][keep],pal=np.array(jp,dtype=object))
    json.dump({k:dict(v) for k,v in status.items()},open(sys.argv[2]+".status.json","w"),indent=0)
    print("kept",keep.sum(),"of",len(X))
    print("UNMAPPED",dict(status["unmapped"]))
    print("STRIP",dict(status["strip"]))

