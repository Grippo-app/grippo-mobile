plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.designSystem.resources)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.preview)
        implementation(projects.composeLibs.segmentControl)
        implementation(projects.composeLibs.konfetti)
        implementation(projects.composeLibs.chart)
        implementation(projects.presentationFeatures.presentationApi)
        implementation(projects.common.dateUtils)

        implementation(compose.foundation)
        implementation(compose.runtime)
        implementation(compose.material3)

        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}