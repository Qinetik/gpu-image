plugins {
    id("com.android.library")
    kotlin("android")
}


android {
    namespace = "jp.co.cyberagent.android.gpuimage"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        ndk.abiFilters.addAll(listOf(
            "armeabi-v7a","arm64-v8a","x86","x86_64"
        ))
    }
    externalNativeBuild {
        cmake { path("src/main/cpp/CMakeLists.txt") }
    }

    buildTypes {
        debug {
//            debuggable true
//            isJniDebuggable = true
        }
        release {
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
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
    implementation("com.danielgergely.kgl:kgl:0.6.2")
    implementation("com.danielgergely.kgl:kgl-android:0.6.2")
    implementation(project(":gpu-image"))
}