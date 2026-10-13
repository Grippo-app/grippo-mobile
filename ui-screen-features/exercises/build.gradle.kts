plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.grippo.ui.screen.features.exercises"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.analytics.analyticsApi)
        implementation(libs.kotlinx.serialization.json)
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.toolkit.dateUtils)
        implementation(projects.designSystem.core)
        implementation(projects.dataMappers.stateToDomain)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.preview)
        implementation(projects.designSystem.components)
        implementation(projects.uiScreenFeatures.screenApi)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataMappers.domainToState)

        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)

        implementation(libs.datetime)
        implementation(libs.immutable.collections)
    }
}