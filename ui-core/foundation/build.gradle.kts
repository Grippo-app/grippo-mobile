plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("ios.cocoapods.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.logger)
        implementation(projects.uiCore.error.errorProvider)

        api(libs.decompose.core)
        api(libs.decompose.extensions)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.immutable.collections)

        implementation(compose.foundation)
    }
}