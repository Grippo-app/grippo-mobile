plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.google.services)
}

kotlin {
    android {
        namespace = "com.grippo.data.services.firebase"
    }

    sourceSets.androidMain.dependencies {
        implementation(project.dependencies.platform(libs.android.firebase.bom))
        implementation(libs.android.firebase.analytics)
        implementation(libs.android.firebase.crashlytics)
    }
}