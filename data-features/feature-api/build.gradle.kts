plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.features.feature.api"
        withHostTestBuilder {}
    }

    sourceSets.getByName("androidHostTest").dependencies {
        implementation(kotlin("test-junit"))
    }

    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
        implementation(libs.kotlinx.coroutines.test)
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.dateUtils)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.datetime)
    }
}
