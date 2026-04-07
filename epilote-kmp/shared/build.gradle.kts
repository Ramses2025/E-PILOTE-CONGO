plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.coroutines.core)
                implementation(libs.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.auth)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.couchbase.lite.android)
                implementation(libs.ktor.client.android)
                implementation(libs.coroutines.android)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.couchbase.lite.java)
                implementation(libs.ktor.client.java)
                implementation(libs.coroutines.swing)
            }
        }
    }
}

android {
    namespace = "cg.epilote.shared"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
