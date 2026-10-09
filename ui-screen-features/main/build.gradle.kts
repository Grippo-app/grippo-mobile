plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.ui.screen.features.main"
        withHostTestBuilder {}
    }

    sourceSets.getByName("androidHostTest").dependencies {
        implementation(kotlin("test-junit"))
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.uiScreenFeatures.screenApi)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.components)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.immutable.collections)
    }
}
