plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.grippo.ui.screen.features.debug"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.analytics.analyticsApi)
        implementation(libs.kotlinx.serialization.json)
        implementation(projects.uiCore.foundation)
        implementation(projects.toolkit.dateUtils)
        implementation(projects.toolkit.logger)
        implementation(projects.uiCore.state)
        implementation(projects.dataMappers.domainToState)
        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.uiScreenFeatures.screenApi)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.preview)

        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)

        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}