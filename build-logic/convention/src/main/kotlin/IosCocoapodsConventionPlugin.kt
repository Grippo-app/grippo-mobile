import com.grippo.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension

class IosCocoapodsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.native.cocoapods")

            extensions.getByType<KotlinMultiplatformExtension>().apply {
                extensions.getByType<CocoapodsExtension>().apply {
                    version = "1.0"
                    summary = "Shared Code"
                    homepage = "https://github.com/voitenkodev/Grippo"
                    podfile = file("$rootDir/iosApp/Podfile")
                    ios.deploymentTarget = "16.0"

                    framework {
                        baseName = "shared"
                        isStatic = true
                        linkerOpts.add("-lsqlite3")

                        export(libs.findLibrary("decompose.core").get())
                        export(libs.findLibrary("decompose.essenty").get())
                        export(libs.findLibrary("decompose.state.keeper").get())
                        export(libs.findLibrary("decompose.back.handler").get())
                    }
                }
            }
        }
    }
}
