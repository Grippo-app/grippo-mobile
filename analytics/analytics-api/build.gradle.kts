plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("kotlin.multiplatform.test.convention")
}

kotlin {
    android {
        namespace = "com.grippo.analytics.api"
    }

    sourceSets.commonMain.dependencies {
        api(projects.toolkit.analytics.api)
    }
}
