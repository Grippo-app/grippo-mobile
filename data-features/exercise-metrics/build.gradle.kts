plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.features.exercise.metrics"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.backend)
        implementation(projects.dataMappers.dtoToDomain)
        implementation(projects.dataMappers.entityToDomain)
        implementation(projects.uiCore.error.errorProvider)

        implementation(libs.kotlinx.coroutines.core)
    }
}