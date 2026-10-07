plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.grippo.design.resources.provider"
    generateResClass = always
}

kotlin {
    android {
        namespace = "com.grippo.design.system.resources.provider"
    }

    androidLibrary {
        androidResources.enable = true
    }

    sourceSets.commonMain.dependencies {
        api(libs.compose.resources)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material.icons.extended)
    }
}