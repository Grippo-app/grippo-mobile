plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.toolkit.permission.manager"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.context)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.appcompat)
    }
}
