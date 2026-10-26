plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.toolkit.localization"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.context)

        implementation(libs.compose.foundation)
    }
}
