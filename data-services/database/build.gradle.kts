plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    id("android.library.convention")
    id("ios.cocoapods.convention")
}

apply(from = "$rootDir/gradle/common/kotlin.gradle")

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.common.platformCore)
            implementation(projects.common.logger)
            implementation(projects.common.error.errorProvider)

            implementation(libs.androidx.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.sqlite)
            implementation(libs.koin.core)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosX64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}

