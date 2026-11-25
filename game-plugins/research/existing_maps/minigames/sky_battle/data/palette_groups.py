import csv,collections
rows=list(csv.DictReader(open("data/blocks_mg_sb_01.csv")))
c=collections.Counter(r["block"].replace("minecraft:","") for r in rows); n=len(rows)
def g(b):
    if b=="barrier": return "barrier shell"
    if b in ("red_concrete","yellow_concrete","green_concrete","blue_concrete"): return "team concrete"
    if b in ("red_terracotta","yellow_terracotta","green_terracotta","blue_terracotta"): return "team terracotta"
    if b in ("red_glazed_terracotta","yellow_glazed_terracotta","green_glazed_terracotta","blue_glazed_terracotta"): return "team glazed terracotta"
    if b in ("orange_wool","cyan_wool","lime_wool","purple_wool"): return "balloon wool"
    if b in ("white_concrete","smooth_quartz","smooth_quartz_stairs","stone_bricks","stone_brick_stairs","stone_brick_wall","cobblestone_wall","polished_blackstone","polished_blackstone_wall"): return "neutral stone/quartz"
    if b in ("oak_fence","oak_leaves","oak_trapdoor","spruce_fence","spruce_fence_gate","spruce_trapdoor","spruce_slab","stripped_spruce_log","crafting_table","barrel","chest"): return "wood/containers"
    if b=="gray_wool": return "lobby wool"
    if b in ("gold_block","raw_gold_block"): return "lobby gold"
    if b in ("red_stained_glass","yellow_stained_glass","green_stained_glass","blue_stained_glass"): return "lobby glass"
    if b=="chain": return "chain"
    if b in ("iron_block",): return "iron knots"
    if b in ("command_block","repeating_command_block","birch_wall_sign","stone_button","light","lectern","campfire"): return "utility/lighting"
    if b.endswith("_concrete_powder"): return "join pads"
    return "other"
grp=collections.Counter()
for b,k in c.items(): grp[g(b)]+=k
for k,v in grp.most_common(): print(f"{k:28s} {v:6d} {100*v/n:6.2f}%")
