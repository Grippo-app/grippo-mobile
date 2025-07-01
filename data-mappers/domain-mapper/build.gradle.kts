plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("android.library.convention")
}

apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.presentationFeatures.presentationApi)
        implementation(projects.dataServices.network)
        implementation(projects.common.logger)

        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}