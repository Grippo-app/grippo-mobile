plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("kotlin.multiplatform.test.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.grippo.toolkit.http.client"
    }

    sourceSets.getByName("androidHostTest").dependencies {
        implementation(libs.ktor.client.mock)
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.toolkit.context)
            implementation(projects.toolkit.logger)
            implementation(projects.toolkit.serialization)
            implementation(projects.uiCore.error.errorProvider)

            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
