import csv, collections
rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
keys = ['slime_block', 'blue_ice', 'obsidian', 'iron_trapdoor', 'ladder', 'stone_brick_stairs',
        'smooth_quartz_stairs', 'spruce_stairs', 'polished_deepslate_stairs', 'deepslate_brick_stairs',
        'brick_slab', 'smooth_quartz_slab', 'spruce_slab', 'oak_slab', 'stone_brick_slab',
        'deepslate_tile_slab', 'polished_deepslate_slab', 'deepslate_brick_slab', 'sticky_piston',
        'piston_head', 'iron_door', 'iron_block', 'spruce_trapdoor', 'light']
by = collections.defaultdict(list)
for r in rows:
    b = r['block'].split(':')[1]
    if b in keys:
        by[b].append((int(r['x']), int(r['y']), int(r['z'])))
for k in keys:
    if not by[k]:
        continue
    print('==', k, len(by[k]))
    # group by y
    gy = collections.defaultdict(list)
    for x, y, z in by[k]:
        gy[y].append((x, z))
    for y in sorted(gy):
        pts = sorted(gy[y])
        print(f'  y={y}: ' + ' '.join(f'{x},{z}' for x, z in pts))
