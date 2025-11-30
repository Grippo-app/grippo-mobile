import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.ktor.client.core)
        implementation(libs.koin.core)
        implementation(projects.toolkit.context)
        implementation(projects.toolkit.httpClient)
        implementation(projects.toolkit.serialization)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.credentials)
        implementation(libs.androidx.credentials.play.services.auth)
        implementation(libs.google.identity.googleid)
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        if (konanTarget.family.isAppleFamily) {
            binaries.all {
                linkerOpts("-framework", "AuthenticationServices")
                linkerOpts("-framework", "Security")
            }
        }
    }
}
