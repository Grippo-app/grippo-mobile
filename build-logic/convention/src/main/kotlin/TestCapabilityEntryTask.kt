import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class TestCapabilityEntryTask : DefaultTask() {
    @get:Input abstract val modulePath: Property<String>
    @get:Input abstract val capabilities: SetProperty<String>
    // lane id -> exact executable Gradle task path (never a guessed suffix)
    @get:Input abstract val lanes: MapProperty<String, String>
    @get:OutputFile abstract val outputFile: RegularFileProperty

    @TaskAction
    fun write() {
        val laneJson = lanes.get().toSortedMap().entries.joinToString(",") {
            "\"${it.key}\":{\"taskPath\":\"${it.value}\"}"
        }
        val caps = capabilities.get().sorted().joinToString(",") { "\"$it\"" }
        outputFile.get().asFile.writeText(
            "{\"path\":\"${modulePath.get()}\",\"capabilities\":[$caps],\"lanes\":{$laneJson}}"
        )
    }
}
