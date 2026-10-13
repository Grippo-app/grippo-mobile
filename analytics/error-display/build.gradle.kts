plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.analytics.error.display"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.analytics.analyticsApi)
    }
}
