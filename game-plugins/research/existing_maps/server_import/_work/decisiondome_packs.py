import json
import zipfile
from pathlib import Path

SERVER = Path(r"C:\Users\LeonS\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\review")
root = SERVER / "mcc_decisiondome" / "datapacks"
for p in sorted(root.iterdir()):
    if p.is_dir():
        meta = p / "pack.mcmeta"
        print(p.name, "DIR", meta.exists(), meta.read_text(encoding="utf-8", errors="replace")[:200] if meta.exists() else "")
    elif p.suffix == ".zip":
        try:
            z = zipfile.ZipFile(p)
            metas = [n for n in z.namelist() if n.endswith("pack.mcmeta")]
            print(p.name, "ZIP", metas, z.read(metas[0]).decode("utf-8", "replace")[:200] if metas else "")
        except Exception as exc:
            print(p.name, "ERR", exc)
