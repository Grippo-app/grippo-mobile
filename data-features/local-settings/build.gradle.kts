plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.features.local.settings"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.datastore)

        implementation(libs.androidx.datastore.preferences.core)
        implementation(libs.kotlinx.coroutines.core)
    }
}
