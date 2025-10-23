plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.backend)
        implementation(projects.toolkit.logger)
        implementation(projects.toolkit.dateUtils)

        implementation(libs.datetime)
    }
}