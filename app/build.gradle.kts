plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cq61tech.chilledandroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cq61tech.chilledandroid"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Debug anahtariyla imzalanir: direkt kurulabilen duz APK
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    androidResources {
        noCompress += listOf("mp4", "mp3")
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}
