plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dialogFeatures.dialogApi)
        implementation(projects.common.error.errorProvider)
        implementation(projects.common.state)
    }
}