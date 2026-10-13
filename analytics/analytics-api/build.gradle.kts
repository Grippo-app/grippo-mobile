plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.analytics.api"
    }

    sourceSets.commonMain.dependencies {
        api(projects.toolkit.analytics.api)
    }
}
