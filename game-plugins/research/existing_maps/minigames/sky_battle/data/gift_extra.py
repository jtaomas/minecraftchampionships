import csv,collections
rows=list(csv.DictReader(open("data/blocks_mg_sb_01.csv")))
def inbox(x,z):
    return (1961<=x<=1973 and -6<=z<=6) or (2027<=x<=2039 and -6<=z<=6) or (1994<=x<=2006 and 27<=z<=39) or (1994<=x<=2006 and -39<=z<=-27)
sub=[r for r in rows if inbox(int(r["x"]),int(r["z"])) and "barrier" not in r["block"]]
print(len(sub))
# components not the 4 main islands: print blocks with x/z far from centers
for r in sub:
    x,y,z=int(r["x"]),int(r["y"]),int(r["z"])
    # four centers
    if min(abs(x-1967)+abs(z-0),abs(x-2033)+abs(z-0),abs(x-2000)+abs(z-33),abs(x-2000)+abs(z+33))>8:
        print(x,y,z,r["block"])
