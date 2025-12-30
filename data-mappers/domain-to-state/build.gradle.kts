plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.mappers.domain.to.state"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.uiCore.state)
        implementation(projects.toolkit.logger)
        implementation(projects.toolkit.dateUtils)
        implementation(compose.foundation)

        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}
