// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    ext.kotlin_version = '1.3.72'
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:4.2.0-beta04'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"

// TODO: Close JCenter on May 1st https://jfrog.com/blog/into-the-sunset-bintray-jcenter-gocenter-and-chartcenter/
//        classpath 'com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.5'
//        classpath 'com.github.dcendents:android-maven-gradle-plugin:2.1'
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
}

task clean(type: Delete) {
    delete rootProject.buildDir
}