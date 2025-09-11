import com.android.build.gradle.LibraryExtension
import com.grippo.applySafely
import com.grippo.configureJvmToolchain
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.applySafely("com.android.library")

            extensions.configure<LibraryExtension> {
                compileSdk = 36
                namespace = "com.grippo"

                defaultConfig {
                    minSdk = 26
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
