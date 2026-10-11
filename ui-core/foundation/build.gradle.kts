plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.grippo.ui.core.foundation"
        withHostTestBuilder {}
    }

    sourceSets.getByName("androidHostTest").dependencies {
        implementation(kotlin("test-junit"))
        implementation(libs.kotlinx.coroutines.test)
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.logger)
        implementation(projects.uiCore.error.errorProvider)
        implementation(projects.dataServices.firebase)

        api(libs.decompose.core)
        api(libs.decompose.extensions)

        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.immutable.collections)

        implementation(libs.compose.foundation)
    }
}
