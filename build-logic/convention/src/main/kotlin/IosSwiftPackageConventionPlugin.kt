import com.grippo.applySafely
import com.grippo.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

class IosSwiftPackageConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.applySafely("org.jetbrains.kotlin.multiplatform")

            extensions.getByType<KotlinMultiplatformExtension>().apply {
                val xcf = XCFramework("shared")

                targets
                    .withType<KotlinNativeTarget>()
                    .matching { nativeTarget -> nativeTarget.konanTarget.family.isAppleFamily }
                    .configureEach {
                        binaries.framework(listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)) {
                            baseName = "shared"
                            isStatic = true

                            linkerOpts.add("-lsqlite3")

                            xcf.add(this)

                            listOf(
                                libs.findLibrary("decompose.core").get(),
                                libs.findLibrary("decompose.essenty").get(),
                                libs.findLibrary("decompose.state.keeper").get(),
                                libs.findLibrary("decompose.back.handler").get()
                            ).forEach { exportedDep -> export(exportedDep) }

                            export(project(":toolkit:crash-reporting:api"))
                            export(project(":toolkit:messaging:api"))
                            export(project(":toolkit:analytics:api"))
                        }
                    }
            }
        }
    }
}
