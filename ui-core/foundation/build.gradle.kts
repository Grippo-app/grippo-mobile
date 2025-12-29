plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.ui.core.foundation"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.logger)
        implementation(projects.uiCore.error.errorProvider)

        api(libs.decompose.core)
        api(libs.decompose.extensions)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.immutable.collections)

        implementation(compose.foundation)
    }
}
