plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.services.firebase"
    }

    sourceSets.commonMain.dependencies {
        implementation("dev.gitlive:firebase-installations:2.4.0")
        implementation("dev.gitlive:firebase-analytics:2.4.0")
    }
}