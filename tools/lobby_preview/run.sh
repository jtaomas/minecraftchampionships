#!/bin/bash
# usage: run.sh tag  -> regenerates dumps + renders standard views into previews/lobby/<tag>_*.png
set -e
cd /c/Users/USER/Desktop/Mcc/tools/lobby_preview
J='/c/Users/USER/Desktop/java/bin'
TAG=$1
rm -rf out/*.class out/com
(cd ../../plugin-src/com/mcm/lobby && "$J/javac" -d ../../../../tools/lobby_preview/out HNoise.java Vox.java HubLayout.java Glyphs.java Draw.java Trees.java Shapes.java BlockFix.java HubGenerator.java HubVenue.java HubPlaza.java HubSights.java ../../../../tools/lobby_preview/HubPreview.java)
"$J/java" -Xmx6g -cp out HubPreview out island:-215:48:-235:215:200:215 venue:-75:55:-135:75:185:12 plaza:-62:60:24:62:120:128 cut:-50:58:-112:50:150:-62 cutq:-50:58:-112:0:105:-62 ${EXTRA}
P=../previews/lobby
python ../render_preview.py out/island.mcmdump.gz $P/${TAG}_island_a0.png > /dev/null
python ../render_preview.py out/island.mcmdump.gz $P/${TAG}_island_a2.png --angle 2 > /dev/null
python ../render_preview.py out/island.mcmdump.gz $P/${TAG}_island_top.png --top --scale 3 > /dev/null
python ../render_preview.py out/venue.mcmdump.gz $P/${TAG}_venue_a0.png > /dev/null
python ../render_preview.py out/venue.mcmdump.gz $P/${TAG}_venue_a1.png --angle 1 > /dev/null
python ../render_preview.py out/plaza.mcmdump.gz $P/${TAG}_plaza_a0.png > /dev/null
python ../render_preview.py out/plaza.mcmdump.gz $P/${TAG}_plaza_a3.png --angle 3 > /dev/null
python ../render_preview.py out/cut.mcmdump.gz $P/${TAG}_interior_a0.png > /dev/null
python ../render_preview.py out/cutq.mcmdump.gz $P/${TAG}_interior_q.png --angle 1 > /dev/null
echo rendered
