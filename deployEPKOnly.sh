#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
source_epk="$script_dir/wasm_gc_teavm/javascript/assets.epk"
deploy_dir=${1:-/home/tails1154/ecraft/gaming/ecraft}

if [ ! -f "$source_epk" ]; then
	printf '%s\n' "Missing compiled assets.epk: $source_epk" >&2
	printf '%s\n' "Run CompileEPK.sh first." >&2
	exit 1
fi
if [ ! -d "$deploy_dir" ]; then
	printf '%s\n' "Deployment directory does not exist: $deploy_dir" >&2
	exit 1
fi

backup_dir=$(mktemp -d "${deploy_dir}.backup-epk-XXXXXX")
cp -a "$deploy_dir/." "$backup_dir/"
cleanup() { rm -f "$deploy_dir/assets.epk.new"; }
trap cleanup EXIT HUP INT TERM

cp "$source_epk" "$deploy_dir/assets.epk.new"
mv "$deploy_dir/assets.epk.new" "$deploy_dir/assets.epk"
cmp "$source_epk" "$deploy_dir/assets.epk"

printf '%s\n' "Deployed assets.epk to: $deploy_dir"
printf '%s\n' "Backup created at: $backup_dir"
sha256sum "$deploy_dir/assets.epk"
