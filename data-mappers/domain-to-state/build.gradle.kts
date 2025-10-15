plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.uiCore.state)
        implementation(projects.toolkit.logger)

        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}