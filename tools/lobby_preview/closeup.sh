#!/bin/bash
# usage: closeup.sh TAG [lobby-src-dir]  -> shape-accurate close-ups (stairs, fences, panes, walls, carpets)
#        into previews/lobby/TAG_*.png  (each block is expanded to R^3 micro-voxels by HubCloseup)
set -e
cd /c/Users/LeonS/Desktop/Mcc/tools/lobby_preview
J='/c/Users/LeonS/Desktop/java/bin'
TAG=$1
SRC=${2:-../../plugin-src/com/mcm/lobby}
OUT=out_closeup
rm -rf $OUT; mkdir -p $OUT
"$J/javac" -nowarn -d $OUT $(ls $SRC/*.java | grep -v HubManager.java) HubCloseup.java
R=8
VIEWS="domestairs:-9:70:-10:9:80:1 gatestairs:-8:67:34:8:77:43 pier:29:62:188:39:67:205 beach:28:60:150:40:70:164 clock:-17:88:-10:17:103:-8 lighthouse:108:100:110:124:106:126 truss:-12:100:-28:12:109:-24 aisle:0:62:-38:16:82:-18"
"$J/java" -Xmx6g -cp $OUT HubCloseup $OUT $R $VIEWS ${CLOSEUP_EXTRA}
P=../previews/lobby
for vw in $VIEWS; do
  n=${vw%%:*}
  A=""; [ "$n" = aisle ] && A="--angle 2"
  python ../render_preview.py $OUT/$n.mcmdump.gz $P/${TAG}_${n}.png $A ${RENDER_ARGS} > /dev/null
done
echo rendered
