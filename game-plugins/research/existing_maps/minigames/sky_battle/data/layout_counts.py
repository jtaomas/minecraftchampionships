import csv,collections
rows=list(csv.DictReader(open("data/blocks_mg_sb_01.csv")))
def count_box(x0,x1,y0,y1,z0,z1,barrier=False):
    return sum(1 for r in rows if x0<=int(r["x"])<=x1 and y0<=int(r["y"])<=y1 and z0<=int(r["z"])<=z1 and (barrier or "barrier" not in r["block"]))
boxes={
"GIFT-RED":(1961,1973,11,26,-6,6),"GIFT-GREEN":(2027,2039,11,26,-6,6),
"GIFT-BLUE":(1994,2006,11,26,27,39),"GIFT-YELLOW":(1994,2006,11,26,-39,-27),
"BALLOON-NW":(1970,1978,14,42,-30,-22),"BALLOON-SW":(1970,1978,14,42,22,30),
"BALLOON-NE":(2022,2030,14,42,-30,-22),"BALLOON-SE":(2022,2030,14,42,22,30),
"RING":(1980,2020,11,37,-20,20),
"MID":(1999,2001,20,30,-1,1),
"LOBBY":(1993,2007,57,80,-7,7),
}
for k,b in boxes.items(): print(k,count_box(*b))
print("ring y25",sum(1 for r in rows if 1980<=int(r["x"])<=2020 and int(r["y"])==25 and -20<=int(r["z"])<=20 and "barrier" not in r["block"]))
print("canopy y27-37",sum(1 for r in rows if 1980<=int(r["x"])<=2020 and 27<=int(r["y"])<=37 and -20<=int(r["z"])<=20 and "barrier" not in r["block"]))
print("fence/leaves",sum(1 for r in rows if 1980<=int(r["x"])<=2020 and (("oak_fence" in r["block"]) or ("oak_leaves" in r["block"])) and -20<=int(r["z"])<=20))
print("inner lip y19",sum(1 for r in rows if 1980<=int(r["x"])<=2020 and int(r["y"])==19 and -20<=int(r["z"])<=20 and "barrier" not in r["block"]))
