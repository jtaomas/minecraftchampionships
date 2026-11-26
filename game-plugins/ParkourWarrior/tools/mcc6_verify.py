"""Block-by-block diff of the replica world against the Parkour Warrior world the plugin built from the imported map.

    python -I tools/mcc6_verify.py <Championship Parkour>/region <server>/mcc_parkourwarrior/region [report.json]

Every block of the source world is classed as: identical, converted (1.15 -> 1.21.1 migration), removed on purpose
(command machinery / instruction signs / stray credit signs) or DIFFERENT; every block of the built world that the source
does not have is EXTRA. Data only (the source world is never executed); run with `python -I`.
"""
import collections, json, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mcc6_import as imp  # noqa: E402
from mc6world import modern_chunks  # noqa: E402


def read_built(region_dir):
    blocks = {}
    for cx, cz, secs in modern_chunks(region_dir):
        for sy, (pal, idx) in secs.items():
            x0, y0, z0 = cx * 16, sy * 16, cz * 16
            if idx is None:
                if pal[0] in imp.AIRS:
                    continue
                for i in range(4096):
                    blocks[(x0 + (i & 15), y0 + (i >> 8), z0 + ((i >> 4) & 15))] = pal[0]
                continue
            for i, p in enumerate(idx):
                n = pal[p]
                if n in imp.AIRS:
                    continue
                blocks[(x0 + (i & 15), y0 + (i >> 8), z0 + ((i >> 4) & 15))] = n
    return blocks


def norm(s):
    """Property order does not matter; the server writes them sorted."""
    if "[" not in s:
        return s
    h, p = s[:-1].split("[", 1)
    return h + "[" + ",".join(sorted(p.split(","))) + "]"


def sub_props(built, want):
    """Same block; the newer version only added default properties (leaves / lantern / skull waterlogged, powered ...)."""
    if "[" in built and "[" not in want:
        return built.split("[", 1)[0] == want
    if "[" not in want or "[" not in built:
        return built == want
    if built.split("[", 1)[0] != want.split("[", 1)[0]:
        return False
    bp = dict(kv.split("=", 1) for kv in built[built.index("[") + 1:-1].split(","))
    wp = dict(kv.split("=", 1) for kv in want[want.index("[") + 1:-1].split(","))
    return all(bp.get(k) == v for k, v in wp.items())


def main(argv):
    src_region, built_region = argv[0], argv[1]
    out = argv[2] if len(argv) > 2 else None
    blocks, tiles, ents = imp.read_world(src_region)
    stray = {k for k in blocks if abs(k[0]) > imp.MAX_X}
    work = {k: v for k, v in blocks.items() if k not in stray}
    strip = imp.machinery_cells(work) | {k for k, v in work.items() if imp.short(v) in imp.ALWAYS_STRIP}
    sign_tiles = {(t["x"], t["y"], t["z"]): imp.sign_lines(t) for t in tiles if t.get("id") == "minecraft:sign"}
    import re
    sec = re.compile(r"^Stage (\d)-(\d)$")
    drop_signs = {p for p, l in sign_tiles.items() if p in work and p not in strip and not sec.match(l[1].strip()) and any(x.strip() for x in l)}
    expected, _ = imp.convert({k: v for k, v in work.items() if k not in strip and k not in drop_signs})
    built = read_built(built_region)

    cat = collections.Counter()
    diff_examples = collections.defaultdict(list)
    by_type = collections.defaultdict(collections.Counter)
    for k, v in blocks.items():
        b = built.get(k)
        if k in stray or k in strip or k in drop_signs:
            why = "removed: stray credit sign" if k in stray else "removed: command machinery" if k in strip else "removed: instruction sign"
            if b is None:
                cat[why] += 1
            else:
                cat["DIFFERENT: should be gone but present"] += 1
                diff_examples["present"].append((k, v, b))
            continue
        if b is not None and norm(b) == norm(v):
            cat["identical"] += 1
        elif b is not None and sub_props(b, v):
            cat["identical (+ default properties the newer version adds, e.g. waterlogged=false)"] += 1
            by_type["default_props_added"][imp.short(v)] += 1
        elif b is not None and sub_props(b, expected[k]):
            cat["converted (1.15 -> 1.21.1 migration)"] += 1
            by_type["converted"][imp.short(v) + " -> " + imp.short(expected[k])] += 1
        else:
            cat["DIFFERENT"] += 1
            by_type["DIFFERENT"][imp.short(v) + " -> " + (imp.short(b) if b else "air")] += 1
            if len(diff_examples["different"]) < 40:
                diff_examples["different"].append((k, v, b))
    extra = {k: v for k, v in built.items() if k not in blocks}
    cat["EXTRA (built, not in source)"] = len(extra)
    for k, v in extra.items():
        by_type["EXTRA"][imp.short(v)] += 1
    kept = len(blocks) - sum(n for c, n in cat.items() if c.startswith("removed"))
    same = cat["identical"] + cat["identical (+ default properties the newer version adds, e.g. waterlogged=false)"]
    ident = same + cat["converted (1.15 -> 1.21.1 migration)"]
    res = {
        "source_blocks": len(blocks), "built_blocks": len(built), "categories": dict(cat),
        "kept_blocks_expected": kept,
        "pct_exact_string_identical_of_kept": round(100.0 * cat["identical"] / kept, 3),
        "pct_same_block_of_kept": round(100.0 * same / kept, 3),
        "pct_identical_or_converted_of_kept": round(100.0 * ident / kept, 3),
        "pct_of_source_all": round(100.0 * cat["identical"] / len(blocks), 3),
        "by_type": {k: dict(v.most_common(25)) for k, v in by_type.items()},
        "examples": {k: [(list(a), b, c) for a, b, c in v[:20]] for k, v in diff_examples.items()},
        "glass_barrier_source": {k: v for k, v in sorted(collections.Counter(imp.short(x) for x in blocks.values()).items()) if "glass" in k or k == "barrier"},
        "glass_barrier_built": {k: v for k, v in sorted(collections.Counter(imp.short(x) for x in built.values()).items()) if "glass" in k or k == "barrier"},
    }
    print(json.dumps(res, indent=1, default=str))
    if out:
        json.dump(res, open(out, "w"), indent=1, default=str)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
