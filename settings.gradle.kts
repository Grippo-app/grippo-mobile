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

rootProject.name = "grippo-mobile"

include(":androidApp")
include(":shared")

include(":design-system:preview")
include(":design-system:core")
include(":design-system:resources:provider")
include(":design-system:resources:provider-impl")
include(":design-system:components")

include(":data-services:google-auth")
include(":data-services:firebase")
include(":data-services:backend")
include(":data-services:database")
include(":data-services:ai-agent")

include(":data-features:feature-api")
include(":data-features:authorization")
include(":data-features:user")
include(":data-features:weight-history")
include(":data-features:muscle")
include(":data-features:equipment")
include(":data-features:ai-suggestions")
include(":data-features:excluded-muscles")
include(":data-features:excluded-equipments")
include(":data-features:trainings")
include(":data-features:exercise-examples")
include(":data-features:exercise-metrics")

include(":ui-core:foundation")
include(":ui-core:state")
include(":ui-core:error:error-provider-impl")
include(":ui-core:error:error-provider")

include(":ui-screen-features:screen-api")
include(":ui-screen-features:authorization")
include(":ui-screen-features:trainings")
include(":ui-screen-features:profile")
include(":ui-screen-features:home")
include(":ui-screen-features:debug")
include(":ui-screen-features:training")

include(":ui-dialog-features:dialog-api")
include(":ui-dialog-features:exercise")
include(":ui-dialog-features:draft-training")
include(":ui-dialog-features:exercise-example")
include(":ui-dialog-features:error-display")
include(":ui-dialog-features:confirmation")
include(":ui-dialog-features:weight-picker")
include(":ui-dialog-features:height-picker")
include(":ui-dialog-features:date-picker")
include(":ui-dialog-features:month-picker")
include(":ui-dialog-features:filter-picker")
include(":ui-dialog-features:iteration-picker")
include(":ui-dialog-features:exercise-example-picker")
include(":ui-dialog-features:menu-picker")
include(":ui-dialog-features:profile")
include(":ui-dialog-features:statistics")
include(":ui-dialog-features:muscle-loading")
include(":ui-dialog-features:training-streak")
include(":ui-dialog-features:performance-trend")

include(":toolkit:context")
include(":toolkit:localization")
include(":toolkit:theme")
include(":toolkit:http-client")
include(":toolkit:image-loader")
include(":toolkit:logger")
include(":toolkit:connectivity")
include(":toolkit:serialization")
include(":toolkit:date-utils")

include(":compose-libs:wheel-picker")
include(":compose-libs:segment-control")
include(":compose-libs:konfetti")
include(":compose-libs:chart")

include(":data-mappers:entity-to-domain")
include(":data-mappers:dto-to-entity")
include(":data-mappers:dto-to-domain")
include(":data-mappers:domain-to-state")
include(":data-mappers:domain-to-entity")
include(":data-mappers:domain-to-dto")
include(":data-mappers:state-to-domain")
