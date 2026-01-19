package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable

@Immutable
public data class TrainingLoadProfileState(
    val kind: TrainingProfileKindState,
    val dimensions: List<TrainingDimensionScoreState>,
    val dominant: TrainingDimensionKindState?,
    val confidence: Int,
) {
    public fun title(): String = kind.title(this)

    public fun subtitle(): String = kind.subtitle(this)

    public fun details(): String = kind.details(this)

    public fun tip(): String? = kind.tip(this)

    public fun description(): String {
        val t = title()
        val s = subtitle()
        val d = details()
        val tip = tip()

        return buildString {
            appendLine(t)
            appendLine(s)
            append(d)
            if (!tip.isNullOrBlank()) {
                appendLine()
                append(tip)
            }
        }
    }

    public fun confidenceLabel(): String {
        val c = confidence.coerceIn(0, 100)
        return when (c) {
            in 0..33 -> "Low confidence"
            in 34..66 -> "Medium confidence"
            else -> "High confidence"
        }
    }

    public fun focusTitleForAxis(axis: TrainingDimensionKindState): String {
        val c = confidence.coerceIn(0, 100)
        val base = axis.label()
        return when {
            c >= 70 -> "$base-dominant"
            c >= 35 -> "$base-leaning"
            else -> "$base (low confidence)"
        }
    }

    public fun distributionLine(): String {
        val s = scoreOf(TrainingDimensionKindState.Strength)
        val h = scoreOf(TrainingDimensionKindState.Hypertrophy)
        val e = scoreOf(TrainingDimensionKindState.Endurance)
        return "Strength $s • Hypertrophy $h • Endurance $e"
    }

    public fun dominanceLine(): String {
        val s = scoreOf(TrainingDimensionKindState.Strength)
        val h = scoreOf(TrainingDimensionKindState.Hypertrophy)
        val e = scoreOf(TrainingDimensionKindState.Endurance)

        val top = maxOf(s, h, e)
        val low = minOf(s, h, e)
        val second = s + h + e - top - low
        val gap = (top - second).coerceAtLeast(0)

        val axis = dominant
        return if (axis == null) {
            "No clear focus: top is only +$gap over second."
        } else {
            "${axis.label()} leads by +$gap."
        }
    }

    public fun scoreOf(kind: TrainingDimensionKindState): Int {
        return dimensions.firstOrNull { it.kind == kind }?.score?.coerceIn(0, 100) ?: 0
    }
}

@Immutable
public enum class TrainingDimensionKindState {
    Strength,
    Hypertrophy,
    Endurance;

    public fun axisId(): String {
        return when (this) {
            Strength -> "strength"
            Hypertrophy -> "hypertrophy"
            Endurance -> "endurance"
        }
    }

    public fun label(): String {
        return when (this) {
            Strength -> "Strength"
            Hypertrophy -> "Hypertrophy"
            Endurance -> "Endurance"
        }
    }
}

@Immutable
public enum class TrainingProfileKindState {
    Strength,
    Hypertrophy,
    Endurance,
    Powerbuilding,
    Mixed,
    Easy;

    public fun label(): String {
        return when (this) {
            Strength -> "Strength"
            Hypertrophy -> "Hypertrophy"
            Endurance -> "Endurance"
            Powerbuilding -> "Powerbuilding"
            Mixed -> "Mixed"
            Easy -> "Easy"
        }
    }

    public fun title(profile: TrainingLoadProfileState): String {
        return when (this) {
            Easy -> "Easy session"
            Powerbuilding -> "Powerbuilding • ${profile.confidenceLabel()}"
            Mixed -> "Mixed • ${profile.confidenceLabel()}"
            Strength -> "${profile.focusTitleForAxis(TrainingDimensionKindState.Strength)} • ${profile.confidenceLabel()}"
            Hypertrophy -> "${profile.focusTitleForAxis(TrainingDimensionKindState.Hypertrophy)} • ${profile.confidenceLabel()}"
            Endurance -> "${profile.focusTitleForAxis(TrainingDimensionKindState.Endurance)} • ${profile.confidenceLabel()}"
        }
    }

    public fun subtitle(profile: TrainingLoadProfileState): String {
        return when (this) {
            Easy -> "Recovery / technique / low-stress day."
            Powerbuilding -> "Heavy work + volume in the same session."
            Mixed -> "No single axis dominates today."
            Strength -> "More of your work leans toward strength stimulus."
            Hypertrophy -> "More of your work leans toward muscle-building stimulus."
            Endurance -> "More of your work leans toward endurance / density."
        }
    }

    public fun details(profile: TrainingLoadProfileState): String {
        val distribution = profile.distributionLine()
        return when (this) {
            Easy -> "Low overall load.  $distribution"
            Powerbuilding -> "Strength and hypertrophy are both strong.  $distribution"
            Mixed -> "${profile.dominanceLine()}  $distribution"
            Strength -> "${profile.dominanceLine()}  $distribution"
            Hypertrophy -> "${profile.dominanceLine()}  $distribution"
            Endurance -> "${profile.dominanceLine()}  $distribution"
        }
    }

    public fun tip(profile: TrainingLoadProfileState): String? {
        val c = profile.confidence.coerceIn(0, 100)
        return when (this) {
            Easy -> "If it felt too easy: add 1 hard set to your main lift or push closer to failure."
            Powerbuilding -> "Simple structure: 1–3 heavy sets + 2–4 back-off sets (6–10 reps)."
            Mixed -> "Want a clearer profile next time? Choose one knob: heavier / more sets / shorter rests."
            Strength -> when {
                c >= 70 -> "Keep heavy sets crisp: longer rests, fewer junk sets, track top sets."
                c >= 35 -> "To push strength: add a heavier top set, then reduce accessories."
                else -> "To make it clearly strength-focused: lower reps, longer rests, fewer exercises."
            }

            Hypertrophy -> when {
                c >= 70 -> "Stay consistent: 6–12 reps, stable volume, progress via reps or load."
                c >= 35 -> "To push size: add 1–2 hard sets to key muscles and keep reps controlled."
                else -> "To make it clearly hypertrophy-focused: more working sets in 6–12 reps, shorter rests."
            }

            Endurance -> when {
                c >= 70 -> "Density is king: shorter rests, keep pace, avoid long downtime."
                c >= 35 -> "To push endurance: slightly higher reps and reduce rest times."
                else -> "To make it clearly endurance-focused: higher reps, shorter rests, keep pace."
            }
        }
    }
}

@Immutable
public data class TrainingDimensionScoreState(
    val kind: TrainingDimensionKindState,
    val score: Int, // 0..100
) {
    public fun segment(): String = "${kind.label()} ${score.coerceIn(0, 100)}"
}
