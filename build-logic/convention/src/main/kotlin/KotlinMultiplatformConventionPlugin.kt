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
                    languageSettings.apply {
                        optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                        optIn("androidx.compose.ui.text.ExperimentalTextApi")
                        optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                        optIn("androidx.compose.ui.ExperimentalComposeUiApi")
                        optIn("androidx.compose.foundation.layout.ExperimentalLayoutApi")
                        optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                        optIn("androidx.compose.ui.unit.ExperimentalUnitApi")
                        optIn("androidx.compose.animation.ExperimentalAnimationApi")
                        optIn("kotlin.time.ExperimentalTime")
                        optIn("kotlinx.cinterop.ExperimentalForeignApi")
                        optIn("com.arkivanov.decompose.DelicateDecomposeApi")
                        optIn("androidx.compose.animation.ExperimentalSharedTransitionApi")
                        optIn("kotlin.uuid.ExperimentalUuidApi")
                        optIn("com.arkivanov.decompose.ExperimentalDecomposeApi")

                        // Emit the compilation warning on expect/actual classes. The warning must mention that expect/actual classes are in Beta
                        // https://youtrack.jetbrains.com/issue/KT-61573
                        targets.all {
                            compilations.all {
                                compileTaskProvider.configure {
                                    compilerOptions {
                                        freeCompilerArgs.add("-Xexpect-actual-classes")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}