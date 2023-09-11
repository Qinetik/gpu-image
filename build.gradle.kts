// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${property("agp.version")}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${property("kotlin.version")}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
}


tasks.create<Delete>("clean"){
    delete(rootProject.buildDir)
}