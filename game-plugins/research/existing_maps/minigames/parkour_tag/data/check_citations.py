import re, os
files = ['README.md', 'REFERENCE_IMAGES.md', 'STATUS.md']
paths = set()
for f in files:
    s = open(f, encoding='utf-8').read()
    for m in re.findall(r'`((?:frames|renders)/[^`]+?\.(?:png|jpg|jpeg))`', s):
        paths.add(m)
missing = [p for p in sorted(paths) if not os.path.exists(p)]
print('cited', len(paths), 'missing', missing)
for p in sorted(paths):
    print(p)
