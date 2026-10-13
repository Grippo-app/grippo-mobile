plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.analytics.month.picker"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.analytics.analyticsApi)
    }
}
