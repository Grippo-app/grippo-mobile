plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.toolkit.localization"
    }

    sourceSets.commonMain.dependencies {
        implementation(compose.foundation)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.appcompat)
    }
}
