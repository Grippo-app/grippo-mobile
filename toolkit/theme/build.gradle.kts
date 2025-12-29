plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.toolkit.theme"
    }

    sourceSets.commonMain.dependencies {
        implementation(compose.foundation)
    }
}
