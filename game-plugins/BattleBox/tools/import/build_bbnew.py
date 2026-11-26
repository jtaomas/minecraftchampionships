import sys, numpy as np
sys.path.insert(0, r"C:\Users\LeonS\AppData\Local\Temp\opencode\work")
import mk

X, Y, Z, P, pal = mk.load(r"C:\Users\LeonS\AppData\Local\Temp\opencode\work\bbnew.npz")
m = (X >= -138) & (X <= -86) & (Y >= -60) & (Y <= -17) & (Z >= -134) & (Z <= -95)
# local origin: centre cell (-112, -114), floor block y = -54
xs, ys, zs, ps = X[m] + 112, Y[m] + 54, Z[m] + 114, P[m]
spec = dict(id="bbnew_sky", name="Sky Box (Battle Box New)",
            credit="Legitimoose (youtube.com/c/legitimoose) - Battle Box New",
            license="private use only, no licence given",
            centre=(0, -2, 0), centre_size=(4, 4),
            spawn_a=(-23, 1, 0), spawn_b=(22, 1, 0),
            gates_a=[(-20, 1, -3, -20, 4, 2)], gates_b=[(19, 1, -3, 19, 4, 2)])
mk.build(spec, xs, ys, zs, ps, pal)

