"""Playability audit of the imported map `mcc6_full` (nothing is changed, it only reports): for every section, is there a chain of
foothold cells from the section's respawn to the next checkpoint box (or the finish volume) where every hop is within what a
sprint jump can do (flat 4.9 blocks centre to centre, 4.0 up one block, +0.5..0.6 per block dropped, from jump physics)?

    python -I tools/mcc6_reach.py <built map folder: course.txt + mcc6_full.mcmi.gz> [generous]

Standable cell = a solid-ish block below and two free cells above; fences/panes/trapdoors/leaves are treated as not standable
(conservative), so a "NONE" means "no plain-jump chain found", to be looked at by a person, not a proof. `generous` adds 1.0 to every reach.
"""
import gzip, heapq, math, os, re, struct, sys

NONSOLID = re.compile(r"torch|sign|banner|button|lever|tripwire|grass|fern|flower|tulip|poppy|dandelion|allium|bush|pressure_plate|rail|vine|"
                      r"redstone_wire|water|lava|fire|cobweb|sapling|skull|head|carpet|lantern|ladder|pickle|potted|bamboo|campfire|bell|end_rod|"
                      r"trapdoor|fence_gate|iron_bars|pane|leaves|hopper|lectern|observer|chain|candle")
EMPTY = re.compile(r"torch|sign|banner|button|lever|grass|fern|flower|tulip|poppy|dandelion|allium|bush|pressure|rail|vine|redstone_wire|"
                   r"cobweb|sapling|skull|carpet|ladder|pickle|potted")


def read_mcmi(path):
    with gzip.open(path, "rb") as f:
        assert f.read(4) == b"MCMI"
        f.read(4)
        n = struct.unpack(">i", f.read(4))[0]
        names = []
        for _ in range(n):
            ln = struct.unpack(">H", f.read(2))[0]
            names.append(f.read(ln).decode())
        m = struct.unpack(">i", f.read(4))[0]
        data = f.read(m * 10)
    blocks = {}
    for i in range(m):
        x, y, z, p = struct.unpack_from(">hhhi", data, i * 10)
        blocks[(x, y, z)] = names[p].split("[")[0].replace("minecraft:", "")
    return blocks


def main(argv):
    d = argv[0]
    extra = 1.0 if len(argv) > 1 and argv[1] == "generous" else 0.0
    b = read_mcmi(os.path.join(d, "mcc6_full.mcmi.gz"))
    cps, fin, start = [], None, None
    for l in open(os.path.join(d, "course.txt"), encoding="utf-8"):
        t = l.split()
        if not t:
            continue
        if t[0] == "cp":
            cps.append((int(t[1]), [float(v) for v in t[3:9]], [float(v) for v in t[10:14]]))
        elif t[0] == "finish":
            fin = [float(v) for v in t[2:8]]
        elif t[0] == "start":
            start = [float(v) for v in t[1:5]]
    sections = [(1, start, cps[0][1])] + [(k + 1, cps[k - 1][2], cps[k][1] if k < len(cps) else fin) for k in range(1, len(cps) + 1)]

    def solid(k):
        n = b.get(k)
        return n is not None and not NONSOLID.search(n) and n != "barrier"

    def empty(k):
        n = b.get(k)
        return n is None or bool(EMPTY.search(n))

    def standable(x, y, z):
        return solid((x, y - 1, z)) and empty((x, y, z)) and empty((x, y + 1, z))

    def reach(dy):
        return extra + (4.0 if dy >= 1 else 4.9 if dy == 0 else [4.9, 5.6, 6.1, 6.7, 7.2, 7.6, 8.0][min(6, -dy)])

    def hit(box, x, y, z):
        return box[0] < x + 0.3 and x - 0.3 < box[3] and y + 1.8 > box[1] and y < box[4] and box[2] < z + 0.3 and z - 0.3 < box[5]

    def path(seen, c):
        out = []
        while c:
            out.append(c)
            c = seen[c]
        return out[::-1]

    bad = []
    for k, st, box in sections:
        s = (int(math.floor(st[0])), int(round(st[1])), int(math.floor(st[2])))
        gc = ((box[0] + box[3]) / 2, (box[1] + box[4]) / 2, (box[2] + box[5]) / 2)
        seen = {s: None}
        q = [(0, s)]
        best, bd, goal = s, 1e9, None
        while q and goal is None:
            g, n = heapq.heappop(q)
            h = math.hypot(gc[0] - n[0], gc[2] - n[2])
            if h < bd:
                bd, best = h, n
            if hit(box, n[0] + 0.5, n[1], n[2] + 0.5):
                goal = n
                break
            R = int(reach(-6)) + 1
            for dx in range(-R, R + 1):
                for dz in range(-R, R + 1):
                    if not dx and not dz:
                        continue
                    D = math.hypot(dx, dz)
                    for dy in range(1, -7, -1):
                        if D > reach(dy):
                            continue
                        c = (n[0] + dx, n[1] + dy, n[2] + dz)
                        if c in seen or not standable(*c):
                            continue
                        seen[c] = n
                        heapq.heappush(q, (g + D, c))
        if goal:
            print("section %2d: chain of %d cells" % (k, len(path(seen, goal))))
        else:
            far = math.hypot(gc[0] - best[0], gc[2] - best[2])
            print("section %2d: NONE  (stuck at %s, %.1f blocks (xz) short of the checkpoint box centre)" % (k, best, far))
            bad.append(k)
    print("sections without a plain-jump chain:", bad or "none")


if __name__ == "__main__":
    main(sys.argv[1:])
