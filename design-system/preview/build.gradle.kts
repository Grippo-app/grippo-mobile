plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    id("android.library.convention")
}

apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.designSystem.core)

        implementation(compose.foundation)
        implementation(compose.components.uiToolingPreview)
    }
    sourceSets.androidMain.dependencies {
        implementation(compose.uiTooling)
    }
}