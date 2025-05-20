enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
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

include(":presentation-features:presentation-api")
include(":presentation-features:authorization")
include(":presentation-features:home")

include(":dialog-features:dialog-api")
include(":dialog-features:weight-picker")
include(":dialog-features:height-picker")
include(":dialog-features:error-display")

include(":common:platform-core")
include(":common:logger")
include(":common:core")
include(":common:error:error-provider-impl")
include(":common:error:error-provider")
include(":common:metrics")
include(":common:validation")
include(":common:date-utils")

include(":compose-libs:wheel-picker")
include(":compose-libs:segment-control")
include(":compose-libs:konfetti")

include(":data-mappers:database-mapper")
include(":data-mappers:network-mapper")
include(":data-mappers:domain-mapper")