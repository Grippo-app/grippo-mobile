plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.services.firebase"
    }

    sourceSets.androidMain.dependencies {
        implementation(project.dependencies.platform(libs.android.firebase.bom))
        implementation(libs.android.firebase.analytics)
    }
}