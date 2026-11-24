plugins {
    id("com.android.application")
}

android {
    namespace = "com.minsoojun.kakaostickerstudio"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.minsoojun.kakaostickerstudio"
        minSdk = 24
        targetSdk = 34
        versionCode = 6
        versionName = "3.1"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/release-key.jks")
            storePassword = "kakaoSticker2026"
            keyAlias = "kakaosticker"
            keyPassword = "kakaoSticker2026"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.core:core:1.12.0")
}
