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
    buildUponDefaultConfig = true
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

// why: cross-project task aggregation kept config-cache safe via subprojects.map { p -> p.tasks.matching { ... } }

tasks.register("allHostTests") {
    group = "verification"
    description = "Runs every test-bearing module's Android host tests."
    dependsOn(subprojects.map { p -> p.tasks.matching { it.name == "hostTests" } })
}

tasks.register("allIosSimulatorTests") {
    group = "verification"
    description = "Runs every eligible module's Kotlin/Native tests on the iOS simulator."
    dependsOn(subprojects.map { p -> p.tasks.matching { it.name == "iosSimulatorTests" } })
}

tasks.register("allAndroidDeviceTests") {
    group = "verification"
    description = "Runs every device-enabled module's instrumented tests on connected devices."
    dependsOn(subprojects.map { p -> p.tasks.matching { it.name == "androidDeviceTests" } })
}

tasks.register("allScreenshotTests") {
    group = "verification"
    description = "Verifies every screenshot module's Roborazzi captures."
    dependsOn(subprojects.map { p -> p.tasks.matching { it.name == "verifyRoborazziAndroidHostTest" } })
}

tasks.register("allConfiguredTests") {
    group = "verification"
    description = "Runs every configured test lane (host, iOS simulator, device, screenshots)."
    dependsOn("allHostTests", "allIosSimulatorTests", "allAndroidDeviceTests", "allScreenshotTests")
}

tasks.register("testCapabilityInventory") {
    group = "verification"
    description = "Aggregates per-module test capability fragments into build/test-capability/inventory.json."
    dependsOn(subprojects.map { p -> p.tasks.matching { it.name == "testCapabilityEntry" } })
    val fragments = files(subprojects.map { p -> p.layout.buildDirectory.file("test-capability/entry.json") })
    inputs.files(fragments).skipWhenEmpty()
    val out = layout.buildDirectory.file("test-capability/inventory.json")
    outputs.file(out)
    doLast {
        val entries = fragments.files.filter { it.exists() }.map { it.readText() }.sorted()
        out.get().asFile.writeText(entries.joinToString(",\n", "[", "]\n"))
    }
}
