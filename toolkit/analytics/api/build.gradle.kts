plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.grippo.toolkit.analytics.api" }

    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.coroutines.core)
    }
}
