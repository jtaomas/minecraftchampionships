import csv,collections,math
rows=list(csv.DictReader(open("data/blocks_mg_sb_01.csv")))
def region(r):
    x,y,z=int(r["x"]),int(r["y"]),int(r["z"]); b=r["block"]
    if "barrier" in b: return "barrier_envelope"
    if y>=57: return "sky_lobby"
    if (1961<=x<=1973 and -6<=z<=6) or (2027<=x<=2039 and -6<=z<=6) or (1994<=x<=2006 and 27<=z<=39) or (1994<=x<=2006 and -39<=z<=-27): return "gift_islands"
    if (1970<=x<=1978 and -30<=z<=-22) or (1970<=x<=1978 and 22<=z<=30) or (2022<=x<=2030 and -30<=z<=-22) or (2022<=x<=2030 and 22<=z<=30): return "balloons"
    d=math.hypot(x-2000,z)
    if d<10: return "mid"
    if d<=22: return "ring_gallery"
    return "other"
use={
"barrier":"invisible envelope: walls x1948/2052, z-52/52, roof y56, lobby cage",
"white_concrete":"gift-box base/ribbon, gallery trim, canopy edge, island foundation",
"stone_bricks":"gallery floor, inner lip, mid plus, fence caps",
"red_concrete":"Red team gift box + Red canopy quadrant",
"yellow_concrete":"Yellow team gift box + Yellow canopy quadrant",
"green_concrete":"Green team gift box + Green canopy quadrant",
"blue_concrete":"Blue team gift box + Blue canopy quadrant",
"red_terracotta":"Red canopy underside / seat band",
"yellow_terracotta":"Yellow canopy underside / seat band",
"green_terracotta":"Green canopy underside / seat band",
"blue_terracotta":"Blue canopy underside / seat band",
"gray_wool":"sky-lobby lantern body and roof pads",
"smooth_quartz":"gift-box ribbon cross and canopy trim",
"oak_fence":"outer colonnade under the canopy rim",
"orange_wool":"NW balloon envelope",
"cyan_wool":"SW balloon envelope",
"lime_wool":"NE balloon envelope",
"purple_wool":"SE balloon envelope",
"cobblestone_wall":"gallery floor edge trim",
"oak_leaves":"hedge/foliage on the outer colonnade",
"oak_trapdoor":"balloon crate + basket details",
"smooth_quartz_stairs":"outer rim stair trim at y25",
"spruce_fence":"balloon crate corners",
"green_glazed_terracotta":"Green gift-box lid pattern",
"red_glazed_terracotta":"Red gift-box lid pattern",
"yellow_glazed_terracotta":"Yellow gift-box lid pattern",
"blue_glazed_terracotta":"Blue gift-box lid pattern",
"chain":"central drop from lobby to mid; balloon hanging chains; mid stem",
"gold_block":"sky-lobby lantern cap/frame",
"stripped_spruce_log":"balloon masts and lobby stem",
"stone_brick_wall":"gallery edge posts",
"spruce_trapdoor":"balloon basket details",
"spruce_fence_gate":"balloon basket rail",
"raw_gold_block":"sky-lobby lantern cap",
"birch_wall_sign":"team-join and control signs",
"iron_block":"four border markers / mineable iron caches",
"stone_brick_stairs":"mid plus / gallery trim",
"red_stained_glass":"sky-lobby team bays",
"yellow_stained_glass":"sky-lobby team bays",
"blue_stained_glass":"sky-lobby team bays",
"green_stained_glass":"sky-lobby team bays",
"polished_blackstone_wall":"central chain collars",
"crafting_table":"team island + mid crafting station",
"barrel":"loot containers (balloon bottoms + mid)",
"spruce_slab":"balloon crate shelf",
"campfire":"balloon/mid smoke plume",
"stone_button":"sky-lobby control buttons",
"white_shulker_box":"team spawn loot containers",
"chest":"balloon crate loot containers",
"light":"invisible light blocks in sky lobby",
"command_block":"sky-lobby start/reset command blocks",
"red_concrete_powder":"Red team join pad in lobby",
"polished_blackstone":"mid chain collar",
"yellow_concrete_powder":"Yellow team join pad in lobby",
"blue_concrete_powder":"Blue team join pad in lobby",
"lectern":"sky-lobby arrival lectern",
"repeating_command_block":"sky-lobby item-cleanup command block",
"green_wool":"single green wool accent in lobby floor",
"green_concrete_powder":"Green team join pad in lobby",
}
cnt=collections.Counter(r["block"].replace("minecraft:","") for r in rows)
total=len(rows)
with open("data/blocks.csv","w",newline="",encoding="utf-8") as f:
    w=csv.writer(f); w.writerow(["block","count","share_pct","group","use","confidence"])
    group=lambda b:("barrier" if "barrier" in b else "team_colour" if any(k in b for k in ["red_","yellow_","green_","blue_"]) else "neutral_stone" if any(k in b for k in ["concrete","quartz","stone","cobble","blackstone"]) else "wood_foliage" if any(k in b for k in ["oak","spruce","birch","leaves"]) else "container_utility")
    for b,c in cnt.most_common():
        w.writerow([b,c,round(100*c/total,3),group(b),use.get(b,""),"H"])
# region matrix
regs=sorted(set(region(r) for r in rows))
mat=collections.defaultdict(collections.Counter)
for r in rows: mat[r["block"].replace("minecraft:","")][region(r)]+=1
with open("data/blocks_by_region.csv","w",newline="",encoding="utf-8") as f:
    w=csv.writer(f); w.writerow(["block","total"]+regs)
    for b,c in cnt.most_common(): w.writerow([b,c]+[mat[b][g] for g in regs])
print("total",total,"types",len(cnt),"regions",regs)
for g in regs: print(g,sum(1 for r in rows if region(r)==g))
