import com.grippo.applySafely
import com.grippo.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension

class IosCocoapodsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.applySafely("org.jetbrains.kotlin.native.cocoapods")

            extensions.getByType<KotlinMultiplatformExtension>().apply {
                extensions.getByType<CocoapodsExtension>().apply {
                    version = "1.0"
                    summary = "Shared Code"
                    homepage = "https://github.com/voitenkodev/Grippo"
                    authors = "Maxim Voitenko"
                    license = "MIT"
                    ios.deploymentTarget = "16.0"
                    podfile = file("$rootDir/iosApp/Podfile")

                    framework {
                        baseName = "shared"
                        isStatic = true
                        linkerOpts.add("-lsqlite3")

                        listOf(
                            libs.findLibrary("decompose.core").get(),
                            libs.findLibrary("decompose.essenty").get(),
                            libs.findLibrary("decompose.state.keeper").get(),
                            libs.findLibrary("decompose.back.handler").get()
                        ).forEach { export(it) }
                    }
                }
            }
        }
    }
}
