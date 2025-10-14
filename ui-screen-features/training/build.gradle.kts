plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.common.state)
        implementation(projects.common.calculation)
        implementation(projects.common.error.errorProvider)
        implementation(projects.common.dateUtils)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.preview)
        implementation(projects.designSystem.components)
        implementation(projects.uiScreenFeatures.screenApi)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataMappers.domainToState)
        implementation(projects.dataMappers.stateToDomain)
        implementation(projects.uiDialogFeatures.dialogApi)

        implementation(compose.foundation)
        implementation(compose.material3)

        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}