plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.services.firebase"
    }

    sourceSets.commonMain.dependencies {
        implementation(libs.koin.core)
        implementation(libs.kotlinx.coroutines.core)
    }

    sourceSets.androidMain.dependencies {
        implementation(projects.toolkit.notificationManager)

        implementation(project.dependencies.platform(libs.android.firebase.bom))
        implementation(libs.android.firebase.analytics)
        implementation(libs.android.firebase.crashlytics)
        implementation(libs.android.firebase.messaging)
        implementation(libs.kotlinx.coroutines.play.services)
    }
}
