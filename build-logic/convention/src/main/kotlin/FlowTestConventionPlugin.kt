import com.grippo.applySafely
import com.grippo.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class FlowTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("coroutines.test.convention")

        extensions.getByType<KotlinMultiplatformExtension>().sourceSets.configureEach {
            if (name == "commonTest") dependencies {
                implementation(libs.findLibrary("turbine").get())
            }
        }

        tasks.named<TestCapabilityEntryTask>("testCapabilityEntry").configure {
            capabilities.add("flow")
        }
    }
}
