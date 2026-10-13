plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.grippo.ui.screen.features.authorization"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.analytics.analyticsApi)
        implementation(libs.kotlinx.serialization.core)
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.uiCore.error.errorProvider)
        implementation(projects.dataMappers.domainToState)
        implementation(projects.dataMappers.stateToDomain)
        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.uiScreenFeatures.screenApi)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.googleAuth)
        implementation(projects.dataServices.appleAuth)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.preview)

        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)

        implementation(libs.immutable.collections)
    }
}
