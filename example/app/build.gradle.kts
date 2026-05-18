import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

// Load file "keystore.properties" where we keep our keys
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

try {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} catch (_: IOException) {
}

val maplibreStyleUrl = keystoreProperties.getOrDefault(
    "MAPLIBRE_STYLE_URL", "https://demotiles.maplibre.org/style.json"
) as String
val maptilerApiKey = keystoreProperties.getOrDefault("MAPTILER_API_KEY", "") as String
val thunderforestApiKey = keystoreProperties.getOrDefault("THUNDERFOREST_API_KEY", "") as String

kotlin {
    androidTarget()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ExampleApp"
            isStatic = false
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation(compose.ui)
            implementation("org.ramani-maps:ramani-maplibre:0.11.0")
        }

        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.13.0")
            implementation("androidx.core:core-ktx:1.18.0")
        }
    }
}

compose.resources {
    generateResClass = never
}

android {
    namespace = "org.ramani.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.ramani.example"
        minSdk = 25
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "MAPLIBRE_STYLE_URL", "\"$maplibreStyleUrl\"")
        buildConfigField("String", "MAPTILER_API_KEY", "\"$maptilerApiKey\"")
        buildConfigField("String", "THUNDERFOREST_API_KEY", "\"$thunderforestApiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Generate a Kotlin file with API keys for commonMain
val generateApiKeys by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/apiKeys/kotlin")
    outputs.dir(outputDir)

    doLast {
        val dir = outputDir.get().asFile.resolve("org/ramani/example")
        dir.mkdirs()
        dir.resolve("GeneratedApiKeys.kt").writeText(
            """
            package org.ramani.example

            internal object ApiKeys {
                const val MAPLIBRE_STYLE_URL = "$maplibreStyleUrl"
                const val MAPTILER_API_KEY = "$maptilerApiKey"
                const val THUNDERFOREST_API_KEY = "$thunderforestApiKey"
            }
            """.trimIndent()
        )
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir(generateApiKeys.map { it.outputs.files.singleFile })
}
