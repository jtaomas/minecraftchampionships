#!/bin/sh
# Offline loop for the B maps: compile the pure-Java map package + model standalone (no Paper), validate
# (report: tools/b/out/validation.txt), dump, render previews into previews/.
# Usage: sh tools/b/render.sh [mapId ...]      (NORENDER=1 skips the PNGs)
cd "$(dirname "$0")/../.."
OUT=tools/b/out
J=/c/Users/USER/Desktop/java/bin
rm -rf "$OUT/classes"; mkdir -p "$OUT/classes" previews
"$J/javac" -encoding UTF-8 -d "$OUT/classes" src/main/java/com/mcm/games/tgttos/model/*.java \
  src/main/java/com/mcm/games/tgttos/maps/b/util/*.java src/main/java/com/mcm/games/tgttos/maps/b/*.java tools/b/*.java || exit 1
"$J/java" -Xmx3g -cp "$OUT/classes" PreviewMain "$OUT" "$@"
RC=$?
[ -n "$NORENDER" ] && exit $RC
R=/c/Users/USER/Desktop/Mcc/tools/render_preview.py
V=tools/b/views.py
IDS="$@"; [ -z "$IDS" ] && IDS="ticket_gates"
for id in $IDS; do
  D=$OUT/$id.mcmdump.gz
  C=$D; [ -f "$OUT/${id}_cut.mcmdump.gz" ] && C=$OUT/${id}_cut.mcmdump.gz
  python "$R" "$C" previews/${id}_top.png --top > /dev/null &
  python "$R" "$C" previews/${id}_iso.png --angle 1 > /dev/null &
  python "$R" "$C" previews/${id}_iso2.png --angle 3 > /dev/null &
  python "$V" "$D" previews/$id &
  wait
done
exit $RC
