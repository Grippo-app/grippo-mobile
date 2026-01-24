import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.services.apple.auth"
    }

    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.ktor.client.core)
        implementation(libs.koin.core)

        implementation(projects.toolkit.context)
        implementation(projects.toolkit.httpClient)
        implementation(projects.toolkit.serialization)

        implementation(compose.runtime)
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        if (konanTarget.family.isAppleFamily) {
            binaries.all {
                linkerOpts("-framework", "AuthenticationServices")
            }
        }
    }
}
