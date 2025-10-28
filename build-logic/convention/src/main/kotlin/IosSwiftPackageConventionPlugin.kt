import com.grippo.applySafely
import com.grippo.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

/**
 * How to refresh shared.xcframework after editing Kotlin code:
 * 1. For Debug, run `./gradlew :shared:assembleSharedDebugXCFramework`; Xcode will pick up the updated bundle at shared/build/XCFrameworks/debug/shared.xcframework automatically.
 * 2. For Release, run `./gradlew :shared:assembleSharedReleaseXCFramework` and update the Xcode reference to ../shared/build/XCFrameworks/release/shared.xcframework.
 * 3. Build the target in Xcode afterwards; if anything gets stuck, try Product > Clean Build Folder and rebuild.
 */
class IosSwiftPackageConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Make sure KMP plugin is applied (safe no-op if it's already there)
            pluginManager.applySafely("org.jetbrains.kotlin.multiplatform")

            extensions.getByType<KotlinMultiplatformExtension>().apply {
                // Aggregate all iOS frameworks into a single XCFramework
                // NOTE: The argument "shared" should match the framework baseName you want to ship.
                val xcf = XCFramework("shared")

                // Configure every Apple (iOS, simulator) native target
                targets
                    .withType<KotlinNativeTarget>()
                    .matching { nativeTarget ->
                        nativeTarget.konanTarget.family.isAppleFamily
                    }
                    .configureEach {
                        // Ensure the binary frameworks are actually registered for every target
                        binaries.framework {
                            baseName = "shared"
                            isStatic = true

                            // Required for Room/SQLite usage on iOS
                            linkerOpts.add("-lsqlite3")

                            // Include this framework into the aggregated XCFramework
                            xcf.add(this)

                            // Re-export transitive deps so Swift sees them
                            listOf(
                                libs.findLibrary("decompose.core").get(),
                                libs.findLibrary("decompose.essenty").get(),
                                libs.findLibrary("decompose.state.keeper").get(),
                                libs.findLibrary("decompose.back.handler").get()
                            ).forEach { exportedDep ->
                                export(exportedDep)
                            }
                        }
                    }
            }
        }
    }
}
