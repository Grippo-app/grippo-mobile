plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.toolkit.image.loader"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.httpClient)

        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor)
    }
}