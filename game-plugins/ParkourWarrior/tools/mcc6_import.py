"""Import the MCC 6 Parkour Warrior replica ("Championship Parkour", 1.15.2, by Throw_Away_Account) 1:1 as the
Parkour Warrior map `mcc6_full`: every block of the world except the command machinery, written as a clean block file
plus a course.txt that holds the checkpoints the replica's command blocks implement (their exact boxes + respawns).

DATA ONLY. The world is untrusted downloaded data: it is parsed (never executed), no datapack / function / script is
read or run. Run it with `python -I` (stdlib only):

    python -I tools/mcc6_import.py <Championship Parkour>/region <out dir> [--drop-lime-glass]

writes   <out>/mcc6_full.mcmi.gz   MCMI v1 (palette + x y z + index; coordinates = the replica's world coordinates)
         <out>/course.txt          map description (see map/FullCourse.java)
         <out>/import_report.json  what was kept, converted and removed (+ the checks)
         <out>/mcc6_full.mcmdump.gz  preview dump (tools/render_preview.py)

What is removed (and nothing else):
  * command blocks (command / chain / repeating), structure blocks, jigsaw, barrier (the replica's invisible runtime markers)
  * the redstone that belongs to the command machinery: repeaters, comparators, wire, redstone blocks, pistons, pressure
    plates, buttons, levers and redstone torches that sit in a cluster with command blocks. Hoppers, lecterns,
    observers, lone levers / buttons / torches are course props and stay.
  * instruction / teleporter / credit signs (the 24 "Stage a-b <name>" signs and blank signs stay, with their text)
  * entities (paintings, the "Test" armor stand) and block entity data (banner patterns, chest contents)
  * the 4 stray credit-sign blocks 4200+ blocks away
Converted for 1.21.1: grass -> short_grass, grass_path -> dirt_path, cauldron[level] -> water_cauldron, walls (1.15 booleans -> none/low/tall).
"""
import gzip, json, os, re, struct, sys, collections

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mc6world import World  # noqa: E402

AIRS = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}
COMMAND = {"command_block", "chain_command_block", "repeating_command_block"}
ALWAYS_STRIP = COMMAND | {"structure_block", "jigsaw", "barrier", "structure_void"}
WIRED = re.compile(r"^(repeater|comparator|redstone_wire|redstone_block|piston|piston_head|sticky_piston|moving_piston|"
                   r".*_pressure_plate|.*_button|lever|redstone_wall_torch|redstone_torch)$")
MAX_X = 1000          # everything beyond is a stray credit sign
FALL_RE = re.compile(r"if entity @p\[y=(-?\d+),dy=(\d+)\]")


def short(state):
    return state.split("[", 1)[0].replace("minecraft:", "")


def props(state):
    if "[" not in state:
        return {}
    return dict(kv.split("=", 1) for kv in state[state.index("[") + 1:-1].split(","))


def mk(head, kv):
    return "minecraft:" + head + ("[" + ",".join("%s=%s" % (k, kv[k]) for k in sorted(kv)) + "]" if kv else "")


def read_world(region_dir):
    w = World(region_dir)
    blocks, tiles, ents = {}, [], []
    for ch in w.chunks():
        for sy, (pal, idx) in ch["sections"].items():
            x0, y0, z0 = ch["x"] * 16, sy * 16, ch["z"] * 16
            if idx is None:
                if pal[0] in AIRS:
                    continue
                for i in range(4096):
                    blocks[(x0 + (i & 15), y0 + (i >> 8), z0 + ((i >> 4) & 15))] = pal[0]
                continue
            for i, p in enumerate(idx):
                n = pal[p]
                if n in AIRS:
                    continue
                blocks[(x0 + (i & 15), y0 + (i >> 8), z0 + ((i >> 4) & 15))] = n
        tiles += ch["tiles"]
        ents += ch["ents"]
    return blocks, tiles, ents


# ------------------------------------------------------------------ command machinery

def machinery_cells(blocks):
    """Command blocks + the wired redstone in clusters that hold command blocks."""
    cmd = {k for k, v in blocks.items() if short(v) in COMMAND}
    wired = {k for k, v in blocks.items() if short(v) in COMMAND or WIRED.match(short(v))}
    seen, strip = set(), set()
    for k in wired:
        if k in seen:
            continue
        comp, q = [], [k]
        seen.add(k)
        while q:
            c = q.pop()
            comp.append(c)
            for dx in (-2, -1, 0, 1, 2):
                for dy in (-2, -1, 0, 1, 2):
                    for dz in (-2, -1, 0, 1, 2):
                        n = (c[0] + dx, c[1] + dy, c[2] + dz)
                        if n in wired and n not in seen:
                            seen.add(n)
                            q.append(n)
        if any(c in cmd for c in comp):
            strip.update(comp)
    return strip


def parse_commands(tiles):
    """Everything the command blocks implement that the plugin has to do itself. Parsed as text only."""
    cmds = []
    for t in tiles:
        if t.get("id") == "minecraft:command_block":
            cmds.append(((t["x"], t["y"], t["z"]), (t.get("Command") or "").strip()))
    trig_re = re.compile(r"^/?execute unless entity @p\[scores=\{Stage=(\d+)\}\] run execute if entity @p\[x=(-?\d+),y=(-?\d+),"
                         r"z=(-?\d+),dx=(-?\d+),dy=(-?\d+),dz=(-?\d+)\]$")
    tele_re = re.compile(r"^/?execute (?:unless entity @p\[scores=\{Stage=\d+\}\] run execute )?run teleport "
                         r"@e\[type=minecraft:armor_stand, name=Test\] (-?\d+) (-?\d+) (-?\d+) (-?\d+) 0$")
    fin_re = re.compile(r"^/?execute if entity @p\[x=(-?\d+),y=(-?\d+),z=(-?\d+),dx=(-?\d+),dy=(-?\d+),dz=(-?\d+)\] "
                        r"run execute if entity @p\[scores=\{finish=0\}\]$")
    triggers, teles, finish, fall = {}, [], None, None
    for pos, c in cmds:
        m = trig_re.match(c)
        if m:
            n = int(m.group(1))
            triggers.setdefault(n, []).append((pos, tuple(int(g) for g in m.groups()[1:])))
            continue
        m = tele_re.match(c)
        if m:
            teles.append((pos, tuple(int(g) for g in m.groups())))
            continue
        m = fin_re.match(c)
        if m:
            finish = (pos, tuple(int(g) for g in m.groups()))
            continue
        m = FALL_RE.search(c)
        if m and "teleport @p" in c:
            fall = (pos, int(m.group(1)), int(m.group(2)))
    return triggers, teles, finish, fall


def selector_box(x, y, z, dx, dy, dz):
    """Target selector volume: integer x/dx -> x .. x+dx+1 (dx >= 0) or x+dx .. x+1 (dx < 0); entity hitbox intersects."""
    def span(p, d):
        return (p, p + d + 1.0) if d >= 0 else (p + d, p + 1.0)
    (x0, x1), (y0, y1), (z0, z1) = span(x, dx), span(y, dy), span(z, dz)
    return (float(x0), float(y0), float(z0), float(x1), float(y1), float(z1))


# ------------------------------------------------------------------ 1.15 -> 1.21.1

PASSABLE_ABOVE = re.compile(r"torch|sign|banner|button|lever|tripwire|grass|fern|flower|tulip|poppy|dandelion|allium|bush|"
                            r"pressure_plate|rail|vine|redstone_wire|water|lava|fire|cobweb|sapling|rod|skull|head|bamboo|"
                            r"kelp|pickle|ladder|potted")


def convert(blocks):
    """Version migration; returns the new dict and per-kind counters."""
    out, stats = {}, collections.Counter()
    for k, s in blocks.items():
        h = short(s)
        if h == "grass":
            out[k] = "minecraft:short_grass"; stats["grass -> short_grass"] += 1
        elif h == "grass_path":
            kv = props(s)
            out[k] = mk("dirt_path", kv); stats["grass_path -> dirt_path"] += 1
        elif h == "cauldron":
            lvl = props(s).get("level", "0")
            out[k] = "minecraft:cauldron" if lvl == "0" else mk("water_cauldron", {"level": lvl})
            stats["cauldron[level] -> water_cauldron"] += 1
        elif h.endswith("_wall") and "east" in props(s) and props(s)["east"] in ("true", "false"):
            out[k] = s     # wait for the second pass (needs the block above)
        else:
            out[k] = s
    for k, s in list(out.items()):
        h = short(s)
        kv = props(s)
        if h.endswith("_wall") and kv.get("east") in ("true", "false"):
            above = out.get((k[0], k[1] + 1, k[2]))
            tall = False
            if above is not None:
                ah = short(above)
                tall = not PASSABLE_ABOVE.search(ah)
            nk = dict(kv)
            for d in ("north", "east", "south", "west"):
                nk[d] = ("tall" if tall else "low") if kv[d] == "true" else "none"
            out[k] = mk(h, nk); stats["wall boolean sides -> low/tall"] += 1
    return out, stats


# ------------------------------------------------------------------ signs

def sign_lines(t):
    res = []
    for kk in ("Text1", "Text2", "Text3", "Text4"):
        v = t.get(kk, "")
        try:
            j = json.loads(v)
            if isinstance(j, str):
                v = j
            elif isinstance(j, dict):
                v = j.get("text", "")
            elif isinstance(j, list):
                v = "".join(x if isinstance(x, str) else x.get("text", "") for x in j)
        except Exception:
            pass
        res.append(v)
    return res


def write_mcmi(path, blocks):
    names, idx, rows = [], {}, []
    for (x, y, z), s in sorted(blocks.items()):
        if s not in idx:
            idx[s] = len(names)
            names.append(s)
        rows.append((x, y, z, idx[s]))
    with gzip.open(path, "wb", compresslevel=9) as f:
        f.write(b"MCMI")
        f.write(struct.pack(">i", 1))
        f.write(struct.pack(">i", len(names)))
        for n in names:
            b = n.encode("utf-8")
            f.write(struct.pack(">H", len(b)))
            f.write(b)
        f.write(struct.pack(">i", len(rows)))
        buf = bytearray()
        for r in rows:
            buf += struct.pack(">hhhi", *r)
        f.write(bytes(buf))
    return len(names), len(rows)


def write_dump(path, blocks):
    mn = [min(k[i] for k in blocks) for i in range(3)]
    mx = [max(k[i] for k in blocks) for i in range(3)]
    with gzip.open(path, "wt", compresslevel=6) as f:
        f.write("MCMDUMP 1 %d %d %d\n" % tuple(mx[i] - mn[i] + 1 for i in range(3)))
        for (x, y, z), v in blocks.items():
            f.write("%d %d %d %s\n" % (x - mn[0], y - mn[1], z - mn[2], v))
    return mn


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    region, out = argv[0], argv[1]
    drop_lime = "--drop-lime-glass" in argv
    os.makedirs(out, exist_ok=True)
    blocks, tiles, ents = read_world(region)
    src_total = len(blocks)
    rep = {"source_blocks_total": src_total}
    stray = {k for k in blocks if k[0] > MAX_X or k[0] < -MAX_X}
    rep["stray_far_blocks_removed"] = sorted(stray)
    work = {k: v for k, v in blocks.items() if k not in stray}

    triggers, teles, finish, fall = parse_commands(tiles)
    # ---- machinery
    strip = machinery_cells(work)
    always = {k for k, v in work.items() if short(v) in ALWAYS_STRIP}
    removed_machinery = strip | always
    rep["removed_machinery"] = dict(collections.Counter(short(work[k]) for k in removed_machinery))
    kept = {k: v for k, v in work.items() if k not in removed_machinery}

    # ---- signs
    sign_tiles = {(t["x"], t["y"], t["z"]): sign_lines(t) for t in tiles if t.get("id") == "minecraft:sign"}
    keep_signs, drop_signs = {}, {}
    sec_re = re.compile(r"^Stage (\d)-(\d)$")
    sections = {}
    for pos, lines in sign_tiles.items():
        if pos not in kept:
            continue
        if sec_re.match(lines[1].strip()):
            keep_signs[pos] = lines
            m = sec_re.match(lines[1].strip())
            sections[(int(m.group(1)) - 1) * 3 + int(m.group(2))] = " ".join(x.strip() for x in lines[2:] if x.strip())
        elif not any(x.strip() for x in lines):
            keep_signs[pos] = lines     # blank decor signs
        else:
            drop_signs[pos] = lines
    for pos in drop_signs:
        del kept[pos]
    rep["signs_kept"] = {str(k): v for k, v in sorted(keep_signs.items())}
    rep["signs_removed"] = {str(k): " | ".join(v) for k, v in sorted(drop_signs.items())}
    assert len(sections) == 24, "expected 24 section signs, got %d" % len(sections)

    if drop_lime:
        lime = [k for k, v in kept.items() if "lime_stained_glass" in v]
        for k in lime:
            del kept[k]
        rep["lime_glass_removed"] = len(lime)

    # ---- migrate
    final, mig = convert(kept)
    rep["converted"] = dict(mig)
    rep["kept_blocks"] = len(final)

    # ---- checkpoints from the command blocks
    table = {}
    for pos, (x, y, z, yaw) in teles:
        if pos[1] == 28 and pos[2] == -41:          # the machine-room table: x = -51 - n  <->  sign "n" at y 29
            n = -51 - pos[0]
            table[n] = (x + 0.5, float(y), z + 0.5, float(yaw))
    assert sorted(table) == list(range(0, 24)), sorted(table)
    # cross-check with the copy next to each trigger
    near = {}
    for n, tr in triggers.items():
        tp = tr[0][0]
        best = min((p for p, _ in teles if not (p[1] == 28 and p[2] == -41)),
                   key=lambda p: (p[0] - tp[0]) ** 2 + (p[1] - tp[1]) ** 2 + (p[2] - tp[2]) ** 2)
        tele = [t for p, t in teles if p == best][0]
        near[n] = (tele[0] + 0.5, float(tele[1]), tele[2] + 0.5, float(tele[3]))
    mismatch = {n: (table[n], near[n]) for n in near if table[n] != near[n]}
    rep["respawn_table_vs_adjacent_teleport_mismatch"] = {str(k): v for k, v in mismatch.items()}
    assert sorted(triggers) == list(range(1, 24)), sorted(triggers)
    cps = []
    for n in range(1, 24):
        assert len(triggers[n]) == 1, (n, triggers[n])
        box = selector_box(*triggers[n][0][1])
        # the respawn is the teleport wired next to the trigger (the machine-room table is the replica's "next challenge" menu)
        cps.append({"n": n, "box": box, "respawn": near[n], "name": sections[n], "trigger_cmd": list(triggers[n][0][0])})
    start = table[0]
    fin_box = selector_box(*finish[1])
    fall_y = fall[1] + fall[2]
    rep["checkpoints"] = cps
    rep["start"] = start
    rep["finish_box"] = fin_box
    rep["fall_below_y"] = fall_y
    rep["fall_command_range_y"] = [fall[1], fall[1] + fall[2]]

    lines = []
    A = lines.append
    A("# Parkour Warrior map 'mcc6_full': the MCC 6 Parkour Warrior replica (\"Championship Parkour\", 1.15.2) imported 1:1.")
    A("# Generated by tools/mcc6_import.py from the downloaded world; PRIVATE third-party data, server-local only (never in git or the jar).")
    A("# Every block of the world except the command machinery (see the tool header). The replica's command blocks are replaced by")
    A("# the plugin: 'cp' lines are the boxes its command blocks test (exact selector volumes) and the respawn they teleport to.")
    A("format linear")
    A("map mcc6_full")
    A("title MCC 6 Replica (full)")
    A("blocks mcc6_full.mcmi.gz")
    A("credit MCC 6 Parkour Warrior replica \"Championship Parkour\" by Throw_Away_Account (Planet Minecraft), built by TryHardOfDoom")
    A("fall %d" % fall_y)
    A("start %.1f %.1f %.1f %.0f" % start)
    for i in range(1, 25):
        A("section %d %s" % (i, sections[i]))
    zones = ["Mesa", "Garden", "Jungle", "Space", "Ocean", "City", "Ice", "Nether"]
    for i, zn in enumerate(zones):
        A("zone %d %s" % (i + 1, zn))
    for c in cps:
        b = c["box"]
        A("cp %d box %g %g %g %g %g %g respawn %.1f %.1f %.1f %.0f" % ((c["n"],) + b + c["respawn"]))
    A("finish box %g %g %g %g %g %g" % fin_box)
    A("overview Course (top-down) -20 190 140 90")
    A("overview Start 7.5 40 -25 30")
    A("overview Final Tower -30 70 20 35")
    for pos, ln in sorted(keep_signs.items()):
        assert not any("|" in x for x in ln)
        A("sign %d %d %d %s" % (pos + ("|".join(ln),)))
    open(os.path.join(out, "course.txt"), "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")

    npal, nblk = write_mcmi(os.path.join(out, "mcc6_full.mcmi.gz"), final)
    mn = write_dump(os.path.join(out, "mcc6_full.mcmdump.gz"), final)
    rep["mcmi_palette"] = npal
    rep["mcmi_blocks"] = nblk
    rep["dump_offset"] = mn

    # glass / barrier census, source vs output
    def census(d):
        c = collections.Counter(short(v) for v in d.values())
        return {k: v for k, v in sorted(c.items()) if "glass" in k or k in ("barrier", "light")}
    rep["glass_barrier_source"] = census(blocks)
    rep["glass_barrier_output"] = census(final)
    # diff
    removed = [k for k in work if k not in final]
    rep["removed_total"] = len(removed) + len(stray)
    rep["entities_not_carried"] = dict(collections.Counter(e.get("id") for e in ents))
    # export the cleaned blocks for the offline diff
    json.dump(rep, open(os.path.join(out, "import_report.json"), "w"), indent=1, default=str)
    print("source blocks %d, kept %d, removed %d (machinery %d, signs %d, strays %d); converted %s" % (
        src_total, len(final), rep["removed_total"], len(removed_machinery), len(drop_signs), len(stray), dict(mig)))
    print("palette %d, file %d bytes" % (npal, os.path.getsize(os.path.join(out, "mcc6_full.mcmi.gz"))))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
