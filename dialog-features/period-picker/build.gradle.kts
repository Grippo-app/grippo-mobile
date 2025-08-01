plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.common.core)
        implementation(projects.common.state)
        implementation(projects.common.dateUtils)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources)
        implementation(projects.designSystem.preview)
        implementation(projects.designSystem.components)
        implementation(projects.composeLibs.wheelPicker)
        implementation(projects.dialogFeatures.dialogApi)

        implementation(compose.foundation)
        implementation(compose.material3)

        implementation(libs.datetime)
        implementation(libs.immutable.collections)
    }
}