plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.dataMappers.domainToDatabase)
        implementation(projects.dataMappers.databaseToDomain)

        implementation(libs.kotlinx.coroutines.core)
    }
}