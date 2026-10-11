plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}
kotlin {
    android { namespace = "com.grippo.toolkit.messaging.api" }
    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.coroutines.core)
    }
}
