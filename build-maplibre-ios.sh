#!/usr/bin/env bash
# Build maplibre-native for iOS (arm64 device + arm64 simulator) with hidden
# visibility, then run internalize-libmaplibre.sh on the resulting archives.
#
# Output:
#   <out>/iosArm64/libMapLibre.a
#   <out>/iosSimulatorArm64/libMapLibre.a
#
# Usage:
#   build-maplibre-ios.sh [--ref <git-ref>] [--out <dir>] [--workdir <dir>]
#
# Defaults:
#   --ref:     contents of .maplibre-native-version at repo root
#   --out:     ramani-maplibre/libs
#   --workdir: a fresh temp directory (kept on failure for debugging)

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ref=""
out_dir="$repo_root/ramani-maplibre/libs"
workdir=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --ref)     ref="$2"; shift 2 ;;
        --out)     out_dir="$2"; shift 2 ;;
        --workdir) workdir="$2"; shift 2 ;;
        *) echo "unknown arg: $1" >&2; exit 2 ;;
    esac
done

[[ -n "$ref" ]] || ref="$(cat "$repo_root/.maplibre-native-version")"

patch_file="$repo_root/maplibre-native-ios.patch"
internalize="$repo_root/internalize-libmaplibre.sh"

# Fail fast — better than dying 40 minutes into a bazel build.
[[ "$(uname -s)" == "Darwin" ]]  || { echo "must run on macOS";   exit 1; }
command -v bazel      >/dev/null || { echo "bazel not found";     exit 1; }
command -v xcodebuild >/dev/null || { echo "Xcode not found";     exit 1; }
[[ -f "$patch_file"  ]]          || { echo "missing $patch_file"; exit 1; }
[[ -x "$internalize" ]]          || { echo "missing $internalize";exit 1; }

cleanup_workdir=0
if [[ -z "$workdir" ]]; then
    workdir="$(mktemp -d -t maplibre-ios-build.XXXXXX)"
    cleanup_workdir=1
fi
echo ">>> workdir: $workdir"
echo ">>> ref:     $ref"
echo ">>> out:     $out_dir"

src="$workdir/maplibre-native"

# Full clone, not --depth=1: bazel's repo rules occasionally stumble on
# shallow clones when resolving vendored submodule SHAs.
if [[ ! -d "$src/.git" ]]; then
    git clone https://github.com/maplibre/maplibre-native "$src"
fi
git -C "$src" fetch --tags origin
git -C "$src" reset --hard
git -C "$src" clean -fdx
git -C "$src" checkout "$ref"
git -C "$src" submodule update --init --recursive

# Idempotent patch apply: if it reverse-applies cleanly the patch is already
# in, so skip; otherwise apply forward.
if ! git -C "$src" apply --reverse --check "$patch_file" 2>/dev/null; then
    git -C "$src" apply "$patch_file"
fi

# Flag set per BUILD_MAPLIBRE_NATIVE_IOS.md.
( cd "$src" && bazel build --compilation_mode=opt \
    --copt=-DMLN_CUSTOM_COMBINED_BUNDLE=1 \
    --copt=-fvisibility=hidden \
    --cxxopt=-fvisibility=hidden \
    --cxxopt=-fvisibility-inlines-hidden \
    --//:renderer=metal \
    --ios_multi_cpus=sim_arm64,arm64 \
    //platform/ios:MapLibre.static //platform/ios:resources )

zip="$src/bazel-bin/platform/ios/MapLibre.static.xcframework.zip"
[[ -f "$zip" ]] || { echo "xcframework not produced"; exit 1; }

extract="$workdir/xcframework"
rm -rf "$extract" && mkdir -p "$extract"
unzip -q -o "$zip" -d "$extract"

mkdir -p "$out_dir/iosArm64" "$out_dir/iosSimulatorArm64"
cp "$extract/MapLibre.xcframework/ios-arm64/MapLibre.framework/MapLibre" \
   "$out_dir/iosArm64/libMapLibre.a"
cp "$extract/MapLibre.xcframework/ios-arm64_x86_64-simulator/MapLibre.framework/MapLibre" \
   "$out_dir/iosSimulatorArm64/libMapLibre.a"

# Headers are arch-independent; take them from the device slice.
device_fw="$extract/MapLibre.xcframework/ios-arm64/MapLibre.framework"

# Public headers (incl. the generated MapLibre.h umbrella) consumed by the
# cinterop .def. Clean stale headers first, but keep the tracked .gitkeep.
include_dir="$out_dir/include/MapLibre"
mkdir -p "$include_dir"
find "$include_dir" -type f ! -name '.gitkeep' -delete
cp -R "$device_fw/Headers/." "$include_dir/"

# MapLibre's runtime resource bundle: the compiled localizations (*.lproj, as
# binary .strings/.stringsdict) and the compiled asset catalog (Assets.car), plus
# a tiny bundle-identity Info.plist. The MapLibre.static xcframework carries NO
# resources, and the //platform/ios:resources target only assembles a full
# Mapbox.bundle when linked into a framework/app, which we never build. But the
# static-lib build leaves an intermediate Mapbox.bundle containing exactly the two
# compiled artifacts we need (*.lproj + Assets.car). Info.plist is not produced
# there, so we ship a vendored copy. These are packaged into the app and located
# at runtime by MapLibreInitializer.kt.
bundle_src=""
while IFS= read -r cand; do
    if [[ -f "$cand/Assets.car" ]]; then bundle_src="$cand"; break; fi
done < <(find "$src/bazel-bin/platform/ios" -type d -name 'Mapbox.bundle')
if [[ -z "$bundle_src" ]]; then
    echo "could not find a Mapbox.bundle (with Assets.car) under $src/bazel-bin/platform/ios" >&2
    find "$src/bazel-bin/platform/ios" -type d -name 'Mapbox.bundle' >&2 || true
    exit 1
fi

vendored_plist="$repo_root/maplibre-ios-resources/Info.plist"
[[ -f "$vendored_plist" ]] || { echo "missing vendored $vendored_plist"; exit 1; }

res_dir="$repo_root/ramani-maplibre/src/iosMain/composeResources/files"
mkdir -p "$res_dir"
# Make writable first: bazel outputs are read-only, so a previous copy can leave
# read-only dirs that rm -rf cannot clear.
chmod -R u+w "$res_dir"
find "$res_dir" -mindepth 1 -maxdepth 1 ! -name '.gitkeep' -exec rm -rf {} +

# Compiled localizations + asset catalog from the intermediate bundle, then the
# vendored bundle-identity plist.
find "$bundle_src" -type d -name '*.lproj' -exec cp -R {} "$res_dir/" \;
cp "$bundle_src/Assets.car" "$res_dir/"
cp "$vendored_plist" "$res_dir/Info.plist"

# Bazel marks its outputs read-only; Compose's resource-prep tasks must recreate
# these under build/, so restore write permission on the copied tree.
chmod -R u+w "$res_dir"

# Wipe stale .orig backups so internalize re-snapshots the fresh archives.
rm -f "$out_dir/iosArm64/libMapLibre.a.orig" \
      "$out_dir/iosSimulatorArm64/libMapLibre.a.orig"

"$internalize" \
    "$out_dir/iosArm64/libMapLibre.a" \
    "$out_dir/iosSimulatorArm64/libMapLibre.a"

(( cleanup_workdir )) && rm -rf "$workdir"

echo ">>> done"
echo "    $out_dir/iosArm64/libMapLibre.a"
echo "    $out_dir/iosSimulatorArm64/libMapLibre.a"
echo "    $include_dir/ (headers)"
echo "    $res_dir/ (resources)"
