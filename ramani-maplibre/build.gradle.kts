plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)

    alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
    android {
        namespace = "org.ramani.compose"
        compileSdk = 36
        minSdk = 25

        withHostTestBuilder {}
        withDeviceTestBuilder {}
    }

    val includeDir = project.file("libs/include").absolutePath

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        val frameworkDir = when (target.name) {
            "iosArm64" -> "iosArm64"
            "iosSimulatorArm64" -> "iosSimulatorArm64"
            else -> error("Unexpected target: ${target.name}")
        }
        val libPath = project.file("libs/$frameworkDir").absolutePath

        target.compilations["main"].cinterops {
            val MapLibre by creating {
                defFile(project.file("src/nativeInterop/cinterop/MapLibre.def"))
                compilerOpts("-I$includeDir")
                extraOpts("-libraryPath", libPath)
            }
        }

        target.binaries.all {
            linkerOpts("-ObjC")
            linkerOpts("-lbz2", "-lc++", "-lsqlite3", "-lz")
            linkerOpts(
                "-framework", "CoreGraphics",
                "-framework", "CoreImage",
                "-framework", "CoreLocation",
                "-framework", "CoreText",
                "-framework", "ImageIO",
                "-framework", "Metal",
                "-framework", "MetalKit",
                "-framework", "MobileCoreServices",
                "-framework", "QuartzCore",
                "-framework", "Security",
                "-framework", "SystemConfiguration",
                "-framework", "UIKit",
                "-framework", "WebKit",
            )
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.compose.runtime)
            implementation(libs.androidx.compose.runtime.saveable)
            implementation(compose.ui)

            // Exposed in the public queryRenderedFeatures return type (GeoJSON Feature),
            // so it must be `api`. Brings kotlinx-serialization-json (JsonObject) transitively.
            api(libs.spatialk.geojson)
        }

        androidMain.dependencies {
            implementation(libs.androidx.foundation)
            implementation(libs.androidx.material)
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.core.ktx)
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.coroutines.android)

            api(libs.maplibre.android.sdk)
            api(libs.maplibre.android.plugin.annotation)
            api(libs.okhttp)
        }

        val androidHostTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }

        iosMain.dependencies {
            implementation(compose.foundation)
        }

        val androidDeviceTest by getting {
            dependencies {
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}

group = "org.ramani-maps"

// Used for local builds and for the snapshot publishing from main. The release
// CI job overrides it with -PVERSION=x.y.z[-SNAPSHOT], extracted from the
// release tag (maplibre-x.y.z[-SNAPSHOT]).
val fallbackVersion = "0.13.0-SNAPSHOT"
version = project.findProperty("VERSION")?.toString() ?: fallbackVersion

// Workaround: Compose Multiplatform registers a resource-copy task for the
// androidDeviceTest variant but (with the AGP KMP library plugin) never
// configures its outputDirectory, failing connectedAndroidDeviceTest. There are
// no Android Compose resources to copy (only iosMain has composeResources), so
// disabling the task is safe.
tasks.matching { it.name == "copyAndroidDeviceTestComposeResourcesToAndroidAssets" }
    .configureEach { enabled = false }

mavenPublishing {
    publishToMavenCentral()
    if (System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null) {
        signAllPublications()
    }
    coordinates(group.toString(), "ramani-maplibre", version.toString())
    pom {
        name = "Ramani-Maplibre"
        description = "A Compose Multiplatform library to manipulate MapLibre maps."
        inceptionYear = "2023"
        url = "https://github.com/ramani-maps/ramani-maps"
        licenses {
            license {
                name = "Mozilla Public License 2.0"
                url = "https://spdx.org/licenses/MPL-2.0.html"
            }
        }
        developers {
            developer {
                id = "romanbapst"
                name = "Roman Bapst"
                email = "bapstroman@gmail.com"
            }
            developer {
                id = "jonasvautherin"
                name = "Jonas Vautherin"
                email = "dev@jonas.vautherin.ch"
            }
        }
        scm {
            connection = "scm:git:https://github.com/ramani-maps/ramani-maps"
            developerConnection = "scm:git:https://github.com/ramani-maps/ramani-maps"
            url = "https://github.com/ramani-maps/ramani-maps"
        }
    }
}
