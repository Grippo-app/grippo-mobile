plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.common.platformCore)
            implementation(projects.common.httpClient)

            implementation(libs.ktor.client.core)

            // Issue https://youtrack.jetbrains.com/issue/KG-303/Packaging-issue-with-netty
            val koogAgent = libs.koog.agent.get()
            val openrouterClient = libs.openrouter.client.get()

            val koogAgentNotation =
                "${koogAgent.module.group}:${koogAgent.module.name}:${koogAgent.versionConstraint.requiredVersion}"
            val koogPromptNotation =
                "${openrouterClient.module.group}:${openrouterClient.module.name}:${openrouterClient.versionConstraint.requiredVersion}"

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
