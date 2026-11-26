#!/bin/bash
# usage: validate.sh [lobby-src-dir]  -> compiles the hub generator + HubValidate and walks every staircase.
# Exit code 0 = no staircase failures, no connection mismatches, no floating carpets.
set -e
cd /c/Users/LeonS/Desktop/Mcc/tools/lobby_preview
J='/c/Users/LeonS/Desktop/java/bin'
SRC=${1:-../../plugin-src/com/mcm/lobby}
OUT=out_validate
rm -rf $OUT; mkdir -p $OUT
EXTRA=""
[ -f "$SRC/Shapes.java" ] || EXTRA="../../plugin-src/com/mcm/lobby/Shapes.java ../../plugin-src/com/mcm/lobby/BlockFix.java"
"$J/javac" -nowarn -d $OUT $(ls $SRC/*.java | grep -v HubManager.java) $EXTRA HubValidate.java
"$J/java" -Xmx6g -cp $OUT com.mcm.lobby.HubValidate
