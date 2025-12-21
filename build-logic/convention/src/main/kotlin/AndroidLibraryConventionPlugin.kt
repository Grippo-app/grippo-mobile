import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.grippo.applySafely
import com.grippo.configureJvmToolchain
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.applySafely("com.android.kotlin.multiplatform.library")

            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                extensions.configure<KotlinMultiplatformExtension> {
                    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                        compileSdk = 36
                        minSdk = 26
                        namespace = "com.grippo"
                    }
                }
            }

            configureJvmToolchain(19)
        }
    }
}
