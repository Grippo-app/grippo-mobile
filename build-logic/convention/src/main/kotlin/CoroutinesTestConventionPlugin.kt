import com.grippo.applySafely
import com.grippo.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class CoroutinesTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("kmp.test.convention")

        extensions.getByType<KotlinMultiplatformExtension>().sourceSets.configureEach {
            if (name == "commonTest") dependencies {
                implementation(libs.findLibrary("kotlinx.coroutines.test").get())
            }
        }

        tasks.named<TestCapabilityEntryTask>("testCapabilityEntry").configure {
            capabilities.add("coroutines")
        }
    }
}
