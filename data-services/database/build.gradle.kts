plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("ios.cocoapods.convention")
    id("room.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.toolkit.context)
            implementation(projects.common.logger)
            implementation(projects.uiCore.error.errorProvider)
        }
    }
}