#!/usr/bin/env python3
"""Extract the 21 old item-NBT command lines from loop.mcfunction.

Writes data/item_nbt_lines.csv with the exact old command and a documented
1.21 component form. Read-only on the source archive; no port is applied.
"""
import csv
import os
import re

SRC = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-pits-by-mickey-kat\TGTTOS Pits - By Mickey_Kat\datapacks\Pits\data\tgttos\functions\loop.mcfunction"
HERE = os.path.dirname(os.path.abspath(__file__))

LINE_RE = re.compile(r"^item replace entity (?P<target>@a(?:\[[^\]]*\])?) (?P<slot>\S+) with "
                     r"(?P<item>[a-z0-9_:]+)(?P<nbt>\{.*\}) (?P<count>\d+)$")


def main():
    lines = open(SRC, encoding="utf-8").read().splitlines()
    rows = []
    for i, line in enumerate(lines, 1):
        m = LINE_RE.match(line.strip())
        if not m:
            continue
        item = m.group("item")
        nbt = m.group("nbt")
        count = int(m.group("count"))
        keys = []
        if "CanDestroy" in nbt:
            keys.append("CanDestroy")
        if "CanPlaceOn" in nbt:
            keys.append("CanPlaceOn")
        if "Unbreakable" in nbt:
            keys.append("Unbreakable")
        if "Enchantments" in nbt:
            keys.append("Enchantments")
        destroy = re.search(r"CanDestroy:\[(.*?)\]", nbt)
        place = re.search(r"CanPlaceOn:\[(.*?)\]", nbt)
        destroy_ids = re.findall(r'"([^"]+)"', destroy.group(1)) if destroy else []
        place_ids = re.findall(r'"([^"]+)"', place.group(1)) if place else []
        if item == "iron_pickaxe":
            component = (f'iron_pickaxe[unbreakable={{}},enchantments={{levels:{{"minecraft:efficiency":2}}}},'
                         f'can_break={{blocks:[{",".join(chr(34)+x+chr(34) for x in destroy_ids)}]}}] {count}')
        else:
            component = f'{item}[can_place_on={{blocks:[{",".join(chr(34)+x+chr(34) for x in place_ids)}]}}] {count}'
        rows.append({
            "function": "loop.mcfunction",
            "line": i,
            "target": m.group("target"),
            "slot": m.group("slot"),
            "item": item,
            "count": count,
            "old_nbt_keys": "+".join(keys),
            "can_destroy_count": len(destroy_ids),
            "can_place_on_count": len(place_ids),
            "old_command": line.strip(),
            "component_form_compact": component,
            "note": ("2 then 1 in the same tick; final count is 1" if count == 1 and any(
                r["item"] == item and r["line"] == i - 1 and r["count"] == 2 for r in rows) else
                     "count 2 setter; overwritten by the next line" if count == 2 else "count 1 setter"),
        })
    with open(os.path.join(HERE, "item_nbt_lines.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=["function", "line", "target", "slot", "item", "count",
                                         "old_nbt_keys", "can_destroy_count", "can_place_on_count",
                                         "old_command", "component_form_compact", "note"])
        w.writeheader()
        w.writerows(rows)
    print("rows", len(rows))
    for r in rows:
        print(f"{r['line']:4d} {r['item']:22s} {r['count']} {r['old_nbt_keys']:40s} "
              f"destroy={r['can_destroy_count']} place={r['can_place_on_count']}  {r['note']}")


if __name__ == "__main__":
    main()
