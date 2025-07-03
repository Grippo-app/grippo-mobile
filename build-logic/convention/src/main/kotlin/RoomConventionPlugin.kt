import com.google.devtools.ksp.gradle.KspAATask
import com.google.devtools.ksp.gradle.KspExtension
import com.grippo.applySafely
import com.grippo.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("com.google.devtools.ksp")

        val kotlinExt = extensions.getByType<KotlinMultiplatformExtension>()

        kotlinExt.sourceSets.named("commonMain") {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                implementation(libs.findLibrary("androidx.room.runtime").get())
                implementation(libs.findLibrary("sqlite.bundled").get())
                implementation(libs.findLibrary("sqlite").get())
            }
        }

        dependencies {
            add("kspAndroid", libs.findLibrary("androidx.room.compiler").get())
            add("kspIosX64", libs.findLibrary("androidx.room.compiler").get())
            add("kspIosArm64", libs.findLibrary("androidx.room.compiler").get())
            add("kspIosSimulatorArm64", libs.findLibrary("androidx.room.compiler").get())
        }

        extensions.configure<KspExtension> {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        project.afterEvaluate {
            tasks.withType(KspAATask::class.java).configureEach {
                if (name != "kspCommonMainKotlinMetadata") {
                    dependsOn("kspCommonMainKotlinMetadata")
                }
            }
        }
    }
}