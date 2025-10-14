plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.common.state)
        implementation(projects.common.dateUtils)

        implementation(libs.kotlinx.serialization.json)
        implementation(libs.datetime)

        implementation(compose.foundation)
    }
}