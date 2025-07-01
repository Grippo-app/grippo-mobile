plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    id("android.library.convention")
}

apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.common.core)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources)
        implementation(projects.designSystem.preview)
        implementation(projects.designSystem.components)

        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.immutable.collections)
    }
}