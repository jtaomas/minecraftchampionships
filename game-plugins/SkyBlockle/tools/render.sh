#!/bin/sh
# Offline Sky Blockle map loop: compile the pure-Java map package + MapModel standalone (no Paper), run every
# validator for 2..10 sides (report: tools/out/validation.txt, fails with exit 2), dump, render PNGs.
# Usage: sh tools/render.sh [wedges] [seed]        (NORENDER=1 skips the PNGs)
set -e
cd "$(dirname "$0")/.."
OUT=tools/out
J=/c/Users/USER/Desktop/java/bin
rm -rf "$OUT/classes"; mkdir -p "$OUT/classes" previews
"$J/javac" -d "$OUT/classes" src/main/java/com/mcm/games/skyblockle/MapModel.java \
  src/main/java/com/mcm/games/skyblockle/map/*.java tools/PreviewMain.java
"$J/java" -Xmx2g -cp "$OUT/classes" PreviewMain "$OUT" "$@"
[ -n "$NORENDER" ] && exit 0
R=/c/Users/USER/Desktop/Mcc/tools/render_preview.py
P=previews
python "$R" "$OUT/map_full.mcmdump.gz" "$P/map_top.png" --top &
python "$R" "$OUT/map_full.mcmdump.gz" "$P/map_iso_a0.png" --angle 0 &
python "$R" "$OUT/map_full.mcmdump.gz" "$P/map_iso_a2.png" --angle 2 &
wait
python "$R" "$OUT/wedge0.mcmdump.gz" "$P/wedge_iso.png" --angle 1 &
python "$R" "$OUT/start0.mcmdump.gz" "$P/start_island.png" --angle 1 &
python "$R" "$OUT/castle.mcmdump.gz" "$P/castle_exterior.png" --angle 0 &
python "$R" "$OUT/castle_cut.mcmdump.gz" "$P/castle_cutaway.png" --angle 0 &
python "$R" "$OUT/castle_slice.mcmdump.gz" "$P/castle_section.png" --angle 3 &
wait
