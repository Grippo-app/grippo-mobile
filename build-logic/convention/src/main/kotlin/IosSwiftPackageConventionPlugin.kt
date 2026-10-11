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

/**
 * Rebuild the complete artifact before using Xcode's manual XCFramework integration:
 * Debug: ./gradlew :shared:assembleSharedDebugXCFramework
 * Release: ./gradlew :shared:assembleSharedReleaseXCFramework
 * These tasks package both Kotlin code and matching Compose resources. Linking an individual
 * framework alone leaves resource offsets inconsistent with an older XCFramework bundle.
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
                    .matching { nativeTarget -> nativeTarget.konanTarget.family.isAppleFamily }
                    .configureEach {
                        // Ensure the binary frameworks are actually registered for every target
                        binaries.framework(listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)) {
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
                            ).forEach { exportedDep -> export(exportedDep) }

                            // SDK-independent platform service contracts for Swift adapters
                            export(project(":toolkit:crash-reporting:api"))
                            export(project(":toolkit:messaging:api"))
                            export(project(":toolkit:analytics:api"))
                        }
                    }
            }
        }
    }
}
