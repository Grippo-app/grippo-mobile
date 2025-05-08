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
        implementation(projects.designSystem.resources)

        implementation(projects.common.platformCore)
        implementation(projects.common.core)

        implementation(projects.dataServices.database)
        implementation(projects.dataServices.network)

        implementation(projects.dataFeatures.authorization)
        implementation(projects.dataFeatures.user)
        implementation(projects.dataFeatures.weightHistory)
        implementation(projects.dataFeatures.muscle)

        implementation(projects.presentationFeatures.presentationApi)
        implementation(projects.presentationFeatures.authorization)

        implementation(projects.dialogFeatures.dialogApi)
        implementation(projects.dialogFeatures.weightPicker)
        implementation(projects.dialogFeatures.heightPicker)

        implementation(libs.koin.core)
        implementation(libs.immutable.collections)

        implementation(compose.ui)
        implementation(compose.material3)
        implementation(compose.foundation)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.koin.android)
    }
}