#!/bin/sh
# Offline Sky Blockle loot check (no Paper): compiles MapModel + the map package + LootPool/LootDeal with
# tools/LootCheck.java and deals the loot of real maps (2..10 wedges) over many seeds. Exit 2 on failure.
# Usage: sh tools/loot_check.sh [seeds]
set -e
cd "$(dirname "$0")/.."
OUT=tools/out/lootcheck
J=/c/Users/USER/Desktop/java/bin
YAML="$(cygpath -w "$HOME/.m2/repository/org/yaml/snakeyaml/2.2/snakeyaml-2.2.jar")"
rm -rf "$OUT"; mkdir -p "$OUT"
S=src/main/java/com/mcm/games/skyblockle
"$J/javac" -cp "$YAML" -d "$OUT" $S/MapModel.java $S/LootPool.java $S/LootDeal.java $S/map/*.java tools/LootCheck.java
"$J/java" -cp "$OUT;$YAML" LootCheck src/main/resources/loot.yml "${1:-200}"
