#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$script_dir"
chmod +x gradlew
gradle_bin=${GRADLE_BIN:-./gradlew}
"$gradle_bin" generateWasmGC
