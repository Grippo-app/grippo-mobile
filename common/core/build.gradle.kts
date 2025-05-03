plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.cocoapods)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
}

apply(from = "$rootDir/gradle/common/android.gradle")
apply(from = "$rootDir/gradle/common/kotlin.gradle")
apply(from = "$rootDir/gradle/common/compose.gradle")
apply(from = "$rootDir/gradle/common/ios.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.common.logger)

        api(libs.decompose.core)
        api(libs.decompose.extensions)
        api(libs.koin.core)

        implementation(libs.kotlinx.coroutines.core)
        implementation(compose.foundation)
    }
}