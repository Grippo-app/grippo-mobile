plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.design.system.resources.provider.impl"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.designSystem.resources.provider)
        implementation(projects.toolkit.theme)
    }
}