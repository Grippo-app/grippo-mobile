plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.datetime)
    }
}
