plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    id("android.application.convention")
}

android {
    defaultConfig {
        applicationId = "com.grippo"
        multiDexEnabled = true
    }

    signingConfigs.create("release") {
        storeFile = file("$rootDir/build-logic/keys/developer")
        storePassword = "qwerty123"
        keyAlias = "developer"
        keyPassword = "qwerty123"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val default = getDefaultProguardFile("proguard-android-optimize.txt")
            proguardFiles(default, "proguard-rules.pro")
            signingConfig = signingConfigs["release"]
        }
    }
}

dependencies {
    implementation(projects.shared)
    implementation(projects.uiCore.foundation)
    implementation(projects.common.dateUtils)
    implementation(projects.platformCore.theme)
    implementation(projects.designSystem.core)

    implementation(compose.foundation)
    implementation(compose.material3)

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
}