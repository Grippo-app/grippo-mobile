plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.features.trainings"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.backend)
        implementation(projects.dataMappers.domainToEntity)
        implementation(projects.dataMappers.entityToDomain)
        implementation(projects.dataMappers.dtoToEntity)
        implementation(projects.dataMappers.domainToDto)
        implementation(projects.toolkit.dateUtils)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.datetime)
    }
}