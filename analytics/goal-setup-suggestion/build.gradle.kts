plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.analytics.goal.setup.suggestion"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.analytics.analyticsApi)
    }
}
