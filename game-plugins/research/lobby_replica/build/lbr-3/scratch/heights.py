import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import build_lbr3 as B

H = B.heightfield()
chars = " .:-=+*#%@"   # 0..9+ by height/5
print("     " + "".join(str((x // 10) % 10) for x in range(B.MX0, B.MX1 + 1)))
for z in range(B.MZ0, B.MZ1 + 1):
    row = ""
    for x in range(B.MX0, B.MX1 + 1):
        h = H[(x, z)]
        row += chars[min(9, h // 5)]
    print("%4d %s" % (z, row))
print()
vals = sorted(H.values())
print("min", vals[0], "max", vals[-1])
print("histogram:", {k: sum(1 for v in vals if v // 5 == k) for k in range(10)})
