plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.common.dateUtils)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources)

        implementation(libs.datetime)
        implementation(libs.immutable.collections)
        implementation(libs.kotlinx.serialization.json)

        implementation(compose.foundation)
    }
}