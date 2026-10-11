import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.grippo.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KotlinMultiplatformTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                val kotlinVersion = libs.findVersion("kotlin").get().requiredVersion
                extensions.configure<KotlinMultiplatformExtension> {
                    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                        withHostTestBuilder {}
                    }
                    sourceSets.named("commonTest") {
                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
                        }
                    }
                    sourceSets.matching { it.name == "androidHostTest" }.configureEach {
                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
                            implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                        }
                    }
                }
            }
        }
    }
}
