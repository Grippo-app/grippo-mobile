package com.grippo.core.state.metrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.high_confidence
import com.grippo.design.resources.provider.low_confidence
import com.grippo.design.resources.provider.medium_confidence
import com.grippo.design.resources.provider.training_dimension_endurance
import com.grippo.design.resources.provider.training_dimension_hypertrophy
import com.grippo.design.resources.provider.training_dimension_strength
import com.grippo.design.resources.provider.training_profile_axis_endurance_id
import com.grippo.design.resources.provider.training_profile_axis_hypertrophy_id
import com.grippo.design.resources.provider.training_profile_axis_strength_id
import com.grippo.design.resources.provider.training_profile_details_dominance_distribution
import com.grippo.design.resources.provider.training_profile_details_easy
import com.grippo.design.resources.provider.training_profile_details_powerbuilding
import com.grippo.design.resources.provider.training_profile_distribution
import com.grippo.design.resources.provider.training_profile_focus_dominant
import com.grippo.design.resources.provider.training_profile_focus_leaning
import com.grippo.design.resources.provider.training_profile_focus_low_confidence
import com.grippo.design.resources.provider.training_profile_kind_easy
import com.grippo.design.resources.provider.training_profile_kind_endurance
import com.grippo.design.resources.provider.training_profile_kind_hypertrophy
import com.grippo.design.resources.provider.training_profile_kind_mixed
import com.grippo.design.resources.provider.training_profile_kind_powerbuilding
import com.grippo.design.resources.provider.training_profile_kind_strength
import com.grippo.design.resources.provider.training_profile_leads_by
import com.grippo.design.resources.provider.training_profile_no_clear_focus
import com.grippo.design.resources.provider.training_profile_subtitle_easy
import com.grippo.design.resources.provider.training_profile_subtitle_endurance
import com.grippo.design.resources.provider.training_profile_subtitle_hypertrophy
import com.grippo.design.resources.provider.training_profile_subtitle_mixed
import com.grippo.design.resources.provider.training_profile_subtitle_powerbuilding
import com.grippo.design.resources.provider.training_profile_subtitle_strength
import com.grippo.design.resources.provider.training_profile_tip_easy
import com.grippo.design.resources.provider.training_profile_tip_endurance_high
import com.grippo.design.resources.provider.training_profile_tip_endurance_low
import com.grippo.design.resources.provider.training_profile_tip_endurance_medium
import com.grippo.design.resources.provider.training_profile_tip_hypertrophy_high
import com.grippo.design.resources.provider.training_profile_tip_hypertrophy_low
import com.grippo.design.resources.provider.training_profile_tip_hypertrophy_medium
import com.grippo.design.resources.provider.training_profile_tip_mixed
import com.grippo.design.resources.provider.training_profile_tip_powerbuilding
import com.grippo.design.resources.provider.training_profile_tip_strength_high
import com.grippo.design.resources.provider.training_profile_tip_strength_low
import com.grippo.design.resources.provider.training_profile_tip_strength_medium
import com.grippo.design.resources.provider.training_profile_title_easy_session
import com.grippo.design.resources.provider.training_profile_title_with_confidence

@Immutable
public data class TrainingLoadProfileState(
    val kind: TrainingProfileKindState,
    val dimensions: List<TrainingDimensionScoreState>,
    val dominant: TrainingDimensionKindState?,
    val confidence: Int,
) {
    @Composable
    public fun title(): String = kind.title(this)

    @Composable
    public fun subtitle(): String = kind.subtitle(this)

    @Composable
    public fun details(): String = kind.details(this)

    @Composable
    public fun tip(): String? = kind.tip(this)

    @Composable
    public fun confidenceLabel(): String {
        val c = confidence.coerceIn(0, 100)
        return when (c) {
            in 0..33 -> AppTokens.strings.res(Res.string.low_confidence)
            in 34..66 -> AppTokens.strings.res(Res.string.medium_confidence)
            else -> AppTokens.strings.res(Res.string.high_confidence)
        }
    }

    @Composable
    public fun focusTitleForAxis(axis: TrainingDimensionKindState): String {
        val c = confidence.coerceIn(0, 100)
        val base = axis.label()
        return when {
            c >= 70 -> AppTokens.strings.res(Res.string.training_profile_focus_dominant, base)
            c >= 35 -> AppTokens.strings.res(Res.string.training_profile_focus_leaning, base)
            else -> AppTokens.strings.res(
                Res.string.training_profile_focus_low_confidence,
                base,
                AppTokens.strings.res(Res.string.low_confidence),
            )
        }
    }

    @Composable
    public fun distributionLine(): String {
        val s = scoreOf(TrainingDimensionKindState.Strength)
        val h = scoreOf(TrainingDimensionKindState.Hypertrophy)
        val e = scoreOf(TrainingDimensionKindState.Endurance)
        return AppTokens.strings.res(
            Res.string.training_profile_distribution,
            TrainingDimensionKindState.Strength.label(),
            s,
            TrainingDimensionKindState.Hypertrophy.label(),
            h,
            TrainingDimensionKindState.Endurance.label(),
            e,
        )
    }

    @Composable
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
            AppTokens.strings.res(Res.string.training_profile_no_clear_focus, gap)
        } else {
            AppTokens.strings.res(Res.string.training_profile_leads_by, axis.label(), gap)
        }
    }

    @Composable
    public fun scoreOf(kind: TrainingDimensionKindState): Int {
        return dimensions.firstOrNull { it.kind == kind }?.score?.coerceIn(0, 100) ?: 0
    }
}

@Immutable
public enum class TrainingDimensionKindState {
    Strength,
    Hypertrophy,
    Endurance;

    @Composable
    public fun axisId(): String {
        return when (this) {
            Strength -> AppTokens.strings.res(Res.string.training_profile_axis_strength_id)
            Hypertrophy -> AppTokens.strings.res(Res.string.training_profile_axis_hypertrophy_id)
            Endurance -> AppTokens.strings.res(Res.string.training_profile_axis_endurance_id)
        }
    }

    @Composable
    public fun label(): String {
        return when (this) {
            Strength -> AppTokens.strings.res(Res.string.training_dimension_strength)
            Hypertrophy -> AppTokens.strings.res(Res.string.training_dimension_hypertrophy)
            Endurance -> AppTokens.strings.res(Res.string.training_dimension_endurance)
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

    @Composable
    public fun label(): String {
        return when (this) {
            Strength -> AppTokens.strings.res(Res.string.training_profile_kind_strength)
            Hypertrophy -> AppTokens.strings.res(Res.string.training_profile_kind_hypertrophy)
            Endurance -> AppTokens.strings.res(Res.string.training_profile_kind_endurance)
            Powerbuilding -> AppTokens.strings.res(Res.string.training_profile_kind_powerbuilding)
            Mixed -> AppTokens.strings.res(Res.string.training_profile_kind_mixed)
            Easy -> AppTokens.strings.res(Res.string.training_profile_kind_easy)
        }
    }

    @Composable
    public fun title(profile: TrainingLoadProfileState): String {
        return when (this) {
            Easy -> AppTokens.strings.res(Res.string.training_profile_title_easy_session)
            Powerbuilding -> AppTokens.strings.res(
                Res.string.training_profile_title_with_confidence,
                label(),
                profile.confidenceLabel(),
            )

            Mixed -> AppTokens.strings.res(
                Res.string.training_profile_title_with_confidence,
                label(),
                profile.confidenceLabel(),
            )

            Strength -> AppTokens.strings.res(
                Res.string.training_profile_title_with_confidence,
                profile.focusTitleForAxis(TrainingDimensionKindState.Strength),
                profile.confidenceLabel(),
            )

            Hypertrophy -> AppTokens.strings.res(
                Res.string.training_profile_title_with_confidence,
                profile.focusTitleForAxis(TrainingDimensionKindState.Hypertrophy),
                profile.confidenceLabel(),
            )

            Endurance -> AppTokens.strings.res(
                Res.string.training_profile_title_with_confidence,
                profile.focusTitleForAxis(TrainingDimensionKindState.Endurance),
                profile.confidenceLabel(),
            )
        }
    }

    @Composable
    public fun subtitle(profile: TrainingLoadProfileState): String {
        return when (this) {
            Easy -> AppTokens.strings.res(Res.string.training_profile_subtitle_easy)
            Powerbuilding -> AppTokens.strings.res(Res.string.training_profile_subtitle_powerbuilding)
            Mixed -> AppTokens.strings.res(Res.string.training_profile_subtitle_mixed)
            Strength -> AppTokens.strings.res(Res.string.training_profile_subtitle_strength)
            Hypertrophy -> AppTokens.strings.res(Res.string.training_profile_subtitle_hypertrophy)
            Endurance -> AppTokens.strings.res(Res.string.training_profile_subtitle_endurance)
        }
    }

    @Composable
    public fun details(profile: TrainingLoadProfileState): String {
        val distribution = profile.distributionLine()
        return when (this) {
            Easy -> AppTokens.strings.res(
                Res.string.training_profile_details_easy,
                distribution
            )

            Powerbuilding ->
                AppTokens.strings.res(
                    Res.string.training_profile_details_powerbuilding,
                    distribution
                )

            Mixed -> AppTokens.strings.res(
                Res.string.training_profile_details_dominance_distribution,
                profile.dominanceLine(),
                distribution,
            )

            Strength -> AppTokens.strings.res(
                Res.string.training_profile_details_dominance_distribution,
                profile.dominanceLine(),
                distribution,
            )

            Hypertrophy -> AppTokens.strings.res(
                Res.string.training_profile_details_dominance_distribution,
                profile.dominanceLine(),
                distribution,
            )

            Endurance -> AppTokens.strings.res(
                Res.string.training_profile_details_dominance_distribution,
                profile.dominanceLine(),
                distribution,
            )
        }
    }

    @Composable
    public fun tip(profile: TrainingLoadProfileState): String? {
        val c = profile.confidence.coerceIn(0, 100)
        return when (this) {
            Easy -> AppTokens.strings.res(Res.string.training_profile_tip_easy)
            Powerbuilding -> AppTokens.strings.res(Res.string.training_profile_tip_powerbuilding)
            Mixed -> AppTokens.strings.res(Res.string.training_profile_tip_mixed)
            Strength -> when {
                c >= 70 -> AppTokens.strings.res(Res.string.training_profile_tip_strength_high)
                c >= 35 -> AppTokens.strings.res(Res.string.training_profile_tip_strength_medium)
                else -> AppTokens.strings.res(Res.string.training_profile_tip_strength_low)
            }

            Hypertrophy -> when {
                c >= 70 -> AppTokens.strings.res(Res.string.training_profile_tip_hypertrophy_high)
                c >= 35 -> AppTokens.strings.res(Res.string.training_profile_tip_hypertrophy_medium)
                else -> AppTokens.strings.res(Res.string.training_profile_tip_hypertrophy_low)
            }

            Endurance -> when {
                c >= 70 -> AppTokens.strings.res(Res.string.training_profile_tip_endurance_high)
                c >= 35 -> AppTokens.strings.res(Res.string.training_profile_tip_endurance_medium)
                else -> AppTokens.strings.res(Res.string.training_profile_tip_endurance_low)
            }
        }
    }
}

@Immutable
public data class TrainingDimensionScoreState(
    val kind: TrainingDimensionKindState,
    val score: Int, // 0..100
)

public fun stubTrainingLoadProfile(): TrainingLoadProfileState {
    return TrainingLoadProfileState(
        kind = TrainingProfileKindState.Powerbuilding,
        dimensions = listOf(
            TrainingDimensionScoreState(
                kind = TrainingDimensionKindState.Strength,
                score = 78
            ),
            TrainingDimensionScoreState(
                kind = TrainingDimensionKindState.Hypertrophy,
                score = 68
            ),
            TrainingDimensionScoreState(
                kind = TrainingDimensionKindState.Endurance,
                score = 32
            )
        ),
        dominant = TrainingDimensionKindState.Strength,
        confidence = 74
    )
}
