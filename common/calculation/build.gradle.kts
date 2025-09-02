plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.common.state)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.resources.provider)

        implementation(compose.foundation)
        implementation(libs.immutable.collections)
    }
}