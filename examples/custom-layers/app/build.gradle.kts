import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Load file "keystore.properties" where we keep our keys
val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

try {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} catch (_: IOException) {
}

android {
    namespace = "org.ramani.example.custom_layers"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.ramani.example.custom_layers"
        minSdk = 25
        targetSdk = 36

        if (keystoreProperties.containsKey("MAPLIBRE_STYLE_URL")) {
            resValue(
                "string",
                "maplibre_style_url",
                keystoreProperties["MAPLIBRE_STYLE_URL"] as String
            )
        } else {
            println("NOTE: MAPLIBRE_STYLE_URL is not present, so we will use the default (demo tiles)")
            resValue("string", "maplibre_style_url", "https://demotiles.maplibre.org/style.json")
        }

        if (keystoreProperties.containsKey("MAPTILER_API_KEY")) {
            resValue("string", "maptiler_api_key", keystoreProperties["MAPTILER_API_KEY"] as String)
        } else {
            throw RuntimeException("You need to specify MAPTILER_API_KEY in the keystore.properties file!")
        }

        if (keystoreProperties.containsKey("THUNDERFOREST_API_KEY")) {
            resValue(
                "string",
                "thunderforest_api_key",
                keystoreProperties["THUNDERFOREST_API_KEY"] as String
            )
        } else {
            throw RuntimeException("You need to specify THUNDERFOREST_API_KEY in the keystore.properties file!")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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

dependencies {
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(platform("androidx.compose:compose-bom:2026.03.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("org.ramani-maps:ramani-maplibre:0.10.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.03.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
