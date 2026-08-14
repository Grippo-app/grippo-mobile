import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.grippo.applySafely
import com.grippo.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class RoomTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("kmp.test.convention")

        val projectPath = path

        enableAndroidDeviceLane()

        // The deviceTest compilation is standalone — it does not include
        // commonTest. Room fidelity is authored on the device lane; fast common
        // DAO tests run through commonTest against the iOS lane's real driver.
        extensions.getByType<KotlinMultiplatformExtension>().sourceSets.configureEach {
            if (name == "androidDeviceTest") dependencies {
                implementation(libs.findLibrary("androidx.room.testing").get())
                implementation(libs.findLibrary("androidx.test.runner").get())
                implementation(libs.findLibrary("androidx.test.core").get())
                implementation(libs.findLibrary("junit4").get())
                implementation(libs.findLibrary("kotlinx.coroutines.test").get())
            }
        }

        tasks.named<TestCapabilityEntryTask>("testCapabilityEntry").configure {
            capabilities.add("room")
            lanes.put("android-device", "$projectPath:connectedAndroidDeviceTest")
        }
    }
}

// Internal helper (build-logic, not a public plugin): the single owner of the
// device-lane enabler, today called only by room.test.convention. Fail-closed
// on Compose modules — CMP 1.10.3's CopyResourcesToAndroidAssetsTask cannot
// configure the deviceTest compilation.
internal fun Project.enableAndroidDeviceLane() {
    check(!pluginManager.hasPlugin("org.jetbrains.compose")) {
        "The Android device lane cannot be enabled on a Compose module at the pinned stack " +
            "(CMP 1.10.3 deviceTest resources task fails configuration validation)."
    }
    extensions.getByType<KotlinMultiplatformExtension>()
        .targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
            withDeviceTest { }
        }
    tasks.register("androidDeviceTests") {
        group = "verification"
        description = "Runs this module's instrumented tests on connected devices."
        dependsOn(tasks.named("connectedAndroidDeviceTest"))
    }
}
