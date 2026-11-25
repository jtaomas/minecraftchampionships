import re
p="data/build_commands.py"
s=open(p,encoding="utf-8").read()
s=s.replace('if c.startswith("/item replace block "): return "loot"','if c.startswith("/item replace block "): return "loot"\n    if c.startswith("/title"): return "countdown_kit"')
s=s.replace('if 943<=x<=951 and y==10 and -4<=z<=2: return "cage_colour"','if 943<=x<=955 and y==10 and -4<=z<=2: return "cage_colour"')
s=s.replace('if x==978 and 10<=y<=23 and z==-8: return "container_reset"','if x==978 and 10<=y<=23 and z==-8: return "container_reset"\n    if x==975 and y==10 and z==-14: return "item_cleanup"\n    if x==978 and y==23 and z==-8: return "item_cleanup"\n    if 949<=x<=953 and 18<=y<=20 and -5<=z<=-3: return "reset_control"')
s=s.replace('if cat=="container_reset": return "removes a loot container (destroy)"','if cat=="container_reset": return "removes a loot container (destroy)"\n    if cat=="item_cleanup": return "kills dropped items"\n    if cat=="reset_control": return "clears the reset/win redstone pulse"')
open(p,"w",encoding="utf-8").write(s)
