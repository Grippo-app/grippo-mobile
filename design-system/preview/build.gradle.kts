plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("android.library.convention")
    id("compose.multiplatform.convention")
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