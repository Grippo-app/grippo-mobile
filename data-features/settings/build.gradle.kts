plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.dataMappers.domainMapper)
        implementation(projects.dataMappers.entityToDomain)

        implementation(libs.kotlinx.coroutines.core)
    }
}