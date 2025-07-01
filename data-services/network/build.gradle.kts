plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("android.library.convention")
}

apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.common.platformCore)
            implementation(projects.common.logger)
            implementation(projects.common.error.errorProvider)
            implementation(projects.dataServices.database)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.auth)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
