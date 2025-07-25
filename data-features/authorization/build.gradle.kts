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
        implementation(projects.dataMappers.domainMapper)
        implementation(projects.dataMappers.databaseMapper)
        implementation(projects.dataMappers.networkMapper)

        implementation(libs.kotlinx.coroutines.core)
    }
}