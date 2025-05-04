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

include(":presentation-features:presentation-api")
include(":presentation-features:authorization")

include(":common:platform-core")
include(":common:logger")
include(":common:core")
include(":common:errors")
include(":common:validation")

include(":data-mappers:database-mapper")
include(":data-mappers:network-mapper")
include(":data-mappers:domain-mapper")