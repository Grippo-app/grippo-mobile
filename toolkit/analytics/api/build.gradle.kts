plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("kotlin.multiplatform.test.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.grippo.toolkit.analytics.api" }

    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.coroutines.core)
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.coroutines.test)
    }
}
