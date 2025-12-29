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
        api(compose.components.resources)
        implementation(compose.foundation)
        implementation(compose.materialIconsExtended)
    }
}