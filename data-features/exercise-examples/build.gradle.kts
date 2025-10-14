plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        л implementation (projects.common.platformCore)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.backend)
        implementation(projects.dataServices.mlTranslation)
        implementation(projects.dataMappers.domainToState)
        implementation(projects.dataMappers.entityToDomain)
        implementation(projects.dataMappers.dtoToEntity)

        implementation(libs.kotlinx.coroutines.core)
    }
}