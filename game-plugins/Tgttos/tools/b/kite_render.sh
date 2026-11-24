#!/bin/sh
# Kite Festival only: compile the model + B util + KiteCourse/KiteFestival + the Kite tools standalone (no Paper,
# none of the other B maps), validate (report: tools/b/out/kite_validation.txt), dump, render previews/.
# Usage: sh tools/b/kite_render.sh        (NORENDER=1 skips the PNGs)
cd "$(dirname "$0")/../.."
OUT=tools/b/out
CL=$OUT/kite_classes
J=/c/Users/USER/Desktop/java/bin
rm -rf "$CL"; mkdir -p "$CL" previews
"$J/javac" -encoding UTF-8 -d "$CL" src/main/java/com/mcm/games/tgttos/model/*.java \
  src/main/java/com/mcm/games/tgttos/maps/b/util/*.java src/main/java/com/mcm/games/tgttos/maps/b/Kite*.java \
  tools/b/KiteV.java tools/b/KiteGlide.java tools/b/KiteMain.java || exit 1
"$J/java" -Xmx3g -cp "$CL" KiteMain "$OUT" "$@"
RC=$?
[ -n "$NORENDER" ] && exit $RC
R=/c/Users/USER/Desktop/Mcc/tools/render_preview.py
D=$OUT/kite_festival.mcmdump.gz
python "$R" "$D" previews/kite_festival_top.png --top > /dev/null &
python "$R" "$D" previews/kite_festival_iso.png --angle 1 > /dev/null &
python "$R" "$D" previews/kite_festival_iso2.png --angle 3 > /dev/null &
python tools/b/kite_views.py "$D" "$OUT/kite_profile.csv" previews/kite_festival &
wait
exit $RC
