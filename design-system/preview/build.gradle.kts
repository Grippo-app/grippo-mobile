plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.design.system.preview"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)

        implementation(libs.compose.foundation)
        implementation(libs.compose.ui.tooling.preview)
        implementation(libs.coil.compose)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.compose.ui.tooling)
    }
}
