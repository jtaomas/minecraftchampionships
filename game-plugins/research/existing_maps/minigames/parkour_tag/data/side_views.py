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

def side(z):
    print(f'=== side view z={z} (x across, y down) ===')
    print('      ' + ''.join(str(x % 10) for x in range(1052, 1070)))
    for y in range(24, 9, -1):
        line = ''
        for x in range(1052, 1070):
            b = g.get((x, y, z))
            line += '.' if b is None else AB.get(b, '?')
        print(f'y{y:2d}  {line}')

for z in (984, 985, 986, 987, 1004, 1005):
    side(z)
