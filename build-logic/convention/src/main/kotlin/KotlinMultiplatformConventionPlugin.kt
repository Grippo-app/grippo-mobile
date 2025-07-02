import com.grippo.applySafely
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KotlinMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.applySafely("org.jetbrains.kotlin.multiplatform")

            extensions.getByType<KotlinMultiplatformExtension>().apply {
                explicitApi()

                androidTarget()
                iosX64()
                iosArm64()
                iosSimulatorArm64()

                applyDefaultHierarchyTemplate()

                sourceSets.configureEach {
                    languageSettings.optIn("androidx.compose.ui.text.ExperimentalTextApi")
                    languageSettings.optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                    languageSettings.optIn("androidx.compose.ui.ExperimentalComposeUiApi")
                    languageSettings.optIn("androidx.compose.foundation.layout.ExperimentalLayoutApi")
                    languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                    languageSettings.optIn("androidx.compose.ui.unit.ExperimentalUnitApi")
                    languageSettings.optIn("androidx.compose.animation.ExperimentalAnimationApi")
                    languageSettings.optIn("kotlin.time.ExperimentalTime")
                    languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                    languageSettings.optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
                    languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
                    languageSettings.optIn("com.arkivanov.decompose.DelicateDecomposeApi")
                    languageSettings.optIn("androidx.compose.animation.ExperimentalSharedTransitionApi")
                    languageSettings.optIn("kotlinx.datetime.format.FormatStringsInDatetimeFormats")
                    languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi")
                    languageSettings.optIn("com.arkivanov.decompose.ExperimentalDecomposeApi")
                }
            }
        }
    }
}