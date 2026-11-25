import csv,collections
rows=[r for r in csv.DictReader(open("data/blocks_mg_sb_01.csv")) if "barrier" not in r["block"]]
top=collections.defaultdict(int); blk={}
for r in rows:
    x,y,z=int(r["x"]),int(r["y"]),int(r["z"])
    if 1975<=x<=2025 and -25<=z<=25:
        if y>top[(x,z)]: top[(x,z)]=y; blk[(x,z)]=r["block"].replace("minecraft:","")
chars={11:"1",12:"2",13:"3",14:"4",15:"5",16:"6",17:"7",18:"8",19:"9",20:"a",21:"b",22:"c",23:"d",24:"e",25:"f",26:"g",27:"h",28:"i",29:"j",30:"k",31:"l",32:"m",33:"n",34:"o",35:"p",36:"q",37:"r"}
for z in range(-25,26):
    line=""
    for x in range(1975,2026):
        line+=chars.get(top.get((x,z),0)," ")
    print(f"{z:3d} {line}")
print("legend y11=1 .. y37=r, digits then letters")
