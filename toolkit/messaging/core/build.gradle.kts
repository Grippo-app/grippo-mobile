plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}
kotlin {
    android { namespace = "com.grippo.toolkit.messaging.core" }
    sourceSets.commonMain.dependencies {
        api(projects.toolkit.messaging.api)
        implementation(libs.kotlinx.coroutines.core)
    }
}
