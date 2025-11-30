package com.grippo.data.features.suggestions.prompt.exercise.example.sections

import com.grippo.data.features.suggestions.prompt.exercise.example.utils.ExercisePromptSection
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.formatTitleLabel

/**
 * Example output:
 * ```
 * Force mix:
 *  - Push: 2
 *  - Pull: 1
 * ```
 * Or with three force types present:
 * ```
 * Force mix:
 *  - Pull: 3
 *  - Push: 2
 *  - Static: 1
 * ```
 * Section is skipped if the mix is empty or has only one entry.
 */
internal class ForceMixSection(
    mix: Map<String, Int>
) : BaseMixSection(label = "Force mix", mix = mix) {
    override val id: String = "force_mix"
    override val description: String = "Force type coverage"
}

/**
 * Example output:
 * ```
 * Weight mix:
 *  - Free: 1
 *  - Machine: 2
 * ```
 * If cables/bodyweight are also present:
 * ```
 * Weight mix:
 *  - Machine: 3
 *  - Free: 1
 *  - Bodyweight: 1
 * ```
 * Section is omitted for empty or single-entry mixes.
 */
internal class WeightMixSection(
    mix: Map<String, Int>
) : BaseMixSection(label = "Weight mix", mix = mix) {
    override val id: String = "weight_mix"
    override val description: String = "Loaded vs bodyweight mix"
}

/**
 * Example output:
 * ```
 * Experience mix:
 *  - Beginner: 1
 *  - Intermediate: 2
 * ```
 * For a session spanning all tiers:
 * ```
 * Experience mix:
 *  - Intermediate: 2
 *  - Beginner: 1
 *  - Advanced: 1
 * ```
 * Section is omitted when all exercises share the same experience level.
 */
internal class ExperienceMixSection(
    mix: Map<String, Int>
) : BaseMixSection(label = "Experience mix", mix = mix) {
    override val id: String = "experience_mix"
    override val description: String = "Experience tags distribution"
}

internal abstract class BaseMixSection(
    private val label: String,
    private val mix: Map<String, Int>
) : ExercisePromptSection {

    override fun render(into: StringBuilder) {
        if (mix.isEmpty() || mix.size <= 1) return
        into.appendLine()
        into.appendLine("$label:")
        mix.entries.sortedByDescending { it.value }.forEach { (key, value) ->
            into.appendLine(" - ${formatTitleLabel(key)}: $value")
        }
    }
}
