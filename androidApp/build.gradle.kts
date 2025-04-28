plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

apply(from = "$rootDir/gradle/common/android.gradle")

android {
    buildFeatures {
        compose = true
    }

    defaultConfig {
        applicationId = "com.grippo"
        multiDexEnabled = true
    }

    signingConfigs.create("release") {
        storeFile = file("$rootDir/gradle/keys/developer")
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
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.compose.ui.tooling)
}