plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
}

apply(from = "$rootDir/gradle/common/android.gradle")
apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.common.core)
        implementation(projects.presentationFeatures.presentationApi)

        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources)

        implementation(compose.foundation)
        implementation(compose.material3)
    }
}