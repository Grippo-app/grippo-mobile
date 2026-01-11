plugins {
    id("android.application.convention")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.grippo.android.app"

    defaultConfig {
        applicationId = "com.grippo.android"
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        manifestPlaceholders["GOOGLE_SERVER_CLIENT_ID"] = "YOUR_GOOGLE_SERVER_CLIENT_ID"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val default = getDefaultProguardFile("proguard-android-optimize.txt")
            proguardFiles(default, "proguard-rules.pro")
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

    // Firebase
    implementation(projects.dataServices.firebase)
    implementation(project.dependencies.platform(libs.android.firebase.bom))
    implementation(libs.android.firebase.analytics)
    implementation(libs.android.firebase.crashlytics)
}
