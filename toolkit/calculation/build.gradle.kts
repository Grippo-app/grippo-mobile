plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.toolkit.calculation"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.state)
        implementation(projects.toolkit.dateUtils)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.core)

        implementation(compose.foundation)

        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}