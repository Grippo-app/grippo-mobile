import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.grippo.applySafely
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ComposeMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.applySafely("org.jetbrains.kotlin.plugin.compose")
            pluginManager.applySafely("org.jetbrains.compose")

            // Enable Compose compiler metrics, reports, and per-project stability config.
            // - metrics/reports help to find non-skippable / non-restartable composables
            // - stabilityConfigurationFiles teaches the compiler that domain models / collections are stable,
            //   which is critical for skipping on Kotlin/Native (iOS), where recomposition is expensive.
            //
            // The extension is only registered after the kotlin.plugin.compose plugin has been applied,
            // so we configure it through `withPlugin` to avoid "Extension … not found" errors when the
            // convention plugin is applied before the compose plugin finishes wiring itself.
            pluginManager.withPlugin("org.jetbrains.kotlin.plugin.compose") {
                extensions.configure<ComposeCompilerGradlePluginExtension> {
                    metricsDestination.set(layout.buildDirectory.dir("compose-metrics"))
                    reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
                    stabilityConfigurationFiles.add(
                        rootProject.layout.projectDirectory.file("compose-stability.conf")
                    )
                }
            }

            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                extensions.configure<KotlinMultiplatformExtension> {
                    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                        packaging.resources.excludes += setOf(
                            "/META-INF/{AL2.0,LGPL2.1}"
                        )
                    }
                }
            }
        }
    }
}
