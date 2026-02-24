plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.toolkit.browser"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.context)
    }
}
