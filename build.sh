#!/usr/bin/env bash
set -Eeuo pipefail

readonly PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly WASM_DIR="${PROJECT_DIR}/wasm_gc_teavm"
readonly EMSDK_DIR="${EMSDK_DIR:-${HOME}/.local/share/emsdk}"
readonly EMSDK_VERSION="3.1.58"

build_loader=true
case "${1:-}" in
	"") ;;
	--skip-loader) build_loader=false ;;
	-h|--help)
		printf 'Usage: %s [--skip-loader]\n' "$0"
		exit 0
		;;
	*)
		printf 'Unknown option: %s\n' "$1" >&2
		exit 2
		;;
esac

if [[ ! -f "${EMSDK_DIR}/emsdk_env.sh" ]]; then
	printf 'Emscripten SDK not found at %s\n' "${EMSDK_DIR}" >&2
	printf 'Install it with: git clone https://github.com/emscripten-core/emsdk.git %s\n' "${EMSDK_DIR}" >&2
	printf 'Then run: %s/emsdk install %s && %s/emsdk activate %s\n' \
		"${EMSDK_DIR}" "${EMSDK_VERSION}" "${EMSDK_DIR}" "${EMSDK_VERSION}" >&2
	exit 1
fi

if [[ ! -d "${WASM_DIR}" ]]; then
	printf 'WASM-GC project directory not found: %s\n' "${WASM_DIR}" >&2
	exit 1
fi

cd "${WASM_DIR}"

export EMSDK_QUIET=1
# shellcheck source=/dev/null
source "${EMSDK_DIR}/emsdk_env.sh"

actual_emcc_version="$(emcc -dumpversion 2>/dev/null || true)"
if [[ "${actual_emcc_version}" != "${EMSDK_VERSION}" ]]; then
	printf 'Expected Emscripten %s, but emcc reports %s\n' \
		"${EMSDK_VERSION}" "${actual_emcc_version:-unknown}" >&2
	printf 'Run: %s/emsdk activate %s\n' "${EMSDK_DIR}" "${EMSDK_VERSION}" >&2
	exit 1
fi

run_stage() {
	local name="$1"
	local script="$2"
	printf '\n==> %s\n' "${name}"
	bash "${script}"
}

run_stage "Compile assets" "CompileEPK.sh"

if [[ "${build_loader}" == true ]]; then
	run_stage "Compile loader with Emscripten ${EMSDK_VERSION}" "CompileLoaderWASM.sh"
fi

run_stage "Compile game WASM" "CompileWASM.sh"
run_stage "Compile runtime JavaScript" "CompileEagRuntimeJS.sh"
run_stage "Compile bootstrap JavaScript" "CompileBootstrapJS.sh"
run_stage "Create client bundle" "MakeWASMClientBundle.sh"

printf '\nBuild complete: %s\n' "${WASM_DIR}/javascript_dist"
