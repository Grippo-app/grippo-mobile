plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

apply(from = "$rootDir/gradle/common/android.gradle")
apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataServices.database)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.common.dateUtils)
        implementation(projects.common.logger)

        implementation(libs.datetime)
    }
}