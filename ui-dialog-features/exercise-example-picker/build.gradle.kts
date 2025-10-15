plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.toolkit.dateUtils)
        implementation(projects.designSystem.core)
        implementation(projects.dataMappers.stateToDomain)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.preview)
        implementation(projects.designSystem.components)
        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataMappers.domainToState)

        implementation(compose.foundation)
        implementation(compose.material3)

        implementation(libs.datetime)
        implementation(libs.immutable.collections)
    }
}