plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.mappers.domain.to.dto"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.backend)
        implementation(projects.toolkit.logger)

        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}