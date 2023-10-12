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
        val kglVersion = property("kgl.version")
        val commonMain by getting {
            dependencies {
                api("org.qinetik.kgl:kgl:${kglVersion}")
                implementation("org.qinetik:logger:1.0.16")
            }
        }
        val androidMain by getting {
            dependencies {
                api("org.qinetik.kgl:kgl-android:${kglVersion}")
            }
        }
        val jvmMain by getting {
            dependencies {

                api("org.qinetik.kgl:kgl-lwjgl:${kglVersion}")
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