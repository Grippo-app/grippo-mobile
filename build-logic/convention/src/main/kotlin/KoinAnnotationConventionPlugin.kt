import com.google.devtools.ksp.gradle.KspExtension
import com.grippo.applySafely
import com.grippo.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

class KoinAnnotationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.applySafely("com.google.devtools.ksp")

        val kotlinExt = extensions.getByType<KotlinMultiplatformExtension>()

        kotlinExt.sourceSets.named("commonMain").configure {
            dependencies {
                implementation(libs.findLibrary("koin.core").get())
                api(libs.findLibrary("koin.annotations").get())
            }
        }

        dependencies {
            add("kspCommonMainMetadata", libs.findLibrary("koin.ksp.compiler").get())
        }

        extensions.getByType<KspExtension>().apply {
            arg("KOIN_CONFIG_CHECK", "true")
        }

        tasks.withType(KotlinCompilationTask::class.java).configureEach {
            if (name != "kspCommonMainKotlinMetadata") {
                dependsOn("kspCommonMainKotlinMetadata")
            }
        }
    }
}