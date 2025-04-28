plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.cocoapods)
    alias(libs.plugins.android.library)
}

apply(from = "$rootDir/gradle/common/android.gradle")
apply(from = "$rootDir/gradle/common/ios.gradle")
apply(from = "$rootDir/gradle/common/kotlin.gradle")

kotlin.cocoapods {
    podfile = project.file("../iosApp/Podfile")
}

kotlin {
    sourceSets.commonMain.dependencies {
    }
}