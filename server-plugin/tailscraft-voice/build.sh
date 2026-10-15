#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
PAPER_JAR="${PAPER_JAR:-$ROOT/../deps/spigot-api-1.12.2-R0.1-20180712.012057-156.jar}"
BUILD="$ROOT/build"

rm -rf "$BUILD"
mkdir -p "$BUILD/classes"
if [[ ! -f "$PAPER_JAR" ]]; then
  mkdir -p "$(dirname "$PAPER_JAR")"
  curl -fL --retry 3 -o "$PAPER_JAR" \
    "https://hub.spigotmc.org/nexus/repository/snapshots/org/spigotmc/spigot-api/1.12.2-R0.1-SNAPSHOT/spigot-api-1.12.2-R0.1-20180712.012057-156.jar"
fi

find "$ROOT/src/main/java" -name '*.java' -print0 | xargs -0 javac -source 8 -target 8 -encoding UTF-8 -cp "$PAPER_JAR" -d "$BUILD/classes"
cp "$ROOT/plugin.yml" "$BUILD/plugin.yml"
jar cfm "$BUILD/TailsCraftVoice.jar" "$ROOT/manifest.mf" -C "$BUILD/classes" . -C "$BUILD" plugin.yml
echo "$BUILD/TailsCraftVoice.jar"
