plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(compose.foundation)
        implementation(compose.ui)
        implementation(compose.material3)
    }
}