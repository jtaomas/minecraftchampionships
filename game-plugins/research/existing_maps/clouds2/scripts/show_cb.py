#!/usr/bin/env python3
import json
D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
cb = json.load(open(D + r"\command_blocks.json", encoding="utf-8"))
for r in cb:
    print("%6d %5d %6d a=%d c=%d p=%d  %s" % (r['x'], r['y'], r['z'], r['auto'], r['conditional'], r['powered'], r['command']))
