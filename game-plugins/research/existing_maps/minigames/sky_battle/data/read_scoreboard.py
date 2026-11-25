import nbtlib, json
p=r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3\data\scoreboard.dat"
n=nbtlib.load(p)
def walk(tag, depth=0):
    if isinstance(tag, dict):
        for k,v in tag.items():
            if isinstance(v,(dict,list)):
                print("  "*depth+str(k)+":")
                walk(v,depth+1)
            else:
                print("  "*depth+f"{k}={v}")
    elif isinstance(tag, list):
        for i,v in enumerate(tag):
            print("  "*depth+f"[{i}]")
            walk(v,depth+1)
walk(n)
