plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.toolkit.logger"
    }

    sourceSets.commonMain.dependencies {
        implementation(libs.datetime)
    }
}
