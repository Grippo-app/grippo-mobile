plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.features.feature.api"
    }

    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.datetime)
    }
}
