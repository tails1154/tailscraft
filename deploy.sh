#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
source_dir="$script_dir/wasm_gc_teavm/javascript"
source_runtime="$source_dir/eagruntime.js"
source_bundle="$script_dir/wasm_gc_teavm/javascript_dist/assets.epw"
updater_template="$script_dir/tailscraft-updater.js"
update_manifest_template="$script_dir/update.json.template"
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

printf '%s\n' "Building WASM GC, runtime JS, and EPK..."
(
	cd "$script_dir/wasm_gc_teavm"
	sh ./CompileWASM.sh
	sh ./CompileEagRuntimeJS.sh
	sh ./CompileEPK.sh
	sh ./MakeWASMClientBundle.sh
)

if [ ! -f "$source_dir/classes.wasm" ] || [ ! -f "$source_runtime" ] || [ ! -f "$source_dir/assets.epk" ] || [ ! -f "$source_bundle" ]; then
	printf '%s\n' "Missing compiled client artifacts in $source_dir or javascript_dist" >&2
	printf '%s\n' "Build did not produce the required artifacts." >&2
	exit 1
fi
if [ ! -f "$updater_template" ] || [ ! -f "$update_manifest_template" ]; then
	printf '%s\n' "Missing updater metadata templates." >&2
	exit 1
fi
if [ ! -d "$deploy_dir" ]; then
	printf '%s\n' "Deployment directory does not exist: $deploy_dir" >&2
	exit 1
fi

backup_dir=$(mktemp -d "${deploy_dir}.backup-full-XXXXXX")
cp -a "$deploy_dir/." "$backup_dir/"
commit=$(git -C "$script_dir" rev-parse HEAD)
cleanup() { rm -f "$deploy_dir/classes.wasm.new" "$deploy_dir/eagruntime.js.new" "$deploy_dir/assets.epk.new" "$deploy_dir/assets.epw.new" "$deploy_dir/tailscraft-updater.js.new" "$deploy_dir/update.json.new"; }
trap cleanup EXIT HUP INT TERM

cp "$source_dir/classes.wasm" "$deploy_dir/classes.wasm.new"
cp "$source_runtime" "$deploy_dir/eagruntime.js.new"
cp "$source_dir/assets.epk" "$deploy_dir/assets.epk.new"
cp "$source_bundle" "$deploy_dir/assets.epw.new"
cp "$updater_template" "$deploy_dir/tailscraft-updater.js.new"
cp "$update_manifest_template" "$deploy_dir/update.json.new"
sed -i "s/__TAILSCRAFT_COMMIT__/$commit/g" "$deploy_dir/tailscraft-updater.js.new" "$deploy_dir/update.json.new"
if [ -f "$deploy_dir/index.html" ]; then
	# Keep the updater script cache-busted without replacing the site's existing index.
	sed -i -E "s#tailscraft-updater\\.js\\?v=[^\"']*#tailscraft-updater.js?v=$commit#g" "$deploy_dir/index.html"
fi
mv "$deploy_dir/classes.wasm.new" "$deploy_dir/classes.wasm"
mv "$deploy_dir/eagruntime.js.new" "$deploy_dir/eagruntime.js"
mv "$deploy_dir/assets.epk.new" "$deploy_dir/assets.epk"
mv "$deploy_dir/assets.epw.new" "$deploy_dir/assets.epw"
mv "$deploy_dir/tailscraft-updater.js.new" "$deploy_dir/tailscraft-updater.js"
mv "$deploy_dir/update.json.new" "$deploy_dir/update.json"
cmp "$source_dir/classes.wasm" "$deploy_dir/classes.wasm"
cmp "$source_runtime" "$deploy_dir/eagruntime.js"
cmp "$source_dir/assets.epk" "$deploy_dir/assets.epk"
cmp "$source_bundle" "$deploy_dir/assets.epw"
grep -F "currentCommit = \"$commit\"" "$deploy_dir/tailscraft-updater.js" >/dev/null
grep -F "\"commit\": \"$commit\"" "$deploy_dir/update.json" >/dev/null

printf '%s\n' "Deployed classes.wasm, eagruntime.js, assets.epk, and assets.epw to: $deploy_dir"
printf '%s\n' "Backup created at: $backup_dir"
sha256sum "$deploy_dir/classes.wasm" "$deploy_dir/eagruntime.js" "$deploy_dir/assets.epk" "$deploy_dir/assets.epw"
