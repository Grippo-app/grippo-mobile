plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(compose.foundation)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.appcompat)
    }
}
