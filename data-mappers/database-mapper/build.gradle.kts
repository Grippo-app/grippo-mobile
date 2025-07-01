plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataServices.database)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.common.dateUtils)
        implementation(projects.common.logger)

        implementation(libs.datetime)
    }
}