plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.grippo.analytics.api"
    }

    sourceSets.commonMain.dependencies {
        api(projects.toolkit.analytics.api)
    }
}
