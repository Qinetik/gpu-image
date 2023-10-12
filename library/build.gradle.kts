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
    val kglVersion = property("kgl.version")
    implementation("org.qinetik.kgl:kgl:${kglVersion}")
    implementation("org.qinetik.kgl:kgl-android:${kglVersion}")
    api(project(":gpu-image"))
}