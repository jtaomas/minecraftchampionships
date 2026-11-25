#!/usr/bin/env python3
"""ASCII surface maps: top walkable block per column, one char per block."""
import gzip, sys, argparse
from collections import Counter, defaultdict

CH = {
 'terracotta':'t','stone':'S','granite':'g','andesite':'a','red_sand':'r','dirt':'d',
 'brown_concrete_powder':'b','white_terracotta':'W','red_terracotta':'R','gray_terracotta':'G',
 'diorite':'D','polished_granite':'p','soul_sand':'s','smooth_stone':'O','light_gray_terracotta':'l',
 'cyan_terracotta':'c','quartz_block':'Q','stone_bricks':'B','gravel':'v','red_concrete':'X',
 'black_wool':'k','chain':'h','barrier':'#','acacia_leaves':'L','cactus':'C','jungle_planks':'j',
 'nether_brick_wall':'n','blackstone':'N','red_wool':'x','nether_brick_slab':'m','white_concrete':'q',
 'smooth_red_sandstone':'u','black_terracotta':'T','gray_concrete':'Y','sea_lantern':'*',
 'light_gray_concrete':'-','cyan_concrete_powder':'y','white_wool':'o','iron_block':'I',
 'iron_trapdoor':':','daylight_detector':'%','black_concrete':'K','iron_bars':'i','spruce_planks':'P',
 'cyan_concrete':'C','cracked_stone_bricks':'E','lantern':'@','nether_brick_fence':'f',
 'smooth_stone_slab':'=','light_blue_concrete':'_','oak_leaves':'l','oak_wall_sign':'?',
 'red_sandstone_stairs':'z','diorite_wall':'|','jungle_fence':'J','light_gray_concrete_powder':',',
 'black_concrete_powder':'.','light_blue_concrete_powder':';','iron_bars':'i',
}

ap = argparse.ArgumentParser()
ap.add_argument('--min', nargs=3, type=int, required=True)
ap.add_argument('--max', nargs=3, type=int, required=True)
a = ap.parse_args()
lo, hi = a.min, a.max
solid = {}
allblocks = defaultdict(list)
with gzip.open('world.mcmdump.gz', 'rt') as f:
    f.readline()
    for line in f:
        p = line.split(' ', 3)
        x = int(p[0]); y = int(p[1]) - 64; z = int(p[2]) - 16
        if not (lo[0] <= x <= hi[0] and lo[1] <= y <= hi[1] and lo[2] <= z <= hi[2]):
            continue
        b = p[3].strip().split('[')[0].replace('minecraft:', '')
        allblocks[(x, z)].append((y, b))

def walkable(x, z):
    col = sorted(allblocks.get((x, z), []), reverse=True)
    ys = {y for y, b in col}
    for y, b in col:
        if b in ('acacia_leaves','oak_leaves','cactus','chain','lantern','barrier'):
            continue
        if (y + 1) not in ys and (y + 2) not in ys:
            return y, b
    return None

print('    ' + ''.join(str((x // 100) % 10) for x in range(lo[0], hi[0] + 1)))
print('    ' + ''.join(str((x // 10) % 10) for x in range(lo[0], hi[0] + 1)))
print('    ' + ''.join(str(x % 10) for x in range(lo[0], hi[0] + 1)))
for z in range(lo[2], hi[2] + 1):
    row = []
    for x in range(lo[0], hi[0] + 1):
        w = walkable(x, z)
        row.append(CH.get(w[1], '?') if w else ' ')
    print(f'{z:3d} ' + ''.join(row))
