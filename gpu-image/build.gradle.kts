plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        androidTarget {
            publishLibraryVariants()
        }
        jvm()
        js {
            browser()
        }
        val commonMain by getting {
            dependencies {
                api("com.danielgergely.kgl:kgl:0.6.2")
                implementation("org.qinetik:logger:1.0.16")
            }
        }
        val androidMain by getting {
            dependencies {
                api("com.danielgergely.kgl:kgl-android:0.6.2")
            }
        }
        val jvmMain by getting {
            dependencies {

                api("com.danielgergely.kgl:kgl-lwjgl:0.6.2")
            }
        }
    }
}

android {
    namespace="org.qinetik.gpuimage"
    compileSdk = 33
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}