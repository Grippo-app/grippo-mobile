plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
}

apply(from = "$rootDir/gradle/common/android.gradle")
apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.common.core)
        implementation(projects.common.validation)
        implementation(projects.common.platformCore)
        implementation(projects.common.dateUtils)
        implementation(projects.dataMappers.domainMapper)
        implementation(projects.dialogFeatures.dialogApi)
        implementation(projects.presentationFeatures.presentationApi)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.preview)

        implementation(compose.foundation)
        implementation(compose.material3)

        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}