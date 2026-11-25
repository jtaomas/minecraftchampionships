import csv, collections
rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
X0, X1, Z0, Z1 = 1048, 1071, 978, 1010
grid = {}
for r in rows:
    grid[(int(r['x']), int(r['y']), int(r['z']))] = r['block'].split(':')[1]

ABBR = {
    'gray_concrete': 'G', 'cyan_terracotta': 'C', 'stone_bricks': 'B', 'smooth_quartz': 'Q',
    'white_concrete': 'W', 'dead_horn_coral_block': 'D', 'barrier': '#', 'brick_slab': 'b',
    'black_concrete': 'K', 'deepslate_bricks': 'E', 'red_concrete': 'R', 'waxed_copper_block': 'c',
    'smooth_quartz_stairs': 'q', 'waxed_exposed_copper': 'e', 'waxed_weathered_copper': 'w',
    'redstone_wire': 'r', 'iron_trapdoor': 'T', 'repeater': 'p', 'stone_brick_stairs': 's',
    'waxed_oxidized_copper': 'o', 'chain_command_block': 'x', 'smooth_quartz_slab': 'u',
    'spruce_stairs': 'S', 'command_block': 'X', 'repeating_command_block': 'P',
    'blue_ice': 'I', 'light': 'L', 'terracotta': 't', 'birch_wall_sign': 'N',
    'deepslate_tiles': 'F', 'tinted_glass': 'g', 'oak_slab': 'O', 'polished_deepslate_wall': 'V',
    'obsidian': 'Z', 'stone_brick_wall': 'v', 'polished_deepslate_stairs': 'd', 'ladder': 'H',
    'spruce_slab': 'U', 'deepslate_brick_stairs': 'i', 'sticky_piston': 'Y', 'iron_door': 'M',
    'slime_block': 'J', 'red_terracotta': 'A', 'stone_brick_slab': 'j', 'birch_button': 'n',
    'redstone_block': 'X', 'soul_lantern': 'l', 'spruce_trapdoor': 'm', 'iron_block': 'k',
    'bookshelf': 'K', 'deepslate_tile_slab': 'f', 'piston_head': 'y', 'waxed_cut_copper_stairs': 'a',
    'stone_pressure_plate': 'z', 'polished_deepslate_slab': 'e', 'potted_cornflower': 'i',
    'deepslate_brick_slab': 'h', 'lectern': 't', 'glowstone': '*', 'sea_lantern': '!',
    'chiseled_stone_bricks': 'H',
}

def show(y, x0=X0, x1=X1, z0=Z0, z1=Z1):
    print(f'--- y={y} ---')
    print('     ' + ''.join(str(x % 10) for x in range(x0, x1 + 1)))
    for z in range(z0, z1 + 1):
        line = ''
        for x in range(x0, x1 + 1):
            b = grid.get((x, y, z))
            line += '.' if b is None else ABBR.get(b, '?')
        print(f'z{z:3d} {line}')

for y in [10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26]:
    show(y)
