plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.mappers.dto.to.entity"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.backend)
        implementation(projects.toolkit.logger)
    }
}