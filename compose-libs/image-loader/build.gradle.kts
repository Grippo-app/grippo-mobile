plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.platformCore.httpClient)

//        implementation(compose.foundation)
        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor)
    }
}