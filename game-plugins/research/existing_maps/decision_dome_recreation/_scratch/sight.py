import csv, math
p=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\_scratch\blocks_raw.csv"
B={}
for row in csv.DictReader(open(p,encoding="utf-8")):
    B[(int(row["x"]),int(row["y"]),int(row["z"]))]=row["block"].replace("minecraft:","").split("[")[0]
CX,CZ=-3,16
TRANSPARENT={"white_stained_glass","white_stained_glass_pane","light_gray_stained_glass_pane","barrier","light_gray_carpet","gray_carpet","chain","yellow_wool","light_blue_wool","lime_wool","purple_wool","gray_wool"}
for c in ["lime","orange","yellow","red","pink","purple","green","cyan","light_blue","blue"]:
    TRANSPARENT.add(f"{c}_stained_glass"); TRANSPARENT.add(f"{c}_stained_glass_pane")

def solid(x,y,z):
    n=B.get((x,y,z))
    if n is None: return False
    return n not in TRANSPARENT

def ray(a,b,step=0.03):
    dx=b[0]-a[0]; dy=b[1]-a[1]; dz=b[2]-a[2]
    d=math.sqrt(dx*dx+dy*dy+dz*dz); n=int(d/step)
    for i in range(1,n):
        t=i/n
        x=a[0]+dx*t; y=a[1]+dy*t; z=a[2]+dz*t
        if solid(math.floor(x),math.floor(y),math.floor(z)):
            return False
    return True

def ang(a): return math.radians(a)
# targets: wedge centres at 22.5+45k at r6 and r8.5, dial rim at r9, centre
targets=[]
for k in range(8):
    a=math.radians(22.5+45*k)
    for r in (5,8):
        targets.append((f"w{k+1}r{r}", CX+r*math.cos(a), 58.55, CZ+r*math.sin(a)))
targets.append(("centre",CX,58.55,CZ))
# terrace positions
print("Terrace eye 69.62, r26.5")
for a_deg in range(0,360,15):
    a=ang(a_deg)
    eye=(CX+26.5*math.cos(a),69.62,CZ+26.5*math.sin(a))
    vis=[]
    for name,x,y,z in targets:
        if ray(eye,(x,y,z)): vis.append(name)
    print(f"{a_deg:3d}deg: {len(vis):2d}/{len(targets)} visible -> {','.join(vis) if vis else '-'}")

print("\nFrom red carpet north (-3,71.6,-19):")
eye=(-3,71.62,-19)
vis=[name for name,x,y,z in targets if ray(eye,(x,y,z))]
print(len(vis),"/",len(targets),vis)

print("\nFrom tunnel mouth west (-29,69.6,16):")
eye=(-28,69.62,16)
vis=[name for name,x,y,z in targets if ray(eye,(x,y,z))]
print(len(vis),"/",len(targets),vis)

print("\nFrom outside ground/top view (0,100,0) all visible?")
eye=(0,100,0)
vis=[name for name,x,y,z in targets if ray(eye,(x,y,z))]
print(len(vis),"/",len(targets))
