plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.grippo.toolkit.analytics.core" }

    sourceSets.commonMain.dependencies {
        api(projects.toolkit.analytics.api)
        implementation(libs.kotlinx.coroutines.core)
    }
}
