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

    cocoapods {
        framework {
            export(libs.decompose.core.get())
            export(libs.decompose.essenty.get())
        }
    }

    sourceSets.commonMain.dependencies {
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
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.koin.android)
    }
}