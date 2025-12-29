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
        implementation(compose.foundation)
        implementation(compose.ui)
        implementation(compose.material3)
        implementation(compose.components.uiToolingPreview)
    }
    sourceSets.androidMain.dependencies {
        implementation(compose.uiTooling)
    }
}