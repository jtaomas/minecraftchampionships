#!/bin/sh
# Offline loop for Frozen Canal + Quarry Steps only: compile the pure-Java model + B maps + B tools standalone (no Paper)
# into tools/b/out_cq/classes (separate from the shared render.sh), validate (report:
# tools/b/out/canal_quarry_validation.txt), dump, render previews into previews/.
# Usage: sh tools/b/canal_quarry_render.sh [frozen_canal] [quarry_steps]     (NORENDER=1 skips the PNGs)
cd "$(dirname "$0")/../.."
OUT=tools/b/out
CL=tools/b/out_cq/classes
J=/c/Users/USER/Desktop/java/bin
rm -rf "$CL"; mkdir -p "$CL" "$OUT" previews
B=src/main/java/com/mcm/games/tgttos/maps/b
"$J/javac" -encoding UTF-8 -d "$CL" src/main/java/com/mcm/games/tgttos/model/*.java $B/util/*.java \
  $B/FrozenCanal.java $B/Canal*.java $B/Quarry*.java tools/b/CanalQuarryMain.java tools/b/CanalV.java tools/b/QuarryV.java || exit 1
"$J/java" -Xmx3g -cp "$CL" CanalQuarryMain "$OUT" "$@"
RC=$?
[ -n "$NORENDER" ] && exit $RC
R=/c/Users/USER/Desktop/Mcc/tools/render_preview.py
V=tools/b/views.py
IDS="$@"; [ -z "$IDS" ] && IDS="frozen_canal quarry_steps"
for id in $IDS; do
  D=$OUT/$id.mcmdump.gz
  T=$D; [ -f "$OUT/${id}_decoys.mcmdump.gz" ] && T=$OUT/${id}_decoys.mcmdump.gz
  python "$R" "$T" previews/${id}_top.png --top > /dev/null &
  python "$R" "$D" previews/${id}_iso.png --angle 1 > /dev/null &
  python "$R" "$D" previews/${id}_iso2.png --angle 3 > /dev/null &
  python "$V" "$D" previews/$id &
  wait
done
exit $RC
