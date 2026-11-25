import os, re, glob
HERE = r'C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\tgttos_badlands'
missing = []
for md in glob.glob(os.path.join(HERE, '*.md')) + glob.glob(os.path.join(HERE, 'data', '*.md')):
    text = open(md, encoding='utf-8').read()
    for m in re.finditer(r'`((?:renders|frames|data)/[^`]+?\.(?:png|csv|md|json|mcfunction|mcmeta))`', text):
        p = m.group(1)
        if not os.path.exists(os.path.join(HERE, p.replace('/', os.sep))):
            missing.append((os.path.basename(md), p))
    for m in re.finditer(r'`((?:renders|frames)/[^`]+?\.png)`', text):
        p = m.group(1)
        if not os.path.exists(os.path.join(HERE, p.replace('/', os.sep))):
            missing.append((os.path.basename(md), p))
print('missing:', sorted(set(missing)))
print('renders', len(glob.glob(os.path.join(HERE, 'renders', '*.png'))))
print('frames', len(glob.glob(os.path.join(HERE, 'frames', '*.png'))))
print('data files', len(glob.glob(os.path.join(HERE, 'data', '*'))))
