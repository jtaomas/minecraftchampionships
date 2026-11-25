import sys
sys.path.insert(0, r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\tgttos_industry\_tools")
import scan_fast as s

live = r"C:\Users\LeonS\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\review\mcc_tgttos_industry"
res = s.dump(s.os.path.join(live, "region"), do_blocks=True, do_entities=False)
print("live total", sum(res["hist"].values()), "states", len(res["hist"]), "chunks", res["chunks"])
print("bbox", res["bbox"])
