plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("ios.cocoapods.convention")
    id("room.convention")
    alias(libs.plugins.ksp)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.common.platformCore)
            implementation(projects.common.logger)
            implementation(projects.common.error.errorProvider)
            implementation(libs.koin.core)
        }
    }
}