plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    id("android.library.convention")
}

apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.common.core)
        implementation(projects.common.validation)
        implementation(projects.common.dateUtils)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources)

        implementation(libs.kotlinx.serialization.json)
        implementation(libs.datetime)
        implementation(libs.immutable.collections)

        implementation(compose.foundation)
    }
}