plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("android.library.convention")
}

apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.network)
        implementation(projects.dataMappers.domainMapper)
        implementation(projects.dataMappers.databaseMapper)
        implementation(projects.dataMappers.networkMapper)

        implementation(libs.koin.core)
        implementation(libs.kotlinx.coroutines.core)
    }
}