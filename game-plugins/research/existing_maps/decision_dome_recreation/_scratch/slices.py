import csv, sys
from collections import Counter
path=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\_scratch\blocks_raw.csv"
B={}
for row in csv.DictReader(open(path,encoding="utf-8")):
    B[(int(row["x"]),int(row["y"]),int(row["z"]))]=row["block"].replace("minecraft:","")

# symbol map
def sym(n):
    if n is None: return "."
    n=n.split("[")[0]
    table={
      "gray_concrete":"G","white_concrete":"W","black_concrete":"K","red_concrete":"R","orange_concrete":"O",
      "yellow_concrete":"Y","lime_concrete":"L","green_concrete":"E","cyan_concrete":"C","light_blue_concrete":"B",
      "blue_concrete":"U","purple_concrete":"P","pink_concrete":"I","light_gray_concrete":"g",
      "white_stained_glass":"w","white_stained_glass_pane":"p","light_gray_stained_glass_pane":"q",
      "lime_stained_glass_pane":"l","orange_stained_glass_pane":"o","yellow_stained_glass_pane":"y",
      "red_stained_glass_pane":"r","pink_stained_glass_pane":"i","cyan_stained_glass_pane":"c",
      "light_blue_stained_glass_pane":"b","blue_stained_glass_pane":"u","purple_stained_glass_pane":"v",
      "green_stained_glass_pane":"e","black_stained_glass_pane":"k",
      "gold_block":"A","smooth_quartz_slab":"s","quartz_slab":"S","stone_slab":"T","polished_andesite_slab":"a",
      "polished_andesite_stairs":"A2","smooth_quartz":"Q","polished_diorite":"D","light_gray_terracotta":"t",
      "red_terracotta":"1","orange_terracotta":"2","yellow_terracotta":"3","lime_terracotta":"4",
      "green_terracotta":"5","cyan_terracotta":"6","light_blue_terracotta":"7","blue_terracotta":"8",
      "purple_terracotta":"9","pink_terracotta":"0","barrier":"#","light_gray_carpet":"_","gray_carpet":"-",
      "chain":"|","sea_lantern":"*","iron_trapdoor":"H","smooth_red_sandstone_slab":">","yellow_wool":"~",
      "redstone_block":"!","beehive":"h","stone_bricks":"X","polished_blackstone":"x","quartz_block":"z",
    }
    return table.get(n,"?")

def slice_z(z, x0=-46, x1=40, y0=55, y1=80):
    print(f"\n=== slice z={z} (x horizontal {x0}..{x1}, y vertical {y1}..{y0}) ===")
    hdr="     "+"".join(str(abs(x)%10) for x in range(x0,x1+1))
    print(hdr)
    for y in range(y1,y0-1,-1):
        line="".join(sym(B.get((x,y,z))) for x in range(x0,x1+1))
        print(f"{y:4d} {line}")

def slice_x(x, z0=-22, z1=45, y0=55, y1=80):
    print(f"\n=== slice x={x} (z horizontal {z0}..{z1}, y vertical {y1}..{y0}) ===")
    hdr="     "+"".join(str(abs(z)%10) for z in range(z0,z1+1))
    print(hdr)
    for y in range(y1,y0-1,-1):
        line="".join(sym(B.get((x,y,z))) for z in range(z0,z1+1))
        print(f"{y:4d} {line}")

slice_z(16)
slice_z(0)
slice_x(-3)
slice_x(8)
