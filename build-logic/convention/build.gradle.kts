plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.gradle.plugin)
    implementation(libs.compose.multiplatform.gradle.plugin)
    implementation(libs.ksp.plugin.api)
}

gradlePlugin {
    plugins {
        register("android.library.convention") {
            id = "android.library.convention"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("android.application.convention") {
            id = "android.application.convention"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("compose.multiplatform.convention") {
            id = "compose.multiplatform.convention"
            implementationClass = "ComposeMultiplatformConventionPlugin"
        }
        register("kotlin.multiplatform.convention") {
            id = "kotlin.multiplatform.convention"
            implementationClass = "KotlinMultiplatformConventionPlugin"
        }
        register("koin.annotation.convention") {
            id = "koin.annotation.convention"
            implementationClass = "KoinAnnotationConventionPlugin"
        }
        register("room.convention") {
            id = "room.convention"
            implementationClass = "RoomConventionPlugin"
        }
        register("ios.swiftpackage.convention") {
            id = "ios.swiftpackage.convention"
            implementationClass = "IosSwiftPackageConventionPlugin"
        }
        register("kmp.test.convention") {
            id = "kmp.test.convention"
            implementationClass = "KmpTestConventionPlugin"
        }
        register("coroutines.test.convention") {
            id = "coroutines.test.convention"
            implementationClass = "CoroutinesTestConventionPlugin"
        }
        register("flow.test.convention") {
            id = "flow.test.convention"
            implementationClass = "FlowTestConventionPlugin"
        }
        register("network.test.convention") {
            id = "network.test.convention"
            implementationClass = "NetworkTestConventionPlugin"
        }
        register("di.test.convention") {
            id = "di.test.convention"
            implementationClass = "DiTestConventionPlugin"
        }
        register("room.test.convention") {
            id = "room.test.convention"
            implementationClass = "RoomTestConventionPlugin"
        }
        register("compose.ui.test.convention") {
            id = "compose.ui.test.convention"
            implementationClass = "ComposeUiTestConventionPlugin"
        }
    }
}
