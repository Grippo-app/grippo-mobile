plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

apply(from = "$rootDir/gradle/common/android.gradle")
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