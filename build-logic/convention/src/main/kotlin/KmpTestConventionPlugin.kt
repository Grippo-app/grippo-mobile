import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.grippo.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // Captured project path — inside a task-configuration lambda an unqualified
        // `path` resolves to the task path, never the module path.
        val projectPath = path

        extensions.getByType<KotlinMultiplatformExtension>().apply {
            // THE single enabler call site for the whole build. Defaults stay
            // out of the enabler: capability plugins flip them reactively via
            // the compilation DSL (proven post-creation on the pinned stack).
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                withHostTest { }
            }
            sourceSets.configureEach {
                if (name == "commonTest") dependencies {
                    implementation(libs.findLibrary("kotlin.test").get())
                }
            }
        }

        // Capability inventory fragment: one JSON per module, aggregated by the
        // root `testCapabilityInventory` task (configuration-cache safe: the
        // root task consumes these files, never project state).
        tasks.register<TestCapabilityEntryTask>("testCapabilityEntry") {
            group = "verification"
            description = "Writes this module's test capability inventory fragment."
            modulePath.set(projectPath)
            capabilities.add("base")
            lanes.put("host", "$projectPath:testAndroidHostTest")
            outputFile.set(layout.buildDirectory.file("test-capability/entry.json"))
        }

        // Stable per-module lane aliases. Each depends on the REAL test task
        // provider — a missing provider fails the build instead of printing
        // "nothing to do". iOS/device aliases appear only on modules whose
        // targets configure those lanes.
        tasks.register("hostTests") {
            group = "verification"
            description = "Runs this module's Android host tests (testAndroidHostTest)."
            dependsOn(tasks.named("testAndroidHostTest"))
        }
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            extensions.getByType<KotlinMultiplatformExtension>().targets.configureEach {
                if (name == "iosSimulatorArm64") {
                    tasks.register("iosSimulatorTests") {
                        group = "verification"
                        description = "Runs this module's Kotlin/Native tests on the iOS simulator."
                        dependsOn(tasks.named("iosSimulatorArm64Test"))
                    }
                    tasks.named<TestCapabilityEntryTask>("testCapabilityEntry").configure {
                        lanes.put("ios-simulator", "$projectPath:iosSimulatorArm64Test")
                    }
                }
            }
        }
    }
}
