plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.mappers.domain.to.entity"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.toolkit.logger)
        implementation(projects.toolkit.dateUtils)

        implementation(libs.datetime)
    }
}