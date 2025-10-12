plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.backend)
        implementation(projects.dataMappers.domainToState)
        implementation(projects.dataMappers.domainToDatabase)
        implementation(projects.dataMappers.databaseToDomain)
        implementation(projects.dataMappers.networkToDatabase)
        implementation(projects.dataMappers.domainToNetwork)
        implementation(projects.common.dateUtils)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.datetime)
    }
}