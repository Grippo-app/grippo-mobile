plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.network)
        implementation(projects.dataMappers.domainMapper)
        implementation(projects.dataMappers.databaseMapper)
        implementation(projects.dataMappers.networkMapper)
        implementation(projects.common.dateUtils)

        implementation(libs.koin.core)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.datetime)
    }
}