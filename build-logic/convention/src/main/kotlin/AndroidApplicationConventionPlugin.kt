import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.grippo.applySafely
import com.grippo.configureJvmToolchain
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.exclude

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.applySafely("com.android.application")

            extensions.configure<ApplicationExtension> {
                compileSdk = 37
                namespace = "com.grippo"

                defaultConfig {
                    minSdk = 26
                    targetSdk = 36
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_19
                    targetCompatibility = JavaVersion.VERSION_19
                }
            }

            extensions.configure<ApplicationAndroidComponentsExtension> {
                onVariants(selector().withBuildType(RELEASE_BUILD_TYPE)) { variant ->
                    PREVIEW_TOOLING_GROUPS.forEach { group ->
                        PREVIEW_TOOLING_MODULES.forEach { module ->
                            variant.runtimeConfiguration.exclude(group = group, module = module)
                        }
                    }
                }
            }

            configureJvmToolchain(19)
        }
    }

    private companion object {
        const val RELEASE_BUILD_TYPE = "release"
        val PREVIEW_TOOLING_GROUPS = listOf("androidx.compose.ui", "org.jetbrains.compose.ui")
        val PREVIEW_TOOLING_MODULES = listOf(
            "ui-tooling",
            "ui-tooling-android",
            "ui-tooling-data",
            "ui-tooling-data-android",
        )
    }
}
