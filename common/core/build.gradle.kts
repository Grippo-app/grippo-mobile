plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("ios.cocoapods.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.common.logger)
        implementation(projects.common.error.errorProvider)

        api(libs.decompose.core)
        api(libs.decompose.extensions)

        api(libs.koin.core) // todo remove it

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.immutable.collections)

        implementation(compose.foundation)
    }
}