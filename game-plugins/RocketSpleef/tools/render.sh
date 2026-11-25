#!/bin/sh
# Offline render loop for the three old-MCC Rocket Spleef maps: compile the pure-Java generators, dump, render PNGs
# into previews/.  Usage: sh tools/render.sh [mapId...]      (NORENDER=1 skips the PNGs)
set -e
cd "$(dirname "$0")/.."
OUT=target/offline
rm -rf "$OUT/classes"; mkdir -p "$OUT/classes" previews
"/c/Users/USER/Desktop/java/bin/javac" -d "$OUT/classes" src/main/java/com/mcm/games/rocketspleef/map/Canvas.java \
  src/main/java/com/mcm/games/rocketspleef/map/MapGen.java src/main/java/com/mcm/games/rocketspleef/map/*Map.java tools/PreviewMain.java
"/c/Users/USER/Desktop/java/bin/java" -cp "$OUT/classes" PreviewMain "$OUT" "$@"
[ -n "$NORENDER" ] && exit 0
R=/c/Users/USER/Desktop/Mcc/tools/render_preview.py
for f in "$OUT"/*_full.mcmdump.gz; do
  id=$(basename "$f" _full.mcmdump.gz)
  if [ -n "$*" ]; then case " $* " in *" $id "*) ;; *) continue;; esac; fi
  python "$R" "$f" "previews/${id}_iso_a0.png" --angle 0
  python "$R" "$f" "previews/${id}_iso_a2.png" --angle 2
  python "$R" "$f" "previews/${id}_top.png" --top
  python "$R" "$OUT/${id}_below.mcmdump.gz" "previews/${id}_below.png" --angle 0
  python "$R" "$OUT/${id}_side.mcmdump.gz" "previews/${id}_side.png" --top
done
