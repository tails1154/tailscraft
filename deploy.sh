#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
source_dir="$script_dir/wasm_gc_teavm/javascript"
deploy_dir=${1:-/home/tails1154/ecraft/gaming/ecraft}

if [ ! -d "$deploy_dir" ]; then
	printf '%s\n' "Deployment directory does not exist: $deploy_dir" >&2
	exit 1
fi

# This project's Gradle build requires Java 21 on this host.
if [ -d /usr/lib/jvm/java-21-openjdk ]; then
	export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
	export PATH="$JAVA_HOME/bin:$PATH"
fi

printf '%s\n' "Building WASM GC and EPK..."
(
	cd "$script_dir/wasm_gc_teavm"
	sh ./CompileWASM.sh
	sh ./CompileEPK.sh
)

if [ ! -f "$source_dir/classes.wasm" ] || [ ! -f "$source_dir/assets.epk" ]; then
	printf '%s\n' "Missing compiled classes.wasm or assets.epk in $source_dir" >&2
	printf '%s\n' "Build did not produce the required artifacts." >&2
	exit 1
fi
if [ ! -d "$deploy_dir" ]; then
	printf '%s\n' "Deployment directory does not exist: $deploy_dir" >&2
	exit 1
fi

backup_dir=$(mktemp -d "${deploy_dir}.backup-full-XXXXXX")
cp -a "$deploy_dir/." "$backup_dir/"
cleanup() { rm -f "$deploy_dir/classes.wasm.new" "$deploy_dir/assets.epk.new"; }
trap cleanup EXIT HUP INT TERM

cp "$source_dir/classes.wasm" "$deploy_dir/classes.wasm.new"
cp "$source_dir/assets.epk" "$deploy_dir/assets.epk.new"
mv "$deploy_dir/classes.wasm.new" "$deploy_dir/classes.wasm"
mv "$deploy_dir/assets.epk.new" "$deploy_dir/assets.epk"
cmp "$source_dir/classes.wasm" "$deploy_dir/classes.wasm"
cmp "$source_dir/assets.epk" "$deploy_dir/assets.epk"

printf '%s\n' "Deployed classes.wasm and assets.epk to: $deploy_dir"
printf '%s\n' "Backup created at: $backup_dir"
sha256sum "$deploy_dir/classes.wasm" "$deploy_dir/assets.epk"
