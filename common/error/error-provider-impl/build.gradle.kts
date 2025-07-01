plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("android.library.convention")
}

apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dialogFeatures.dialogApi)
        implementation(projects.common.error.errorProvider)

        implementation(libs.koin.core)
    }
}