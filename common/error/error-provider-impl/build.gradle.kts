plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dialogFeatures.dialogApi)
        implementation(projects.common.error.errorProvider)

        implementation(libs.koin.core)
    }
}