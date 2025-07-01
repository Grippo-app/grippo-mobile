plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("android.library.convention")
}

apply(from = "$rootDir/gradle/common/kotlin.gradle")