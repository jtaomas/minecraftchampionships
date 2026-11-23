#!/bin/sh
# Builder-A offline loop: compile the contract (model) + maps.a (pure Java, no Paper), run all validators
# (report: tools/a/out/validation.txt, exit 2 on problems), dump, and render previews into previews/.
# Usage: sh tools/a/render.sh [mapId ...]      (NORENDER=1 skips the PNGs; default: all four maps)
cd "$(dirname "$0")/../.."
OUT=tools/a/out
J=/c/Users/USER/Desktop/java/bin
SEED=${SEED:-20261124}
rm -rf "$OUT/classes"; mkdir -p "$OUT/classes" previews
"$J/javac" -encoding UTF-8 -d "$OUT/classes" src/main/java/com/mcm/games/tgttos/model/*.java \
  src/main/java/com/mcm/games/tgttos/maps/a/util/*.java src/main/java/com/mcm/games/tgttos/maps/a/*.java tools/a/PreviewMain.java || exit 1
ALL="laundry_line:LaundryLineMap dam_face:DamFaceMap bookshelf_run:BookshelfRunMap orchard_drop:OrchardDropMap"
SEL=""
for e in $ALL; do
  id=${e%%:*}; cls=${e##*:}
  if [ $# -eq 0 ] || echo " $* " | grep -q " $id "; then SEL="$SEL com.mcm.games.tgttos.maps.a.$cls"; IDS="$IDS $id"; fi
done
"$J/java" -Xmx3g -cp "$OUT/classes" PreviewMain "$OUT" "$SEED" $SEL
RC=$?
[ -n "$NORENDER" ] && exit $RC
R=/c/Users/USER/Desktop/Mcc/tools/render_preview.py
V=tools/a/view.py
for id in $IDS; do
  python "$R" "$OUT/$id.mcmdump.gz" "previews/${id}_top.png" --top &
  python "$R" "$OUT/${id}_routes.mcmdump.gz" "previews/${id}_routes_top.png" --top &
  python "$R" "$OUT/$id.mcmdump.gz" "previews/${id}_iso.png" --angle ${ANGLE:-1} &
  python "$V" side "$OUT/$id.mcmdump.gz" "previews/${id}_side.png" --scale 4 &
  python "$V" persp "$OUT/${id}_go.mcmdump.gz" "$OUT/$id.cam.json" "previews/${id}_start_view.png" &
  wait
done
exit $RC
