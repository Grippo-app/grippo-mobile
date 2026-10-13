import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.grippo.applySafely
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ComposeMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.applySafely("org.jetbrains.kotlin.plugin.compose")
            pluginManager.applySafely("org.jetbrains.compose")

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
