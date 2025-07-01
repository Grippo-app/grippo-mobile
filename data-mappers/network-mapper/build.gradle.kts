plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("android.library.convention")
}

apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.network)
        implementation(projects.common.logger)
    }
}