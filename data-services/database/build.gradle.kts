plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("room.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.services.database"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.toolkit.context)
            implementation(projects.toolkit.logger)
            implementation(projects.uiCore.error.errorProvider)
        }
    }
}
