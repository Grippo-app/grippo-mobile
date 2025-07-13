plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.decompose.core)
        implementation(libs.decompose.extensions)

        implementation(compose.foundation)
    }
}