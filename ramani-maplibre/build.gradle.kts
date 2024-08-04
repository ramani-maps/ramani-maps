import java.util.Properties
import java.io.FileInputStream
import java.io.IOException

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.parcelize)
    `maven-publish`
    signing
}

// Load file "keystore.properties" where we keep our keys
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

try {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} catch (ignored: IOException) {
}

android {
    namespace = "org.ramani.compose"
    compileSdk = 34

    defaultConfig {
        minSdk = 25

        group = "org.ramani-maps"
        version = "0.5.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)

    api(libs.maplibre.android.sdk)
    api(libs.maplibre.android.plugin.annotation)

    testImplementation(libs.junit)
}

if (keystoreProperties.containsKey("ossrhUsername") && keystoreProperties.containsKey("ossrhPassword")) {
    afterEvaluate {
        publishing {
            publications {
                create<MavenPublication>("release") {
                    from(components["release"])

                    pom {
                        name = "Ramani-Maplibre"
                        packaging = "aar"
                        description = "An Android Compose library to manipulate MapLibre maps."
                        url = "https://github.com/ramani-maps/ramani-maps"

                        scm {
                        connection = "scm:git:https://github.com/ramani-maps/ramani-maps"
                        developerConnection = "scm:git:https://github.com/ramani-maps/ramani-maps"
                        url = "https://github.com/ramani-maps/ramani-maps"
                        }

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
                    }
                }
            }
            repositories {
                maven {
                    url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

                    credentials {
                        username = keystoreProperties["ossrhUsername"] as String
                        password = keystoreProperties["ossrhPassword"] as String
                    }
                }
            }
        }

        signing {
            useGpgCmd()
            sign(publishing.publications["release"])
        }
    }
}

