# Building MapLibre Native for iOS (for ramani-maps)

This document describes how the `libMapLibre.a` archives shipped in
`ramani-maplibre/libs/iosArm64/` and `ramani-maplibre/libs/iosSimulatorArm64/`
are built and processed, and why a non-trivial post-processing step is needed.

If you only want to regenerate the archives, skip to
[Build and install](#build-and-install).

---

## The problem

`ramani-maps` is a Compose Multiplatform wrapper around MapLibre Native. On
iOS, the example app (built with Kotlin/Native + Compose Multiplatform) was
crashing with `EXC_BAD_ACCESS` during text rendering as soon as a Compose
`Text` was laid out. The crash address decoded as an OpenType script tag
(`0x4c61746e00000014` = `"Latn" << 32 | 0x14`), pointing at a HarfBuzz struct
being interpreted with the wrong layout.

### Root cause

Compose Multiplatform's text engine uses Skia, which ships with its own copy
of HarfBuzz. MapLibre Native also vendors HarfBuzz statically. When both
end up in the same final iOS framework, the linker sees two copies of the
HarfBuzz symbols (including weak C++ template instantiations such as
`hb_lazy_loader_t<OT::fvar>::get_relaxed`) and silently picks one definition.

The picked definition is then called with the *other* library's struct
layouts. The two HarfBuzz copies have diverged enough internally (different
table indices, different field offsets) that the receiver immediately
dereferences a script tag as if it were a pointer, and crashes.

This is not specific to HarfBuzz. Any transitive dependency that MapLibre
vendors and that is also pulled in by another library in the consumer's
binary (Skia, ICU, libpng, etc.) can produce the same class of bug.

---

## The solution

Two things in combination:

1. **Compile MapLibre with `-fvisibility=hidden` and
   `-fvisibility-inlines-hidden`.** MapLibre's public API headers annotate
   public symbols with `MLN_EXPORT` (which expands to
   `__attribute__((visibility("default")))`). With the global default flipped
   to `hidden`, only those annotated symbols remain externally visible;
   everything else (MapLibre internals, vendored HarfBuzz, vendored ICU, etc.)
   becomes private-external.

2. **Post-process the static archive with `ld -r` to demote private-external
   symbols to non-external (local).** Private-external symbols are hidden
   from the dylib export table but are still link-visible within the same
   image — they can still satisfy weak-symbol references from other code
   linked into the same binary. `ld -r` without `-keep_private_externs`
   (the default) demotes them to truly local, after which the linker
   genuinely cannot use MapLibre's HarfBuzz to satisfy Skia's references.

The result: at consumer link time, MapLibre's vendored HarfBuzz is invisible
to Skia, Skia uses its own HarfBuzz, and each library calls its own struct
layouts. No collision, no crash. This works for any future transitive dep
without us having to enumerate it, because the rule is namespace-agnostic:
whatever the author marked as non-public stays internal.

---

## Build and install

### 1. Build MapLibre Native

```bash
cd /path/to/maplibre-native

bazel build --compilation_mode=opt \
    --copt=-DMLN_CUSTOM_COMBINED_BUNDLE=1 \
    --copt=-fvisibility=hidden \
    --cxxopt=-fvisibility=hidden \
    --cxxopt=-fvisibility-inlines-hidden \
    --//:renderer=metal \
    --ios_multi_cpus=sim_arm64,arm64 \
    //platform/ios:MapLibre.static
```

Flag-by-flag:

- `--compilation_mode=opt` — release build.
- `--copt=-DMLN_CUSTOM_COMBINED_BUNDLE=1` — applies the project's
  `maplibre-native-ios.patch`, which lets us consume the static archive
  outside Xcode.
- `--copt=-fvisibility=hidden` — global default for all C/C++/Obj-C
  compiles, including vendored deps.
- `--cxxopt=-fvisibility=hidden` — same, but for C++-only compiles
  (Bazel applies `copt` to everything, `cxxopt` only to C++; setting
  both is belt-and-braces).
- `--cxxopt=-fvisibility-inlines-hidden` — required to hide weak template
  instantiations (the actual cause of the HarfBuzz crash).
- `--//:renderer=metal` — Metal renderer (we don't use the OpenGL path).
- `--ios_multi_cpus=sim_arm64,arm64` — produces both device (arm64) and
  Apple-silicon simulator slices.

Output:

```
bazel-bin/platform/ios/MapLibre.static.xcframework.zip
```

### 2. Extract and install into ramani-maps

```bash
rm -rf /tmp/mln-new
unzip -o /path/to/maplibre-native/bazel-bin/platform/ios/MapLibre.static.xcframework.zip \
        -d /tmp/mln-new

# Device slice
cp /tmp/mln-new/MapLibre.xcframework/ios-arm64/MapLibre.framework/MapLibre \
   /path/to/ramani-maps/ramani-maplibre/libs/iosArm64/libMapLibre.a

# Simulator slice (xcframework names it ios-arm64_x86_64-simulator even though
# we only built sim_arm64 — that's the standard layout name)
cp /tmp/mln-new/MapLibre.xcframework/ios-arm64_x86_64-simulator/MapLibre.framework/MapLibre \
   /path/to/ramani-maps/ramani-maplibre/libs/iosSimulatorArm64/libMapLibre.a

# Drop any old .orig backups so the internalize script backs up the new files
rm -f /path/to/ramani-maps/ramani-maplibre/libs/iosArm64/libMapLibre.a.orig \
      /path/to/ramani-maps/ramani-maplibre/libs/iosSimulatorArm64/libMapLibre.a.orig
```

### 3. Run the internalize script

```bash
cd /path/to/ramani-maps
./internalize-libmaplibre.sh \
    ramani-maplibre/libs/iosArm64/libMapLibre.a \
    ramani-maplibre/libs/iosSimulatorArm64/libMapLibre.a
```

What the script does, per archive:

1. Backs up the original to `<archive>.orig` on first run (idempotent —
   subsequent runs re-process from the backup).
2. Picks the right platform string for `ld` based on whether the path
   contains "Simulator".
3. Runs `ld -r -force_load <backup> -o <merged.o>`. `-force_load` pulls
   every member object out of the archive; `ld -r` merges them into a
   single relocatable object file and (because we don't pass
   `-keep_private_externs`) demotes every private-external symbol to
   non-external (local). MapLibre's public API stays external because
   `MLN_EXPORT` overrode the global `-fvisibility=hidden` for those
   symbols.
4. Wraps the merged `.o` back into a static archive with
   `libtool -static`, replacing the input file in place.
5. Sanity-checks by counting any remaining externally-visible
   HarfBuzz/OpenType symbols — should always print `0`.

### 4. (If necessary) clear caches and rebuild

If you've changed the archive but your build still picks up an old
version, several caches need clearing:

```bash
rm -rf ~/Library/Developer/Xcode/DerivedData/iosApp-*
rm -rf /path/to/ramani-maps/ramani-maplibre/build \
       /path/to/ramani-maps/example/app/build
```

(`./gradlew :app:clean` is not enough — it does not touch DerivedData or
the Kotlin/Native cinterop cache.)

---

## Verification

The following `nm -m` probe shows the visibility distribution by symbol
namespace. Run it on `libMapLibre.a` *before* and *after* the internalize
script to confirm the post-processing did its job. Note: Apple's `nm -m`
uses "private external", not "private extern".

```bash
nm -m ramani-maplibre/libs/iosArm64/libMapLibre.a 2>/dev/null | awk '
  /\(undefined\)/ { next }
  / external | private external | non-external / {
    sym = $NF
    if      ($0 ~ / private external /) vis = "priv"
    else if ($0 ~ / non-external /)     vis = "loc"
    else                                vis = "ext"
    if      (sym ~ /^_hb_/)                       hb_c[vis]++
    else if (sym ~ /^__Z.*[0-9]+hb_/)             hb_cpp[vis]++
    else if (sym ~ /^__Z.*[0-9]+OT[0-9NIE]/)      ot_cpp[vis]++
    else if (sym ~ /^__Z.*[0-9]+mbgl/)            mbgl_cpp[vis]++
    else if (sym ~ /^_OBJC_CLASS_\$_MLN/)         cls[vis]++
    else if (sym ~ /^_MLN/)                       mln[vis]++
    else                                          other[vis]++
  }
  END {
    print "hb_* C        ext/priv/loc:", hb_c["ext"]+0,    hb_c["priv"]+0,    hb_c["loc"]+0
    print "hb_* C++      ext/priv/loc:", hb_cpp["ext"]+0,  hb_cpp["priv"]+0,  hb_cpp["loc"]+0
    print "OT::* C++     ext/priv/loc:", ot_cpp["ext"]+0,  ot_cpp["priv"]+0,  ot_cpp["loc"]+0
    print "mbgl::* C++   ext/priv/loc:", mbgl_cpp["ext"]+0,mbgl_cpp["priv"]+0,mbgl_cpp["loc"]+0
    print "MLN classes   ext/priv/loc:", cls["ext"]+0,     cls["priv"]+0,     cls["loc"]+0
    print "MLN*          ext/priv/loc:", mln["ext"]+0,     mln["priv"]+0,     mln["loc"]+0
    print "other         ext/priv/loc:", other["ext"]+0,   other["priv"]+0,   other["loc"]+0
  }'
```

Expected after `internalize-libmaplibre.sh`:

- `hb_*`, `OT::*`: 0 external, 0 private-external, everything local.
- `MLN classes` and `MLN*`: external counts roughly match the public-API
  surface (the `MLN_EXPORT`'d types); private-external is 0; the rest are
  local.
- `mbgl::*` and `other`: 0 private-external; bulk of symbols are now local.

If `hb_*` shows any external or private-external symbols after the script
runs, the archive was either built without the visibility flags or the
script was not actually applied.
