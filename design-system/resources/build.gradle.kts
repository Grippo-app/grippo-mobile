plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    id("android.library.convention")
}

apply(from = "$rootDir/gradle/common/kotlin.gradle")

compose.resources {
    publicResClass = true
    packageOfResClass = "com.grippo.design.resources"
    generateResClass = always
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(compose.components.resources)
        implementation(compose.foundation)
        implementation(compose.materialIconsExtended)
    }
}