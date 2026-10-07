plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.design.system.core"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.designSystem.resources.provider)

        implementation(libs.compose.foundation)
        implementation(libs.compose.runtime)
    }
}