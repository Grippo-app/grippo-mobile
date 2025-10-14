plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("ios.cocoapods.convention")
    id("koin.annotation.convention")
}

kotlin {
    cocoapods {
        pod("MLKitCommon") {
            moduleName = "MLKitCommon"
        }
        pod("MLKitTranslate") {
            moduleName = "MLKitTranslate"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.google.mlkit.translate)
        }
    }
}
