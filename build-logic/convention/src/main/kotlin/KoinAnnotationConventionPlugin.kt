import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

class KoinAnnotationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        val kotlinExt = extensions.getByType<KotlinMultiplatformExtension>()

        kotlinExt.sourceSets.named("commonMain").configure {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin") // Add KSP-generated code
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