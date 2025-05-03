plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.cocoapods)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
}

apply(from = "$rootDir/gradle/common/android.gradle")
apply(from = "$rootDir/gradle/common/ios.gradle")
apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.decompose.core)
        api(libs.decompose.extensions)

        implementation(projects.designSystem.core)

        implementation(projects.common.platformCore)
        implementation(projects.common.core)

        implementation(projects.dataServices.database)
        implementation(projects.dataServices.network)

        implementation(projects.dataFeatures.authorization)
        implementation(projects.dataFeatures.user)
        implementation(projects.dataFeatures.weightHistory)

        implementation(libs.koin.core)

        implementation(compose.foundation)
        implementation(compose.ui)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.koin.android)
    }
}