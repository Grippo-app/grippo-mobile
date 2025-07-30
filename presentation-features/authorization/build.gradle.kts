plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.common.core)
        implementation(projects.common.validation)
        implementation(projects.common.platformCore)
        implementation(projects.common.dateUtils)
        implementation(projects.common.state)
        implementation(projects.dataMappers.domainToState)
        implementation(projects.dialogFeatures.dialogApi)
        implementation(projects.presentationFeatures.presentationApi)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.preview)

        implementation(compose.foundation)
        implementation(compose.material3)

        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}