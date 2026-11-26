"""Offline route check for imported Battle Box arenas: can a 0.6x1.8 walker get from each spawn to the centre pad
(gates open) and is the centre unreachable with the gates closed? Mirrors what ImportedArenaTheme.generate builds."""
import gzip, struct, sys, os, re, collections
from collections import deque

OUT = r"C:\Users\LeonS\AppData\Local\Temp\opencode\bb_imports\out"

PASS_EXACT = {"air", "cave_air", "void_air", "short_grass", "tall_grass", "fern", "large_fern", "dead_bush", "vine", "lily_pad", "light",
              "water", "fire", "soul_fire", "torch", "wall_torch", "soul_torch", "soul_wall_torch", "lantern", "soul_lantern",
              "ladder", "end_rod", "snow", "weeping_vines", "twisting_vines", "weeping_vines_plant", "twisting_vines_plant",
              "rail", "powered_rail", "detector_rail", "activator_rail", "seagrass", "tall_seagrass", "kelp", "kelp_plant",
              "wheat", "carrots", "potatoes", "beetroots", "sugar_cane", "cobweb", "sweet_berry_bush", "bamboo_sapling",
              "poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet", "allium", "blue_orchid", "red_tulip", "orange_tulip",
              "white_tulip", "pink_tulip", "lily_of_the_valley", "sunflower", "lilac", "rose_bush", "peony", "brown_mushroom", "red_mushroom",
              "campfire", "soul_campfire", "tripwire", "cauldron", "water_cauldron", "lava_cauldron", "moss_carpet"}
PASS_SUFFIX = ("_carpet", "_door", "_fence_gate", "_trapdoor", "_pressure_plate", "_button", "_sapling", "_torch", "_banner", "_sign",
               "_bed", "_pane_x")
BAD = {"lava", "magma_block", "fire", "soul_fire", "campfire", "soul_campfire", "sweet_berry_bush", "cactus", "powder_snow"}


def base(s):
    return s.split("[")[0].replace("minecraft:", "")


def passable(s):
    if s is None:
        return True
    b = base(s)
    if b in PASS_EXACT or b.endswith(PASS_SUFFIX):
        return True
    return False


def load(aid):
    path = os.path.join(OUT, aid, "blocks.mcmi.gz")
    data = gzip.open(path, "rb").read()
    assert data[:4] == b"MCMI"
    ver, npal = struct.unpack(">ii", data[4:12])
    p = 12
    pal = []
    for _ in range(npal):
        n = struct.unpack(">H", data[p:p + 2])[0]
        p += 2
        pal.append(data[p:p + n].decode())
        p += n
    n = struct.unpack(">i", data[p:p + 4])[0]
    p += 4
    blocks = {}
    for i in range(n):
        x, y, z, pi = struct.unpack(">hhhi", data[p:p + 10])
        p += 10
        blocks[(x, y, z)] = pal[pi]
    yml = open(os.path.join(OUT, aid, "map.yml"), encoding="utf-8").read()
    def vec(key):
        m = re.search(key + r": \[(-?\d+), (-?\d+), (-?\d+)\]", yml)
        return tuple(int(v) for v in m.groups())
    def boxes(key):
        m = re.search(key + r": \[(.*)\]\n", yml)
        return [tuple(int(v) for v in b.split(",")) for b in re.findall(r"\[([-\d, ]+)\]", m.group(1))]
    cs = re.search(r"centre_size: \[(\d+), (\d+)\]", yml)
    return dict(blocks=blocks, centre=vec("centre"), size=(int(cs.group(1)), int(cs.group(2))), sa=vec("spawn_a"), sb=vec("spawn_b"),
                ga=boxes("gates_a"), gb=boxes("gates_b"))


def prepare(m, gates_closed):
    b = dict(m["blocks"])
    cx, cy, cz = m["centre"]
    w, d = m["size"]
    for dx in range(w):
        for dz in range(d):
            x, z = cx - w // 2 + dx, cz - d // 2 + dz
            b[(x, cy, z)] = "minecraft:white_concrete"
            for dy in range(1, 4):
                b.pop((x, cy + dy, z), None)
            if (x, cy - 1, z) not in b:
                b[(x, cy - 1, z)] = "minecraft:sea_lantern"
    for sp in (m["sa"], m["sb"]):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                x, z = sp[0] + dx, sp[2] + dz
                for dy in range(0, 3):
                    b.pop((x, sp[1] + dy, z), None)
                if (x, sp[1] - 1, z) not in b:
                    b[(x, sp[1] - 1, z)] = "minecraft:red_concrete"
    if gates_closed:
        for box in m["ga"] + m["gb"]:
            for x in range(box[0], box[3] + 1):
                for y in range(box[1], box[4] + 1):
                    for z in range(box[2], box[5] + 1):
                        b[(x, y, z)] = "minecraft:red_stained_glass"
    return b


def reach(b, start, limit_box):
    (x0, y0, z0, x1, y1, z1) = limit_box
    def P(x, y, z):
        return passable(b.get((x, y, z)))
    def standable(x, y, z):
        if not (x0 <= x <= x1 and y0 <= y <= y1 and z0 <= z <= z1):
            return False
        if not (P(x, y, z) and P(x, y + 1, z)):
            return False
        below = b.get((x, y - 1, z))
        if base(below or "air") in BAD or base(b.get((x, y, z)) or "air") in BAD:
            return False
        if base(b.get((x, y, z)) or "air") in ("water", "ladder", "vine"):
            return True
        return not passable(below) or base(below or "air") in ("water",)
    seen = {start}
    q = deque([start])
    while q:
        x, y, z = q.popleft()
        cur = base(b.get((x, y, z)) or "air")
        nxt = []
        # climb ladders / swim up
        if cur in ("ladder", "vine", "water") and P(x, y + 1, z) and P(x, y + 2, z):
            nxt.append((x, y + 1, z))
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
            nx, nz = x + dx, z + dz
            if dx and dz and not (P(x + dx, y, z) and P(x, y, z + dz) and P(x + dx, y + 1, z) and P(x, y + 1, z + dz)):
                continue
            # level, step up one (needs 3 free above current), drop
            if standable(nx, y, nz):
                nxt.append((nx, y, nz))
            if P(x, y + 2, z) and standable(nx, y + 1, nz):
                nxt.append((nx, y + 1, nz))
            for drop in range(1, 25):
                if not (P(nx, y - drop + 1, nz) and P(nx, y - drop + 2, nz)):
                    break
                if standable(nx, y - drop, nz):
                    nxt.append((nx, y - drop, nz))
                    break
        for n in nxt:
            if n not in seen:
                seen.add(n)
                q.append(n)
    return seen


def check(aid):
    m = load(aid)
    ks = list(m["blocks"].keys())
    lim = (min(k[0] for k in ks) - 2, min(k[1] for k in ks) - 2, min(k[2] for k in ks) - 2,
           max(k[0] for k in ks) + 2, max(k[1] for k in ks) + 12, max(k[2] for k in ks) + 2)
    cx, cy, cz = m["centre"]
    w, d = m["size"]
    cells = [(cx - w // 2 + dx, cy + 1, cz - d // 2 + dz) for dx in range(w) for dz in range(d)]
    res = {}
    for gates_closed in (False, True):
        b = prepare(m, gates_closed)
        for side, sp in (("A", m["sa"]), ("B", m["sb"])):
            seen = reach(b, (sp[0], sp[1], sp[2]), lim)
            # every pad cell must be within block reach (4.4) of some reachable standing position
            covered = 0
            for (cx_, cy_, cz_) in cells:
                if any(((s[0] + .5 - cx_ - .5) ** 2 + (s[1] + 1.62 - cy_ + 1 - .5) ** 2 + (s[2] + .5 - cz_ - .5) ** 2) <= 4.4 ** 2 for s in seen):
                    covered += 1
            near = [1] if covered == len(cells) else []
            res[(side, "closed" if gates_closed else "open")] = (len(seen), len(near) > 0)
    return res


if __name__ == "__main__":
    ids = sys.argv[1:] or sorted(os.listdir(OUT))
    for aid in ids:
        r = check(aid)
        print(aid, {f"{k[0]}-{k[1]}": f"{v[0]} pos, centre {'REACHED' if v[1] else 'no'}" for k, v in r.items()})

