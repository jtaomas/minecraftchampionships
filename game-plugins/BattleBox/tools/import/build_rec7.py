import sys, numpy as np
sys.path.insert(0, r"C:\Users\LeonS\AppData\Local\Temp\opencode\work")
import mk

X, Y, Z, P, pal = mk.load(r"C:\Users\LeonS\AppData\Local\Temp\opencode\work\rec7.npz")

CREDIT = "Gerfield, Hmoushly, blepisagod, speedyparrot, Crazyblop (builders), Blueberry (guide) - BattleBox Recreation 7"
LICENSE = "private use only, no licence given"

hubm = (X >= -14) & (X <= 46) & (Y >= 52) & (Y <= 80) & (Z >= -22) & (Z <= 22)
hubm &= ~((Y < 70) & ((Z < -18) | (Z > 18)))
box = (X >= 0) & (X <= 32) & (Y >= 53) & (Y <= 69) & (Z >= -17) & (Z <= 17)
hubm &= ~((Y < 62) & ((X < 0) | (X > 32)) & ((Z < -7) | (Z > 7)))
hubm &= ~box

ARENAS = [
    ("bb7_neon", "Neon Arena", -193, 60),
    ("bb7_fortress", "Cracked Fortress", -149, 55),
    ("bb7_copper", "Copper Pillars", -105, 62),
    ("bb7_woods", "Farmer Woods", -61, 60),
    ("bb7_desert", "Ruined Desert", 29, 54),
    ("bb7_lighthouse", "Lighthouse", 73, 66),
]
def ladder(x, y0, y1, facing):
    return [(x, y, 0, "minecraft:ladder[facing=%s,waterlogged=false]" % facing) for y in range(y0, y1 + 1)]


def backing(x, y0, y1, state):
    return [(x, y, 0, state) for y in range(y0, y1 + 1)]


ADD = {
    # Neon: the pad sits on a floating platform 6 up (the original needed ender pearls / jump boost): one ladder per side
    "bb7_neon": backing(-5, 1, 5, "minecraft:black_concrete") + backing(5, 1, 5, "minecraft:black_concrete")
                + ladder(-6, 1, 6, "west") + ladder(6, 1, 6, "east"),
    # Lighthouse: the pad crowns the tower: one ladder per side up the tower wall to the ring beside the pad
    "bb7_lighthouse": backing(-2, 2, 12, "minecraft:red_concrete") + backing(2, 2, 12, "minecraft:red_concrete")
                + ladder(-3, 2, 12, "west") + ladder(3, 2, 12, "east"),
}

for aid, name, zt, pady in ARENAS:
    t = (X >= 0) & (X <= 32) & (Y >= 53) & (Y <= 69) & (Z >= zt) & (Z <= zt + 34)
    xs = np.concatenate([X[hubm], X[t]]) - 16
    ys = np.concatenate([Y[hubm], Y[t]]) - 53
    zs = np.concatenate([Z[hubm], Z[t] - zt - 17])
    ps = np.concatenate([P[hubm], P[t]])
    spec = dict(id=aid, name=name, credit=CREDIT, license=LICENSE,
                centre=(0, pady - 53, 0), centre_size=(3, 3),
                spawn_a=(-20, 1, 0), spawn_b=(20, 1, 0),
                gates_a=[(-16, 1, -4, -16, 5, 4)], gates_b=[(16, 1, -4, 16, 5, 4)],
                remove_boxes=[(-15, 2, -4, -15, 2, 4), (15, 2, -4, 15, 2, 4)], add=ADD.get(aid, []))
    mk.build(spec, xs, ys, zs, ps, pal)




