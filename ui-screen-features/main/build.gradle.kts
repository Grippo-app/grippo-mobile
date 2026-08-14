plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
    id("flow.test.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.grippo.ui.screen.features.main"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.uiScreenFeatures.screenApi)
        implementation(projects.uiScreenFeatures.home)
        implementation(projects.uiScreenFeatures.trainings)
        implementation(projects.uiScreenFeatures.profile)
        implementation(projects.uiScreenFeatures.exercises)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.preview)

        implementation(compose.foundation)
        implementation(compose.material3)

        implementation(libs.immutable.collections)
    }

    sourceSets.commonTest.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.uiDialogFeatures.dialogApi)
    }
}
