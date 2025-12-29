plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.compose.libs.wheel.picker"
    }

    sourceSets.commonMain.dependencies {
        implementation(compose.foundation)
        implementation(compose.material3)
    }
}