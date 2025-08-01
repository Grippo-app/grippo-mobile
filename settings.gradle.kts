enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {

    includeBuild("build-logic")

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

@Suppress("UnstableApiUsage")
dependencyResolutionManagement.repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

@Suppress("UnstableApiUsage")
dependencyResolutionManagement.repositories {
    google {
        content {
            includeGroupByRegex("com\\.android.*")
            includeGroupByRegex("com\\.google.*")
            includeGroupByRegex("androidx.*")
        }
    }
    mavenCentral()
}

rootProject.name = "Grippo"

include(":androidApp")
include(":shared")

include(":design-system:preview")
include(":design-system:core")
include(":design-system:resources")
include(":design-system:components")

include(":data-services:network")
include(":data-services:database")

include(":data-features:feature-api")
include(":data-features:authorization")
include(":data-features:user")
include(":data-features:weight-history")
include(":data-features:muscle")
include(":data-features:equipment")
include(":data-features:excluded-muscles")
include(":data-features:excluded-equipments")
include(":data-features:trainings")
include(":data-features:settings")
include(":data-features:exercise-examples")

include(":presentation-features:presentation-api")
include(":presentation-features:authorization")
include(":presentation-features:home")
include(":presentation-features:profile")
include(":presentation-features:debug")
include(":presentation-features:settings")

include(":dialog-features:dialog-api")
include(":dialog-features:weight-picker")
include(":dialog-features:height-picker")
include(":dialog-features:exercise")
include(":dialog-features:date-picker")
include(":dialog-features:period-picker")
include(":dialog-features:exercise-example")
include(":dialog-features:error-display")

include(":common:platform-core")
include(":common:logger")
include(":common:state")
include(":common:core")
include(":common:connectivity")
include(":common:error:error-provider-impl")
include(":common:error:error-provider")
include(":common:validation")
include(":common:date-utils")

include(":compose-libs:wheel-picker")
include(":compose-libs:segment-control")
include(":compose-libs:konfetti")
include(":compose-libs:chart")

include(":data-mappers:database-to-domain")
include(":data-mappers:network-to-database")
include(":data-mappers:domain-to-state")
include(":data-mappers:domain-to-database")
include(":data-mappers:domain-to-network")
include(":data-mappers:state-to-domain")