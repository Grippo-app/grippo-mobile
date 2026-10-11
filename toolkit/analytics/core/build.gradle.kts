plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("kotlin.multiplatform.test.convention")
}

kotlin {
    android { namespace = "com.grippo.toolkit.analytics.core" }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.coroutines.test)
    }

    sourceSets.commonMain.dependencies {
        api(projects.toolkit.analytics.api)
        implementation(libs.kotlinx.coroutines.core)
    }
}
