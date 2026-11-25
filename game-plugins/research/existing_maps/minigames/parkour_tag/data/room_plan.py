import csv, collections
rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
grid = {(int(r['x']), int(r['y']), int(r['z'])): r['block'].split(':')[1] for r in rows}
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
    'chiseled_stone_bricks': 'H', 'chiseled_polished_blackstone': 'X',
}
X0, X1, Z0, Z1 = 1040, 1078, 977, 1010
for y in (0, 1, 2, 3, 4, 5, 6, 9):
    print(f'--- y={y} ---')
    print('      ' + ''.join(str(x % 10) for x in range(X0, X1 + 1)))
    for z in range(Z0, Z1 + 1):
        line = ''
        for x in range(X0, X1 + 1):
            b = grid.get((x, y, z))
            line += '.' if b is None else ABBR.get(b, '?')
        print(f'z{z:4d} {line}')
