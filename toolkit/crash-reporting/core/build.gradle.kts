plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("kotlin.multiplatform.test.convention")
    id("koin.annotation.convention")
}
kotlin {
    android { namespace = "com.grippo.toolkit.crashreporting.core" }
    sourceSets.commonMain.dependencies {
        api(projects.toolkit.crashReporting.api)
        implementation(libs.kotlinx.coroutines.core)
    }
    sourceSets.commonTest.dependencies { implementation(libs.kotlinx.coroutines.test) }
}
