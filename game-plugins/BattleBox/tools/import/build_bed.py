import sys, numpy as np
sys.path.insert(0, r"C:\Users\LeonS\AppData\Local\Temp\opencode\work")
import mk

X, Y, Z, P, pal = mk.load(r"C:\Users\LeonS\AppData\Local\Temp\opencode\work\bedj.npz")
m = (X >= -28) & (X <= 24) & (Y >= 0) & (Y <= 49) & (Z >= -37) & (Z <= 5)
ground = np.array([p.split("[")[0] in ("minecraft:grass_block", "minecraft:dirt", "minecraft:bedrock") for p in pal])[P]
inbox = (X >= -19) & (X <= 15) & (Z >= -33) & (Z <= 1)
m &= ~(ground & ~inbox)
# local origin: centre (-2, -16), floor block y = 4
xs, ys, zs, ps = X[m] + 2, Y[m] - 4, Z[m] + 16, P[m]
spec = dict(id="bbmcbe_skypark", name="Sky Park (Battle Box MCBE)",
            credit="builder not named in the download - MCC BattleBox MCBE (Bedrock world; Chef Franky Function Template, MCC Recreation pack by Chairocchi/pv6q)",
            license="private use only, no licence given",
            centre=(0, 6, 0), centre_size=(3, 3),
            spawn_a=(-20, 1, 0), spawn_b=(20, 1, 0),
            gates_a=[(-16, 1, -4, -16, 4, 4)], gates_b=[(16, 1, -4, 16, 4, 4)])
mk.build(spec, xs, ys, zs, ps, pal)


