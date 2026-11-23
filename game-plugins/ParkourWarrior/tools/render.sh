#!/bin/sh
# Offline Parkour Warrior course loop: compile the pure-Java map package + CourseModel standalone (no Paper),
# run the validators (report: tools/out/validation.txt), dump, render PNGs into previews/.
# Usage: sh tools/render.sh [filter-regex]   (NORENDER=1 skips the PNGs; the regex limits which renders run)
cd "$(dirname "$0")/.."
OUT=tools/out
J=/c/Users/USER/Desktop/java/bin
rm -rf "$OUT/classes"; mkdir -p "$OUT/classes" previews
"$J/javac" -encoding UTF-8 -d "$OUT/classes" src/main/java/com/mcm/games/parkourwarrior/CourseModel.java \
  src/main/java/com/mcm/games/parkourwarrior/map/*.java tools/PreviewMain.java || exit 1
"$J/java" -Xmx3g -cp "$OUT/classes" com.mcm.games.parkourwarrior.map.PreviewMain "$OUT" > "$OUT/run.log" 2>&1
RC=$?
grep -E "RESULT|SUMMARY|ERROR|Exception|at com" "$OUT/run.log" | head -80
[ -n "$NORENDER" ] && exit $RC
R=/c/Users/USER/Desktop/Mcc/tools/render_preview.py
F="${1:-.}"
grep -E "$F" "$OUT/renders.txt" > "$OUT/sel.txt"
while read d png rest; do
  python "$R" "$OUT/dumps/$d" "previews/$png" $rest > /dev/null 2>&1 &
done < "$OUT/sel.txt"
wait
if echo "full" | grep -qE "$F"; then python tools/side_view.py "$OUT/dumps/full.mcmdump.gz" previews/course_side.png 2; fi
exit $RC
