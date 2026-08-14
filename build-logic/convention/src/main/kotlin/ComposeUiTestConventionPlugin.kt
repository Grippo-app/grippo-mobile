import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.grippo.applySafely
import com.grippo.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ComposeUiTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("kmp.test.convention")

        val compose = extensions.getByType<ComposeExtension>()

        extensions.getByType<KotlinMultiplatformExtension>().apply {
            sourceSets.apply {
                // why: common Compose UI tests NPE on a plain host and CMP 1.10.3 can't configure withDeviceTest on a Compose module — iOS-sim + Android/Robolectric host only, never the device lane
                val uiTest = maybeCreate("uiTest").apply {
                    dependsOn(getByName("commonTest"))
                    dependencies {
                        @OptIn(ExperimentalComposeLibrary::class)
                        implementation(compose.dependencies.uiTest)
                    }
                }
                configureEach {
                    if (name == "iosSimulatorArm64Test" || name == "androidHostTest") dependsOn(uiTest)
                    if (name == "androidHostTest") dependencies {
                        implementation(libs.findLibrary("robolectric").get())
                        implementation(libs.findLibrary("junit4").get())
                        implementation(libs.findLibrary("compose.ui.test.manifest").get())
                    }
                }
            }
            // Reactive default — the enabler stays untouched in the base plugin.
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                compilations.configureEach {
                    if (name == "hostTest") {
                        (this as KotlinMultiplatformAndroidHostTestCompilation).isIncludeAndroidResources = true
                    }
                }
            }
        }

        // Robolectric's own sandbox demands Java 21 for SDK 36 ("Android SDK 36
        // requires Java 21"). Narrowed to the exact host task — never a broad
        // tasks.withType<Test> that would silently move every general test in
        // the module onto another launcher.
        val toolchains = extensions.getByType<JavaToolchainService>()
        tasks.withType<Test>().matching { it.name == "testAndroidHostTest" }.configureEach {
            javaLauncher.set(toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
        }

        tasks.named<TestCapabilityEntryTask>("testCapabilityEntry").configure {
            capabilities.add("compose-ui")
        }
    }
}
