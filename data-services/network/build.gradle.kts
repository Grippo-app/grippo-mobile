plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.common.platformCore)
            implementation(projects.common.logger)
            implementation(projects.common.error.errorProvider)
            implementation(projects.common.serialization)
            implementation(projects.dataServices.database)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.auth)
            implementation(libs.kotlinx.serialization.json)

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

        androidMain.dependencies {
            implementation(libs.ktor.client.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
