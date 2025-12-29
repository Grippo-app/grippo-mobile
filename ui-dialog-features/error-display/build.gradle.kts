plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.ui.dialog.features.error.display"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.preview)
        implementation(projects.designSystem.components)

        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.immutable.collections)
    }
}