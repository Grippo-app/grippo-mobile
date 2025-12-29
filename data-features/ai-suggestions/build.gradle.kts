plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.grippo.data.features.ai.suggestions"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.dateUtils)
        implementation(projects.toolkit.serialization)
        implementation(projects.toolkit.localization)
        implementation(projects.uiCore.error.errorProvider)

        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.aiAgent)
        implementation(projects.dataMappers.entityToDomain)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.datetime)
    }
}