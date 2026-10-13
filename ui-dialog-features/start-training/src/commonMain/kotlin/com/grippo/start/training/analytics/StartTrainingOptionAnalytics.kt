package com.grippo.start.training.analytics

import com.grippo.analytics.start.training.models.StartTrainingOptionKind
import com.grippo.start.training.StartTrainingOption

internal fun StartTrainingOption.toAnalytics(): StartTrainingOptionKind = when (this) {
    StartTrainingOption.Empty -> StartTrainingOptionKind.Empty
    is StartTrainingOption.Preset -> StartTrainingOptionKind.Preset
    is StartTrainingOption.Recent -> StartTrainingOptionKind.Recent
}
