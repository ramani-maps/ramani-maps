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

# MapLibre's runtime resource bundle has three parts, each from a different source
# (verified by building //platform/ios:MapLibre.static and inspecting the result):
#   - Assets.car: the compiled asset catalog. The static-lib build is the only
#     thing that produces it, nested as xcassets/Assets.car inside an intermediate
#     Mapbox.bundle. (The static xcframework itself ships NO resources, and
#     //platform/ios:resources only materializes a full bundle when linked into a
#     framework/app, which we never build.)
#   - *.lproj: localizations. The static-lib build does NOT bundle these at all, so
#     we take them straight from maplibre's source tree. They are plain
#     .strings/.stringsdict, which NSBundle reads at runtime (text or compiled).
#   - Info.plist: bundle identity; never produced by the static build, so vendored.
# All three land flat in the Compose resources dir and are located at runtime by
# MapLibreInitializer.kt.
#
# Assets.car is nested under xcassets/, so locate it recursively; pick the first
# Mapbox.bundle that contains one.
bundle_src=""
assets_car=""
while IFS= read -r cand; do
    found="$(find "$cand" -type f -name 'Assets.car' -print -quit)"
    if [[ -n "$found" ]]; then bundle_src="$cand"; assets_car="$found"; break; fi
done < <(find "$src/bazel-bin/platform/ios" -type d -name 'Mapbox.bundle')
if [[ -z "$bundle_src" ]]; then
    echo "could not find a Mapbox.bundle containing Assets.car under $src/bazel-bin/platform/ios" >&2
    find "$src/bazel-bin/platform/ios" -type d -name 'Mapbox.bundle' >&2 || true
    exit 1
fi
echo ">>> Assets.car from: $assets_car"

lproj_src="$src/platform/ios/resources"
[[ -d "$lproj_src" ]] || { echo "missing localization source $lproj_src"; exit 1; }

vendored_plist="$repo_root/maplibre-ios-resources/Info.plist"
[[ -f "$vendored_plist" ]] || { echo "missing vendored $vendored_plist"; exit 1; }

res_dir="$repo_root/ramani-maplibre/src/iosMain/composeResources/files"
mkdir -p "$res_dir"
# Make writable first: bazel outputs are read-only, so a previous copy can leave
# read-only dirs that rm -rf cannot clear.
chmod -R u+w "$res_dir"
find "$res_dir" -mindepth 1 -maxdepth 1 ! -name '.gitkeep' -exec rm -rf {} +

# Assemble the flat runtime layout: Assets.car (built) + *.lproj (source) +
# Info.plist (vendored), all at the resources root.
cp "$assets_car" "$res_dir/Assets.car"
find "$lproj_src" -maxdepth 1 -type d -name '*.lproj' -exec cp -R {} "$res_dir/" \;
cp "$vendored_plist" "$res_dir/Info.plist"

# Fail loudly (with the bundle's real tree) if the expected pieces didn't land,
# rather than silently shipping an incomplete bundle.
lproj_count="$(find "$res_dir" -maxdepth 1 -type d -name '*.lproj' | wc -l | tr -d ' ')"
if [[ ! -f "$res_dir/Assets.car" || "$lproj_count" -eq 0 ]]; then
    echo "resource assembly incomplete (Assets.car present: $([[ -f "$res_dir/Assets.car" ]] && echo yes || echo no), lproj dirs: $lproj_count)" >&2
    echo "bundle tree was:" >&2
    find "$bundle_src" >&2
    exit 1
fi

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
