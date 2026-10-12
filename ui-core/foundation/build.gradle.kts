plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("kotlin.multiplatform.test.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.grippo.ui.core.foundation"
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.coroutines.test)
    }

    sourceSets.commonMain.dependencies {
        api(projects.toolkit.analytics.api)
        implementation(projects.toolkit.logger)
        implementation(projects.uiCore.error.errorProvider)
        implementation(projects.toolkit.crashReporting.api)

        api(libs.decompose.core)
        api(libs.decompose.extensions)

        api(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.immutable.collections)

        implementation(libs.compose.foundation)
    }
}
