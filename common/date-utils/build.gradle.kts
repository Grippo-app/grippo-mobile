plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.designSystem.resources.provider)
            implementation(projects.designSystem.core)
            implementation(projects.common.logger)

            implementation(compose.foundation)

            implementation(libs.datetime)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}