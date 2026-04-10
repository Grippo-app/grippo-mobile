plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.services.datastore"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.context)

        implementation(libs.androidx.datastore.preferences.core)
    }
}
