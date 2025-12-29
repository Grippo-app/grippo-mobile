plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.mappers.entity.to.domain"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataServices.database)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.toolkit.dateUtils)
        implementation(projects.toolkit.logger)

        implementation(libs.datetime)
    }
}