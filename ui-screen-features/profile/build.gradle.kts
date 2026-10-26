plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.grippo.ui.screen.features.profile"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.analytics.analyticsApi)
        implementation(libs.kotlinx.serialization.json)
        implementation(projects.uiCore.foundation)
        implementation(projects.toolkit.dateUtils)
        implementation(projects.toolkit.linkOpener)
        implementation(projects.toolkit.localization)
        implementation(projects.toolkit.notificationManager)
        implementation(projects.uiCore.state)
        implementation(projects.dataMappers.domainToState)
        implementation(projects.dataMappers.stateToDomain)
        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.uiScreenFeatures.screenApi)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.preview)

        implementation(libs.compose.ui.tooling.preview)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)

        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}
