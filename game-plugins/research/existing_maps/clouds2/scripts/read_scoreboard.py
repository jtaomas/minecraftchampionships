#!/usr/bin/env python3
import nbtlib, json, sys
W = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Clouds_2_-_Electric_Boogaloo__Download_Version_\Clouds 2 - Electric Boogaloo"

def dump(path):
    f = nbtlib.load(path)
    def conv(v):
        if hasattr(v, "unpack"):
            try: return conv(v.unpack())
            except Exception: pass
        if isinstance(v, dict): return {str(k): conv(x) for k, x in v.items()}
        if isinstance(v, list): return [conv(x) for x in v]
        if isinstance(v, (bytes, bytearray)): return v.decode("utf-8", "replace")
        return v
    return conv(f)

print("== scoreboard.dat ==")
sb = dump(W + r"\data\scoreboard.dat")
print(json.dumps(sb, indent=1)[:6000])
