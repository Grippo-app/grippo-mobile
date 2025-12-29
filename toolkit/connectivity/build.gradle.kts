plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.toolkit.connectivity"
    }

    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(projects.toolkit.context)
    }
}
