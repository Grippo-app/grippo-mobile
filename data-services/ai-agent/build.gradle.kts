plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.data.services.ai.agent"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.toolkit.httpClient)

            implementation(libs.ktor.client.core)

            // Issue https://youtrack.jetbrains.com/issue/KG-303/Packaging-issue-with-netty
            val koogAgent = libs.koog.agent.get()
            val googleClient = libs.google.client.get()

            val koogAgentNotation =
                "${koogAgent.module.group}:${koogAgent.module.name}:${koogAgent.versionConstraint.requiredVersion}"
            val koogPromptNotation =
                "${googleClient.module.group}:${googleClient.module.name}:${googleClient.versionConstraint.requiredVersion}"

            implementation(koogAgentNotation) {
                exclude(group = "io.netty")
                exclude(group = "io.vertx")
            }
            implementation(koogPromptNotation) {
                exclude(group = "io.netty")
                exclude(group = "io.vertx")
            }
        }
    }
}