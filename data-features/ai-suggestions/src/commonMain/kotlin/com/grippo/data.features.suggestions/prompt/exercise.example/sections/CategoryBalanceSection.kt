package com.grippo.data.features.suggestions.prompt.exercise.example.sections

import com.grippo.data.features.suggestions.prompt.exercise.example.config.SuggestionMath
import com.grippo.data.features.suggestions.prompt.exercise.example.model.CategoryStats
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.ExercisePromptSection
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.formatOneDecimal
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.formatTitleLabel

/**
 * Example output:
 * ```
 * Category balance (done / avg):
 *  - Compound: 2 / avg 1.5 (+0.5)
 *  - Isolation: 1 / avg 2.0 (-1.0)
 * ```
 * Or when historical data is missing:
 * ```
 * Category balance (done / avg):
 *  - Compound: 0 / avg 0.0 (balanced)
 *  - Isolation: 0 / avg 0.0 (balanced)
 * ```
 * When several categories show surpluses/deficits simultaneously:
 * ```
 * Category balance (done / avg):
 *  - Compound: 1 / avg 2.5 (+1.5)
 *  - Isolation: 3 / avg 1.0 (-2.0)
 *  - Plyo: 0 / avg 0.0 (balanced)
 * ```
 */
internal class CategoryBalanceSection(
    private val stats: CategoryStats
) : ExercisePromptSection {

    override val id: String = "category_balance"
    override val description: String = "Category balance vs history"

    override fun render(into: StringBuilder) {
        val categories = (stats.average.keys + stats.current.keys).toSet()
        if (categories.isEmpty()) return

        into.appendLine()
        into.appendLine("Category balance (done / avg):")
        categories.forEach { key ->
            val avg = stats.average[key] ?: 0.0
            val done = stats.current[key] ?: 0
            val diff = stats.deficit(key)
            val diffLabel = when {
                diff > SuggestionMath.DEFICIT_EPS -> "+${formatOneDecimal(diff)}"
                diff < -SuggestionMath.DEFICIT_EPS -> formatOneDecimal(diff)
                else -> "balanced"
            }
            into.appendLine(" - ${formatTitleLabel(key)}: $done / avg ${formatOneDecimal(avg)} ($diffLabel)")
        }
    }
}
