plugins {
    `kotlin-dsl`
}
dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
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
    }
}
