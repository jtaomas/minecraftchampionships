import json, os
base = r'C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs\data\vanilla_compare'
events = ['block.bell.use','block.candle.extinguish','block.cherrywood_trapdoor.toggle','block.cherry_wood_trapdoor.toggle','music_disc.far','block.cave_vines.break','block.chain.break','block.chain.step','block.brewing_stand.brew','block.azalea.break']
for ver in ['1.19.2','1.20.1','1.21.1']:
    p = os.path.join(base, ver, 'minecraft_sounds.json')
    if not os.path.exists(p):
        print(ver, 'no sounds.json'); continue
    s = json.load(open(p, encoding='utf-8'))
    print('###', ver)
    for e in events:
        if e not in s:
            print('  ABSENT', e); continue
        entry = s[e]
        names = []
        for snd in entry.get('sounds', []):
            names.append(snd if isinstance(snd, str) else snd.get('name'))
        print('  ', e, '->', names)
