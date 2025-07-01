plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    id("android.library.convention")
}

apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(compose.foundation)
        implementation(compose.material3)
    }
}