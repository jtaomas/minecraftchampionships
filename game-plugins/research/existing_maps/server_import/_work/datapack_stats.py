import json
import re
from pathlib import Path

SERVER = Path(r"C:\Users\LeonS\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\review")
PACKS = {
    "mcc_tgttos_badlands": "tgttos",
    "mcc_tgttos_industry": "Industry",
    "mcc_tgttos_pits": "Pits",
}
scan = json.load(open(Path(__file__).parent / "scan.json", encoding="utf-8"))
for world, pack in PACKS.items():
    root = SERVER / world / "datapacks" / pack
    print("=" * 20, world, pack)
    print("folder", root)
    mcmeta = json.loads((root / "pack.mcmeta").read_text(encoding="utf-8"))
    print("pack_format", mcmeta.get("pack", {}).get("pack_format"), "desc", mcmeta.get("pack", {}).get("description"))
    funcs = sorted(root.rglob("*.mcfunction"))
    total_lines = 0
    item_nbt = 0
    scoreboard = 0
    tag = 0
    for f in funcs:
        lines = [ln for ln in f.read_text(encoding="utf-8", errors="replace").splitlines() if ln.strip() and not ln.strip().startswith("#")]
        total_lines += len(lines)
        for ln in lines:
            if re.search(r"\b(?:item replace|give)\b[^\n]*\b[a-z_:]+\{[^}]*\}", ln):
                item_nbt += 1
            if ln.strip().startswith("scoreboard"):
                scoreboard += 1
            if ln.strip().startswith("tag "):
                tag += 1
    print("functions", len(funcs), "lines", total_lines, "item_nbt", item_nbt, "scoreboard", scoreboard, "tag", tag)
    for f in funcs:
        print("  ", f.relative_to(root))
    for tagfile in sorted((root / "data" / "minecraft" / "tags").rglob("*.json")):
        print("  tag", tagfile.relative_to(root), tagfile.read_text(encoding="utf-8").replace("\n", " ")[:200])
    print("level enabled", scan[world]["level"]["datapacks"])
