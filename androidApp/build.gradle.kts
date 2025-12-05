plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    id("android.application.convention")
}

android {
    defaultConfig {
        applicationId = "com.grippo.android"
        multiDexEnabled = true
        manifestPlaceholders["GOOGLE_SERVER_CLIENT_ID"] =
            "28935847922-hm3kt6hq2qb493s277pul262rsr0bcbn.apps.googleusercontent.com"
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
    implementation(projects.toolkit.dateUtils)
    implementation(projects.toolkit.theme)
    implementation(projects.designSystem.core)

    implementation(compose.foundation)
    implementation(compose.material3)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)
}
