plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.network)
        implementation(projects.dataMappers.domainToState)
        implementation(projects.dataMappers.databaseToDomain)
        implementation(projects.dataMappers.networkToDatabase)

        implementation(libs.kotlinx.coroutines.core)
    }
}