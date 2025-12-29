plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.ui.core.error.error.provider.impl"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.uiCore.error.errorProvider)
        implementation(projects.uiCore.state)
    }
}