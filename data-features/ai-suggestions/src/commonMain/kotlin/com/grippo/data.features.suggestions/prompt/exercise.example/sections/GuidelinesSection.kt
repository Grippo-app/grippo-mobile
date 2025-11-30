package com.grippo.data.features.suggestions.prompt.exercise.example.sections

import com.grippo.data.features.suggestions.prompt.exercise.example.config.PromptGuidelineConfig
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.ExercisePromptSection
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.dominantKey
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.formatTitleLabel

/**
 * Example output:
 * ```
 * Guidelines:
 *  - Cover primary-muscle deficits first (primary-only accounting).
 *  - Cycles: prefer due/overdue primary muscles...
 * ```
 * When a dominant experience tier is detected:
 * ```
 * Guidelines:
 *  ...
 *  - Keep difficulty around the Intermediate level.
 * ```
 * When locale-specific language cues need to be highlighted:
 * ```
 * Guidelines:
 *  - Cover primary-muscle deficits first (primary-only accounting).
 *  - Cycles: prefer due/overdue primary muscles...
 *  - ...
 *  - IMPORTANT: Write the `reason` in friendly, user-facing language:
 *    * No raw metrics, IDs, percentages, scores, or decimals.
 *    * Explain simply why this exercise fits now (target, freshness, balance, variety).
 *    * You may include a brief set/rep/rest cue (e.g., "3x8 @ RIR2, rest 90s").
 *    * Language: use 'ru-RU' for the reason.
 * ```
 */
internal class GuidelinesSection(
    private val experienceMix: Map<String, Int>,
    private val locale: String
) : ExercisePromptSection {

    override val id: String = "guidelines"
    override val description: String = "LLM steering cues"

    override fun render(into: StringBuilder) {
        val dominantExperience = experienceMix.dominantKey()?.let { formatTitleLabel(it) }

        into.appendLine()
        into.appendLine("Guidelines:")
        into.appendLine(" - Cover primary-muscle deficits first (primary-only accounting).")
        into.appendLine(" - Cycles: prefer due/overdue primary muscles by macro (days) or micro (sessions), both with grace.")
        into.appendLine(" - Respect recovery windows: skip if primary not recovered; avoid candidates with >60% weighted-unrecovered share (adaptive cap).")
        into.appendLine(" - Per-target muscle, steer category toward ~1:1 compound/isolation in this session (use history as reference).")
        into.appendLine(" - Global category: compounds earlier; if a category has a positive deficit, prioritize it.")
        into.appendLine(" - Mild anti-monotony: small penalty if the same primary muscle had meaningful load recently (< ${PromptGuidelineConfig.ANTI_MONOTONY_HOURS}h).")
        into.appendLine(" - Equipment tiebreak: prefer candidates whose equipmentIds DO NOT intersect with equipment used in this session.")
        into.appendLine(" - Promote variety: lower usageCount, older lastUsed. Avoid already performed; respect user exclusions.")
        dominantExperience?.let { into.appendLine(" - Keep difficulty around the $it level.") }
        into.appendLine(" - IMPORTANT: Write the `reason` in friendly, user-facing language:")
        into.appendLine("   * No raw metrics, IDs, percentages, scores, or decimals.")
        into.appendLine("   * Explain simply why this exercise fits now (target, freshness, balance, variety).")
        into.appendLine("   * You may include a brief set/rep/rest cue (e.g., \"3x8 @ RIR2, rest 90s\").")
        into.appendLine("   * Language: use '$locale' for the reason.")
    }
}
