plugins {
    `kotlin-dsl`
}
dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.gradle.plugin)
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
        register("ios.cocoapods.convention") {
            id = "ios.cocoapods.convention"
            implementationClass = "IosCocoapodsConventionPlugin"
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
    }
}
