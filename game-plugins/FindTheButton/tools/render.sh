#!/bin/sh
# Offline loop: compile the pure-Java map generators (no Paper), validate every hiding spot (reachability +
# pressability) and every staircase, write dumps, render previews into previews/.
# Usage: sh tools/render.sh [mapId...]      (NORENDER=1 skips the PNGs)
set -e
cd "$(dirname "$0")/.."
OUT=target/offline
rm -rf "$OUT/classes"; mkdir -p "$OUT/classes" previews
"/c/Users/USER/Desktop/java/bin/javac" -d "$OUT/classes" src/main/java/com/mcm/games/findthebutton/map/*.java tools/PreviewMain.java
"/c/Users/USER/Desktop/java/bin/java" -cp "$OUT/classes" PreviewMain "$OUT" "$@" | tee "$OUT/validation.txt"
[ -n "$NORENDER" ] && exit 0
R=/c/Users/USER/Desktop/Mcc/tools/render_preview.py
for f in "$OUT"/*_cut.mcmdump.gz; do
  id=$(basename "$f" _cut.mcmdump.gz)
  if [ $# -gt 0 ]; then case " $* " in *" $id "*) ;; *) continue;; esac; fi
  python "$R" "$OUT/${id}_cut.mcmdump.gz" "previews/${id}_cutaway.png" --angle 0 &
  python "$R" "$OUT/${id}_spots.mcmdump.gz" "previews/${id}_spots_top.png" --top &
  wait
done
