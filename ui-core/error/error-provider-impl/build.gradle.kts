plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.uiCore.error.errorProvider)
        implementation(projects.uiCore.state)
    }
}