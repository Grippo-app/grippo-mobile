plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("android.library.convention")
    id("compose.multiplatform.convention")
}

apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.decompose.core)
        implementation(libs.decompose.extensions)

        implementation(compose.foundation)
    }
}