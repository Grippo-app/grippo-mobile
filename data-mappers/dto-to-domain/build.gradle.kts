plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.mappers.dto.to.domain"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.backend)
        implementation(projects.toolkit.logger)
        implementation(projects.toolkit.dateUtils)

        implementation(libs.datetime)
    }
}