#!/usr/bin/env python3
"""Read level.dat of the Space Race world and print key facts."""
import nbtlib

P = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Space_Race\Space Race\level.dat"
f = nbtlib.load(P)
d = f["Data"]
for k in d:
    v = d[k]
    s = str(v)
    print(k, "=", s[:400])
