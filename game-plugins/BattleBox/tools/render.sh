#!/bin/sh
# Offline render loop: compile the pure-Java generators, validate staircases + gates + routes (fails on a broken one),
# run the sightline check (report in target/offline/sightlines.txt), dump, render PNGs into previews/.
# Usage: sh tools/render.sh [themeId...]      (NORENDER=1 skips the PNGs)
set -e
cd "$(dirname "$0")/.."
OUT=target/offline
rm -rf "$OUT/classes"; mkdir -p "$OUT/classes" previews
"/c/Users/USER/Desktop/java/bin/javac" -d "$OUT/classes" src/main/java/com/mcm/games/battlebox/arena/*.java tools/PreviewMain.java tools/Sightlines.java tools/Routes.java
"/c/Users/USER/Desktop/java/bin/java" -cp "$OUT/classes" PreviewMain "$OUT" "$@"
[ -n "$NORENDER" ] && exit 0
R=/c/Users/USER/Desktop/Mcc/tools/render_preview.py
for f in "$OUT"/*_full.mcmdump.gz; do
  id=$(basename "$f" _full.mcmdump.gz)
  if [ $# -gt 0 ]; then case " $* " in *" $id "*) ;; *) continue;; esac; fi
  python "$R" "$OUT/${id}_full.mcmdump.gz" "previews/${id}_iso_a0.png" --angle 0 &
  python "$R" "$OUT/${id}_open.mcmdump.gz" "previews/${id}_iso_a2.png" --angle 2 &
  python "$R" "$OUT/${id}_cut.mcmdump.gz" "previews/${id}_top.png" --top &
  python "$R" "$OUT/${id}_cut.mcmdump.gz" "previews/${id}_cutaway.png" --angle 0 &
  python "$R" "$OUT/${id}_floor.mcmdump.gz" "previews/${id}_floorplan.png" --top &
  wait
done
