import java.util.Properties
import java.io.FileInputStream
import java.io.IOException

buildscript {
    dependencies {
        // Needed for jreleaser for some reason
        classpath("com.sun.activation:jakarta.activation:1.2.2")
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.compiler)

    alias(libs.plugins.jreleaser)
    `maven-publish`
}

// Load file "keystore.properties" where we keep our keys
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

try {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} catch (_: IOException) {
    if (project.hasProperty("centralUsername")) keystoreProperties["centralUsername"] = property("centralUsername")
    if (project.hasProperty("centralPassword")) keystoreProperties["centralPassword"] = property("centralPassword")
    if (project.hasProperty("gpgPass")) keystoreProperties["gpgPass"] = property("gpgPass")
}

kotlin {
    android {
        namespace = "org.ramani.compose"
        compileSdk = 36
        minSdk = 25

        withHostTestBuilder {}
        withDeviceTestBuilder {}
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.compose.runtime)
            implementation(libs.androidx.compose.runtime.saveable)
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
version = "0.11.0"

if (keystoreProperties.containsKey("centralUsername") && keystoreProperties.containsKey("centralPassword")) {
    publishing {
        publications.withType<MavenPublication> {
            pom {
                name = "Ramani-Maplibre"
                description = "A Compose Multiplatform library to manipulate MapLibre maps."
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
        repositories {
            maven {
               url = uri(layout.buildDirectory.dir("target/staging-deploy"))
            }
        }
    }

    jreleaser {
        signing {
            setActive("ALWAYS")
            pgp {
                armored.set(true)
                setMode("COMMAND")
                keystoreProperties["gpgPass"]?.let {
                    passphrase.set(it as String)
                }
            }
        }
        deploy {
            release {
                github {
                    skipRelease = true
                    skipTag = true
                }
            }
            maven {
                mavenCentral {
                    create("sonatype") {
                        verifyPom = false
                        setActive("RELEASE")
                        username = keystoreProperties["centralUsername"] as String
                        password = keystoreProperties["centralPassword"] as String
                        url = "https://central.sonatype.com/api/v1/publisher"
                        stagingRepository("build/target/staging-deploy")
                    }
                }
                nexus2 {
                    create("snapshot-deploy") {
                        verifyPom = false
                        setActive("SNAPSHOT")
                        snapshotUrl.set("https://central.sonatype.com/repository/maven-snapshots")
                        url = "https://central.sonatype.com/repository/maven-snapshots"
                        applyMavenCentralRules = true
                        snapshotSupported = true
                        username = keystoreProperties["centralUsername"] as String
                        password = keystoreProperties["centralPassword"] as String
                        stagingRepository("build/target/staging-deploy")
                    }
                }
            }
        }
    }
}
