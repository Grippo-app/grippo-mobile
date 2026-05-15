import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.kotlin.multiplatform.library).apply(false)
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.jetbrains.compose).apply(false)
    alias(libs.plugins.kotlin.serialization).apply(false)
    alias(libs.plugins.kotlin.parcelize).apply(false)
    alias(libs.plugins.google.services).apply(false)
    alias(libs.plugins.firebase.crashlytics).apply(false)
    alias(libs.plugins.room).apply(false)
    alias(libs.plugins.ksp).apply(false)
    alias(libs.plugins.detekt)
}

apply(from = "secure/secure.gradle.kts")

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = false
    parallel = true
    autoCorrect = false
    source.setFrom(
        files(
            "androidApp/src",
            "shared/src",
            "compose-libs",
            "data-features",
            "data-mappers",
            "data-services",
            "design-system",
            "toolkit",
            "ui-core",
            "ui-dialog-features",
            "ui-screen-features",
        )
    )
}

dependencies {
    detektPlugins(projects.tooling.detektRules)
}

tasks.withType<Detekt>().configureEach {
    reports {
        sarif.required.set(true)
        sarif.outputLocation.set(layout.buildDirectory.file("reports/detekt/sarif.json"))
        xml.required.set(false)
        html.required.set(false)
        md.required.set(false)
        txt.required.set(false)
    }
    exclude("**/build/**", "**/schemas/**", "**/compose-metrics/**", "**/compose-reports/**", "**/.kotlin/**")
}
