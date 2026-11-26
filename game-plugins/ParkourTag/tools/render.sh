#!/bin/sh
# Offline: compile the pure-Java maps + PtPreview, run route checks, dump, render PNGs into previews/.
# Usage: sh tools/render.sh [mapId...]   (NORENDER=1 skips the PNGs)
set -e
cd "$(dirname "$0")/.."
OUT=target/offline
rm -rf "$OUT/classes"; mkdir -p "$OUT/classes" previews
"/c/Users/LeonS/Desktop/java/bin/javac" -d "$OUT/classes" src/main/java/com/mcm/games/parkourtag/map/*.java tools/PtPreview.java
"/c/Users/LeonS/Desktop/java/bin/java" -cp "$OUT/classes:src/main/resources" PtPreview "$OUT" "$@" || echo "ROUTE CHECK FAILED"
[ -n "$NORENDER" ] && exit 0
R=/c/Users/LeonS/Desktop/Mcc/tools/render_preview.py
for f in "$OUT"/*_full.mcmdump.gz; do
  id=$(basename "$f" _full.mcmdump.gz)
  if [ -n "$*" ]; then case " $* " in *" $id "*) ;; *) continue;; esac; fi
  python "$R" "$OUT/${id}_full.mcmdump.gz" "previews/${id}_iso_a0.png" --angle 0
  python "$R" "$OUT/${id}_pit.mcmdump.gz" "previews/${id}_iso_a2.png" --angle 2
  python "$R" "$OUT/${id}_pit.mcmdump.gz" "previews/${id}_top.png" --top --scale 12
done
