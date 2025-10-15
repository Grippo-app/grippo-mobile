plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.backend)
        implementation(projects.toolkit.logger)
    }
}