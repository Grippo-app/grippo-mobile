plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.ui.dialog.features.muscle.loading"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataMappers.domainToState)

        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.preview)
        implementation(projects.designSystem.components)
        implementation(projects.toolkit.dateUtils)

        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}
