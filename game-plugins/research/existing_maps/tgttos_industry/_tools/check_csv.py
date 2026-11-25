import csv
import os

d = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\tgttos_industry\data"


def check(name, minrows=0):
    with open(os.path.join(d, name), encoding="utf-8") as f:
        rows = list(csv.reader(f))
    widths = {len(r) for r in rows}
    print(f"{name}: rows={len(rows)-1}, widths={widths}")
    assert len(rows) - 1 >= minrows, name
    assert len(widths) == 1, (name, widths)
    return rows


b = check("blocks.csv", 39)
total = sum(int(r[1]) for r in b[1:])
print("  block total", total)
assert total == 122276, total
check("entities.csv", 24)
check("checkpoints.csv", 2)
check("signs.csv", 1)
check("datapack_functions.csv", 200)
check("timeline.csv", 10)
check("command_blocks.csv", 0)
print("all csv checks passed")
