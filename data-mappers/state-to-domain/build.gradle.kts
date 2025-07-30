plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.common.state)
        implementation(projects.common.logger)

        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}