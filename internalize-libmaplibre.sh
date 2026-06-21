#!/usr/bin/env bash
#
# Demote MapLibre's internal/vendored symbols to local so they cannot collide
# with Compose/Skia's HarfBuzz (or any other transitive dep) at consumer link
# time. Requires libMapLibre.a to be built with -fvisibility=hidden and
# -fvisibility-inlines-hidden, so MapLibre's own code and vendored deps are
# marked as private-external in the archive. `ld -r` then merges all members
# into one relocatable object and (by default) demotes private-external symbols
# to non-external. The MapLibre public Obj-C/C API stays external because it is
# annotated with MLN_EXPORT (visibility=default), which overrides the global
# -fvisibility=hidden.
#
# Build MapLibre with, e.g.:
#   bazel build --compilation_mode=opt \
#     --copt=-DMLN_CUSTOM_COMBINED_BUNDLE=1 \
#     --copt=-fvisibility=hidden \
#     --cxxopt=-fvisibility=hidden \
#     --cxxopt=-fvisibility-inlines-hidden \
#     --//:renderer=metal \
#     --ios_multi_cpus=sim_arm64,arm64 \
#     //platform/ios:MapLibre.static
#
# Idempotent: preserves a .orig backup on first run, re-processes from the
# backup on subsequent runs.
#
# Usage:
#   ./internalize-libmaplibre.sh <libMapLibre.a> [<libMapLibre.a> ...]

set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 <libMapLibre.a> [<libMapLibre.a> ...]" >&2
  exit 1
fi

for INPUT in "$@"; do
  if [ ! -f "$INPUT" ]; then
    echo "Not found: $INPUT" >&2
    exit 1
  fi

  BACKUP="${INPUT}.orig"
  if [ ! -f "$BACKUP" ]; then
    cp "$INPUT" "$BACKUP"
    echo "Backed up original to: $BACKUP"
  fi

  TMPDIR=$(mktemp -d)
  PRELINKED="$TMPDIR/MapLibre.prelinked.o"

  case "$INPUT" in
    *[Ss]imulator*) LD_PLATFORM="ios-simulator" ;;
    *)              LD_PLATFORM="ios" ;;
  esac

  echo "Internalizing $INPUT (platform: $LD_PLATFORM) ..."

  xcrun ld -r \
    -arch arm64 \
    -platform_version "$LD_PLATFORM" 14.0 18.0 \
    -force_load "$BACKUP" \
    -o "$PRELINKED"

  xcrun libtool -static -o "$INPUT" "$PRELINKED"
  rm -rf "$TMPDIR"

  # Sanity: hb_* and OT::* should have no external symbols left.
  REMAINING=$(nm "$INPUT" 2>/dev/null | awk '
    $2 ~ /^[TDRSBW]$/ && ($3 ~ /^_hb_/ || $3 ~ /^__Z.*[0-9]+hb_/ || $3 ~ /^__Z.*[0-9]+OT[0-9NIE]/) { n++ }
    END { print n+0 }')
  echo "  externally-visible hb_*/OT::* symbols remaining: $REMAINING (target: 0)"
done

echo "Done."
