plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.designSystem.resources)
            implementation(projects.designSystem.core)

            implementation(compose.foundation)

            implementation(libs.datetime)
        }
    }
}