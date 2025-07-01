plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    id("android.library.convention")
    id("ios.cocoapods.convention")
}

apply(from = "$rootDir/gradle/common/kotlin.gradle")
apply(from = "$rootDir/gradle/common/compose.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.common.logger)
        implementation(projects.common.error.errorProvider)

        api(libs.decompose.core)
        api(libs.decompose.extensions)
        api(libs.koin.core)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.immutable.collections)

        implementation(compose.foundation)
    }
}