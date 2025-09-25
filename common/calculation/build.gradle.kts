plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.common.state)
        implementation(projects.common.dateUtils)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.core)

        implementation(compose.foundation)

        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}