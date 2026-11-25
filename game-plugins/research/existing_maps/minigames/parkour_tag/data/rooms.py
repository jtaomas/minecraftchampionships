import csv
rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
g = {(int(r['x']), int(r['y']), int(r['z'])): r['block'].split(':')[1] for r in rows}
AB = {'cyan_terracotta': 'C', 'stone_bricks': 'B', 'deepslate_bricks': 'E', 'waxed_copper_block': 'c',
      'waxed_exposed_copper': 'e', 'waxed_weathered_copper': 'w', 'waxed_oxidized_copper': 'o',
      'iron_trapdoor': 'T', 'ladder': 'H', 'stone_brick_stairs': 's', 'smooth_quartz_stairs': 'q',
      'smooth_quartz_slab': 'u', 'brick_slab': 'b', 'spruce_stairs': 'S', 'spruce_slab': 'U',
      'dead_horn_coral_block': 'D', 'blue_ice': 'I', 'slime_block': 'J', 'obsidian': 'Z',
      'red_concrete': 'R', 'white_concrete': 'W', 'gray_concrete': 'G', 'barrier': '#',
      'light': 'L', 'deepslate_tiles': 'F', 'iron_door': 'M', 'iron_block': 'k', 'oak_slab': 'O',
      'stone_brick_slab': 'j', 'deepslate_brick_stairs': 'i', 'polished_deepslate_stairs': 'd',
      'polished_deepslate_slab': 'e', 'deepslate_tile_slab': 'f', 'deepslate_brick_slab': 'h',
      'stone_brick_wall': 'v', 'polished_deepslate_wall': 'V', 'spruce_trapdoor': 'm'}

def show(x0, x1, z0, z1, ys):
    for y in ys:
        print(f'--- x{x0}..{x1} z{z0}..{z1} y={y} ---')
        print('      ' + ''.join(str(x % 10) for x in range(x0, x1 + 1)))
        for z in range(z0, z1 + 1):
            line = ''
            for x in range(x0, x1 + 1):
                b = g.get((x, y, z))
                line += '.' if b is None else AB.get(b, '?')
            print(f'z{z:4d} {line}')

print('### NW room x1050..1058 z984..992')
show(1050, 1058, 984, 992, range(11, 18))
print('### SW room x1050..1058 z998..1005')
show(1050, 1058, 998, 1005, range(11, 18))
print('### SE room x1063..1071 z998..1005')
show(1063, 1071, 998, 1005, range(11, 21))
print('### NE room x1062..1071 z984..993')
show(1062, 1071, 984, 993, range(11, 19))
