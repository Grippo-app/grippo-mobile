plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.toolkit.share"
        androidResources.enable = true
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.context)

        implementation(libs.compose.ui)
        implementation(libs.kotlinx.coroutines.core)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.core)
    }
}
