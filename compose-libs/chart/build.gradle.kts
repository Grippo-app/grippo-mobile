plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.compose.libs.chart"
    }

    sourceSets.commonMain.dependencies {
        implementation(libs.compose.foundation)
        implementation(libs.compose.ui)
        implementation(libs.compose.material3)
        implementation(libs.compose.ui.tooling.preview)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.compose.ui.tooling)
    }
}