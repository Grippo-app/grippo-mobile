plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

apply(from = "$rootDir/gradle/common/android.gradle")
apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.auth)

            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
