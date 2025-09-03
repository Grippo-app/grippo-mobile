import com.android.build.gradle.LibraryExtension
import com.grippo.applySafely
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class ComposeMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.applySafely("org.jetbrains.kotlin.plugin.compose")
            pluginManager.applySafely("org.jetbrains.compose")

            extensions.getByType<LibraryExtension>().apply {
                buildFeatures.compose = true

                packaging.resources.excludes += setOf(
                    "/META-INF/{AL2.0,LGPL2.1}"
                )
            }
        }
    }
}