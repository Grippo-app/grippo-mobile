plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("android.library.convention")
}

apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.koin.core)
        implementation(libs.datetime)
    }
}