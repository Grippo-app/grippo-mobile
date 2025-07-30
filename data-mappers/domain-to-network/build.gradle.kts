plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.network)
        implementation(projects.common.logger)
        implementation(projects.common.state)

        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}