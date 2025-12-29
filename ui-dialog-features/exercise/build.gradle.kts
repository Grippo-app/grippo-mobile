plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.ui.dialog.features.exercise"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.uiScreenFeatures.screenApi)
        implementation(projects.dataMappers.domainToState)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.preview)
        implementation(projects.composeLibs.wheelPicker)

        implementation(compose.foundation)
        implementation(compose.material3)

        implementation(libs.immutable.collections)
    }
}