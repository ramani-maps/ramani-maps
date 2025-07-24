# Using Vulkan as a rendering back-end 

It is possible with some mapping providers (for example [OpenFreeMap](https://www.openfreemap.org)) that you may encounter problems rendering text and symbols on an emulator with certain versions of MapLibre GL Native, and by extension, Ramani Maps.

This is discussed in [issue #3648 on MapLibre Native](https://github.com/maplibre/maplibre-native/issues/3648) and appears to be due to reliability problems with OpenGL emulation. As discussed in this issue, a potential workaround is to use [Vulkan](https://www.vulkan.org) as a back-end, via the Vulkan build of MapLibre Native.

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
