import json, zipfile, os
HERE = r'C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs'
van = json.loads(zipfile.ZipFile(r'C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_vanilla_jars\1.19.2.jar').read('assets/minecraft/lang/en_us.json').decode('utf-8-sig'))
pack = json.load(open(os.path.join(HERE, 'packs','clouds2','extracted','assets','minecraft','lang','en_us.json'), encoding='utf-8-sig'))
print('## pack-only keys')
for k in pack:
    if k not in van:
        print(repr(k), '=>', repr(pack[k]))
print('## changed values')
n=0
for k in pack:
    if k in van and van[k] != pack[k]:
        n+=1
        print(repr(k), '| vanilla:', repr(van[k])[:90], '| pack:', repr(pack[k])[:120])
print('changed', n)
print('## missing keys sample')
miss=[k for k in van if k not in pack]
print(len(miss), miss[:12])
