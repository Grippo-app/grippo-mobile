import com.android.build.api.dsl.ApplicationExtension
import com.grippo.applySafely
import com.grippo.configureJvmToolchain
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.applySafely("com.android.application")
            pluginManager.applySafely("org.jetbrains.kotlin.android")

            extensions.configure<ApplicationExtension> {
                compileSdk = 36
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

            configureJvmToolchain(19)
        }
    }
}
