#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
PAPER_JAR="${PAPER_JAR:-$ROOT/../deps/spigot-api-1.12.2-R0.1-20180712.012057-156.jar}"
BUILD="$ROOT/build"
rm -rf "$BUILD"
mkdir -p "$BUILD/classes"
find "$ROOT/src/main/java" -name '*.java' -print0 | xargs -0 javac -source 8 -target 8 -encoding UTF-8 -cp "$PAPER_JAR" -d "$BUILD/classes"
cp "$ROOT/plugin.yml" "$BUILD/plugin.yml"
jar cfm "$BUILD/TailsCraftCameras.jar" "$ROOT/manifest.mf" -C "$BUILD/classes" . -C "$BUILD" plugin.yml
echo "$BUILD/TailsCraftCameras.jar"
