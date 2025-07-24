# Using Ramani Maps with OpenFreeMap

Ramani Maps works very nicely with the [OpenFreeMap](https://www.openfreemap.org) project. There is one caveat: if using Ramani Maps, or indeed MapLibre Native in general, with OpenFreeMap styles, text and symbols do not render correctly on the Android emulator if you are using MapLibre Native 11.8.0+. This affects versions 0.9.0+ of Ramani Maps as they use these recent versions of MapLibre Native.

The rendering is, however, fine on real Android devices.

This is discussed in [issue #3648 on MapLibre Native](https://github.com/maplibre/maplibre-native/issues/3648) and appears to be due to reliability problems with OpenGL emulation. As discussed in this issue, a potential workaround is to use the Vulkan build of MapLibre Native.

By setting up an exclusion in your `build.gradle.kts`, you can force a Vulkan build of MapLibre Native to be used rather than the bundled OpenGL build. Do note that this is not guaranteed to work as it is not the same version of MapLibre Native that Ramani Maps was built with.

## Setting up version catalog

In your `libs.versions.toml` version catalog, you can add a specifier for `maplibreVulkan` in the `versions` section with a version matching the MapLibre Native version that your Ramani version is using, e.g:

```
maplibreVulkan = "11.11.0"
```

and then specify the library, e.g.

```
maplibre-vulkan = { group="org.maplibre.gl", name="android-sdk-vulkan", version.ref="maplibreVulkan" }
```

## Setting up build.gradle.kts

Then in your `build.gradle.kts`, include the Vulkan version of MapLibre Native, and specify that the bundled version of MapLibre should be *excluded* from the Ramani dependency, e.g.:

```
implementation(libs.maplibre.vulkan)
implementation(libs.ramanimaps) {
    exclude(group = "org.maplibre.gl", module = "android-sdk")
}
```
