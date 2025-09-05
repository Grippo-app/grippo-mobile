package com.grippo.calculation.internal

import com.grippo.calculation.models.Bucket
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.Instruction
import com.grippo.date.utils.DateFormat
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.tooltip_category_description_training
import com.grippo.design.resources.provider.tooltip_category_description_trainings
import com.grippo.design.resources.provider.tooltip_category_title_training
import com.grippo.design.resources.provider.tooltip_category_title_trainings
import com.grippo.design.resources.provider.tooltip_estimated1rm_description_day
import com.grippo.design.resources.provider.tooltip_estimated1rm_description_month
import com.grippo.design.resources.provider.tooltip_estimated1rm_description_training
import com.grippo.design.resources.provider.tooltip_estimated1rm_description_year
import com.grippo.design.resources.provider.tooltip_estimated1rm_title_day
import com.grippo.design.resources.provider.tooltip_estimated1rm_title_month
import com.grippo.design.resources.provider.tooltip_estimated1rm_title_training
import com.grippo.design.resources.provider.tooltip_estimated1rm_title_year
import com.grippo.design.resources.provider.tooltip_experience_description_training
import com.grippo.design.resources.provider.tooltip_experience_description_trainings
import com.grippo.design.resources.provider.tooltip_experience_title_training
import com.grippo.design.resources.provider.tooltip_experience_title_trainings
import com.grippo.design.resources.provider.tooltip_force_type_description_training
import com.grippo.design.resources.provider.tooltip_force_type_description_trainings
import com.grippo.design.resources.provider.tooltip_force_type_title_training
import com.grippo.design.resources.provider.tooltip_force_type_title_trainings
import com.grippo.design.resources.provider.tooltip_muscle_load_description_day
import com.grippo.design.resources.provider.tooltip_muscle_load_description_month
import com.grippo.design.resources.provider.tooltip_muscle_load_description_training
import com.grippo.design.resources.provider.tooltip_muscle_load_description_year
import com.grippo.design.resources.provider.tooltip_muscle_load_title_day
import com.grippo.design.resources.provider.tooltip_muscle_load_title_month
import com.grippo.design.resources.provider.tooltip_muscle_load_title_training
import com.grippo.design.resources.provider.tooltip_muscle_load_title_year
import com.grippo.design.resources.provider.tooltip_percent1rm_description_day
import com.grippo.design.resources.provider.tooltip_percent1rm_description_month
import com.grippo.design.resources.provider.tooltip_percent1rm_description_training
import com.grippo.design.resources.provider.tooltip_percent1rm_description_year
import com.grippo.design.resources.provider.tooltip_percent1rm_title_day
import com.grippo.design.resources.provider.tooltip_percent1rm_title_month
import com.grippo.design.resources.provider.tooltip_percent1rm_title_training
import com.grippo.design.resources.provider.tooltip_percent1rm_title_year
import com.grippo.design.resources.provider.tooltip_stimulus_description_day
import com.grippo.design.resources.provider.tooltip_stimulus_description_month
import com.grippo.design.resources.provider.tooltip_stimulus_description_training
import com.grippo.design.resources.provider.tooltip_stimulus_description_year
import com.grippo.design.resources.provider.tooltip_stimulus_title_day
import com.grippo.design.resources.provider.tooltip_stimulus_title_month
import com.grippo.design.resources.provider.tooltip_stimulus_title_training
import com.grippo.design.resources.provider.tooltip_stimulus_title_year
import com.grippo.design.resources.provider.tooltip_volume_description_day
import com.grippo.design.resources.provider.tooltip_volume_description_month
import com.grippo.design.resources.provider.tooltip_volume_description_training
import com.grippo.design.resources.provider.tooltip_volume_description_year
import com.grippo.design.resources.provider.tooltip_volume_title_day
import com.grippo.design.resources.provider.tooltip_volume_title_month
import com.grippo.design.resources.provider.tooltip_volume_title_training
import com.grippo.design.resources.provider.tooltip_volume_title_year
import com.grippo.design.resources.provider.tooltip_weight_type_description_training
import com.grippo.design.resources.provider.tooltip_weight_type_description_trainings
import com.grippo.design.resources.provider.tooltip_weight_type_title_training
import com.grippo.design.resources.provider.tooltip_weight_type_title_trainings
import com.grippo.design.resources.provider.w
import com.grippo.state.datetime.PeriodState
import com.grippo.state.formatters.UiText
import kotlinx.datetime.LocalDateTime

/**
 * Utilities for creating labels related to dates for chart axes.
 * Contains suspend functions that depend on StringProvider for localization.
 */

// -------- Bucket labelers --------

internal suspend fun defaultLabeler(
    scale: BucketScale,
    stringProvider: StringProvider
): (Bucket) -> String {
    val w = stringProvider.get(Res.string.w)
    return when (scale) {
        BucketScale.DAY -> { b ->
            DateTimeUtils.format(b.start, DateFormat.WEEKDAY_SHORT)
        }

        BucketScale.WEEK -> { b ->
            "$w${isoWeekNumber(b.start)}-${
                DateTimeUtils.format(b.start, DateFormat.MONTH_SHORT)
            }"
        }

        BucketScale.MONTH -> { b ->
            DateTimeUtils.format(b.start, DateFormat.MONTH_SHORT)
        }

        BucketScale.EXERCISE -> { _ -> "" }
    }
}

internal suspend fun LocalDateTime.label(
    scale: BucketScale,
    stringProvider: StringProvider
): String {
    val w = stringProvider.get(Res.string.w)
    return when (scale) {
        BucketScale.DAY, BucketScale.EXERCISE -> {
            DateTimeUtils.format(this, DateFormat.DATE_DD_MMM) // e.g., "02 Sep"
        }

        BucketScale.WEEK -> {
            "$w${isoWeekNumber(this)}-${
                DateTimeUtils.format(this, DateFormat.MONTH_SHORT)
            }"
        }

        BucketScale.MONTH -> {
            DateTimeUtils.format(this, DateFormat.MONTH_SHORT)
        }
    }
}

// -------- Time labels for heatmaps --------

internal suspend fun defaultTimeLabels(
    buckets: List<Bucket>,
    scale: BucketScale,
    stringProvider: StringProvider
): List<String> {
    val w = stringProvider.get(Res.string.w)
    return when (scale) {
        BucketScale.DAY -> buckets.map {
            DateTimeUtils.format(it.start, DateFormat.DATE_DD_MMM) // e.g., "02 Sep"
        }

        BucketScale.WEEK -> buckets.map {
            "$w${isoWeekNumber(it.start)}-${
                DateTimeUtils.format(it.start, DateFormat.MONTH_SHORT)
            }" // e.g., "W36-Sep"
        }

        BucketScale.MONTH -> buckets.map {
            DateTimeUtils.format(it.start, DateFormat.MONTH_SHORT) // e.g., "Sep"
        }

        BucketScale.EXERCISE -> buckets.map {
            DateTimeUtils.format(it.start, DateFormat.DATE_DD_MMM)
        }
    }
}

// -------- Instruction generators --------

internal fun instructionForVolume(
    scale: BucketScale,
): Instruction {
    return when (scale) {
        BucketScale.EXERCISE ->
            Instruction(
                UiText.Res(Res.string.tooltip_volume_title_training),
                UiText.Res(Res.string.tooltip_volume_description_training)
            )

        BucketScale.DAY ->
            Instruction(
                UiText.Res(Res.string.tooltip_volume_title_day),
                UiText.Res(Res.string.tooltip_volume_description_day)
            )

        BucketScale.WEEK ->
            Instruction(
                UiText.Res(Res.string.tooltip_volume_title_month),
                UiText.Res(Res.string.tooltip_volume_description_month)
            )

        BucketScale.MONTH ->
            Instruction(
                UiText.Res(Res.string.tooltip_volume_title_year),
                UiText.Res(Res.string.tooltip_volume_description_year)
            )
    }
}

internal fun instructionForPercent1RM(
    scale: BucketScale,
): Instruction {
    return when (scale) {
        BucketScale.EXERCISE ->
            Instruction(
                UiText.Res(Res.string.tooltip_percent1rm_title_training),
                UiText.Res(Res.string.tooltip_percent1rm_description_training)
            )

        BucketScale.DAY ->
            Instruction(
                UiText.Res(Res.string.tooltip_percent1rm_title_day),
                UiText.Res(Res.string.tooltip_percent1rm_description_day)
            )

        BucketScale.WEEK ->
            Instruction(
                UiText.Res(Res.string.tooltip_percent1rm_title_month),
                UiText.Res(Res.string.tooltip_percent1rm_description_month)
            )

        BucketScale.MONTH ->
            Instruction(
                UiText.Res(Res.string.tooltip_percent1rm_title_year),
                UiText.Res(Res.string.tooltip_percent1rm_description_year)
            )
    }
}

internal fun instructionForStimulus(
    scale: BucketScale,
): Instruction {
    return when (scale) {
        BucketScale.EXERCISE ->
            Instruction(
                UiText.Res(Res.string.tooltip_stimulus_title_training),
                UiText.Res(Res.string.tooltip_stimulus_description_training)
            )

        BucketScale.DAY ->
            Instruction(
                UiText.Res(Res.string.tooltip_stimulus_title_day),
                UiText.Res(Res.string.tooltip_stimulus_description_day)
            )

        BucketScale.WEEK ->
            Instruction(
                UiText.Res(Res.string.tooltip_stimulus_title_month),
                UiText.Res(Res.string.tooltip_stimulus_description_month)
            )

        BucketScale.MONTH ->
            Instruction(
                UiText.Res(Res.string.tooltip_stimulus_title_year),
                UiText.Res(Res.string.tooltip_stimulus_description_year)
            )
    }
}

internal fun instructionForEstimated1RM(
    scale: BucketScale,
): Instruction {
    return when (scale) {
        BucketScale.EXERCISE ->
            Instruction(
                UiText.Res(Res.string.tooltip_estimated1rm_title_training),
                UiText.Res(Res.string.tooltip_estimated1rm_description_training)
            )

        BucketScale.DAY ->
            Instruction(
                UiText.Res(Res.string.tooltip_estimated1rm_title_day),
                UiText.Res(Res.string.tooltip_estimated1rm_description_day)
            )

        BucketScale.WEEK ->
            Instruction(
                UiText.Res(Res.string.tooltip_estimated1rm_title_month),
                UiText.Res(Res.string.tooltip_estimated1rm_description_month)
            )

        BucketScale.MONTH ->
            Instruction(
                UiText.Res(Res.string.tooltip_estimated1rm_title_year),
                UiText.Res(Res.string.tooltip_estimated1rm_description_year)
            )
    }
}

internal fun instructionForMuscleLoad(period: PeriodState): Instruction {
    val scale = deriveScale(period)
    val (titleRes, descRes) = when (scale) {
        BucketScale.EXERCISE ->
            Res.string.tooltip_muscle_load_title_training to
                    Res.string.tooltip_muscle_load_description_training

        BucketScale.DAY ->
            Res.string.tooltip_muscle_load_title_day to
                    Res.string.tooltip_muscle_load_description_day
        // No dedicated "week" strings; reuse month-view copy for weekly aggregation
        BucketScale.WEEK ->
            Res.string.tooltip_muscle_load_title_month to
                    Res.string.tooltip_muscle_load_description_month

        BucketScale.MONTH -> {
            val days = daysInclusive(period.range.from.date, period.range.to.date)
            if (days >= 365)
                Res.string.tooltip_muscle_load_title_year to
                        Res.string.tooltip_muscle_load_description_year
            else
                Res.string.tooltip_muscle_load_title_month to
                        Res.string.tooltip_muscle_load_description_month
        }
    }
    return Instruction(title = UiText.Res(titleRes), description = UiText.Res(descRes))
}

// -------- Distribution instructions --------

internal enum class DistributionMetric { CATEGORY, WEIGHT_TYPE, FORCE_TYPE, EXPERIENCE }

internal fun instructionForDistribution(
    metric: DistributionMetric,
    scale: BucketScale,
): Instruction {
    // For distributions we only distinguish: single session vs aggregated period
    val isTraining = (scale == BucketScale.EXERCISE)

    return when (metric) {
        DistributionMetric.CATEGORY ->
            if (isTraining) {
                Instruction(
                    title = UiText.Res(Res.string.tooltip_category_title_training),
                    description = UiText.Res(Res.string.tooltip_category_description_training)
                )
            } else {
                Instruction(
                    title = UiText.Res(Res.string.tooltip_category_title_trainings),
                    description = UiText.Res(Res.string.tooltip_category_description_trainings)
                )
            }

        DistributionMetric.WEIGHT_TYPE ->
            if (isTraining) {
                Instruction(
                    title = UiText.Res(Res.string.tooltip_weight_type_title_training),
                    description = UiText.Res(Res.string.tooltip_weight_type_description_training)
                )
            } else {
                Instruction(
                    title = UiText.Res(Res.string.tooltip_weight_type_title_trainings),
                    description = UiText.Res(Res.string.tooltip_weight_type_description_trainings)
                )
            }

        DistributionMetric.FORCE_TYPE ->
            if (isTraining) {
                Instruction(
                    title = UiText.Res(Res.string.tooltip_force_type_title_training),
                    description = UiText.Res(Res.string.tooltip_force_type_description_training)
                )
            } else {
                Instruction(
                    title = UiText.Res(Res.string.tooltip_force_type_title_trainings),
                    description = UiText.Res(Res.string.tooltip_force_type_description_trainings)
                )
            }

        DistributionMetric.EXPERIENCE ->
            if (isTraining) {
                Instruction(
                    title = UiText.Res(Res.string.tooltip_experience_title_training),
                    description = UiText.Res(Res.string.tooltip_experience_description_training)
                )
            } else {
                Instruction(
                    title = UiText.Res(Res.string.tooltip_experience_title_trainings),
                    description = UiText.Res(Res.string.tooltip_experience_description_trainings)
                )
            }
    }
}
