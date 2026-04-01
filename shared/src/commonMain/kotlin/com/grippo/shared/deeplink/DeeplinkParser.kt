package com.grippo.shared.deeplink

import com.grippo.core.state.stage.StageState
import com.grippo.shared.root.RootDirection

internal object DeeplinkParser {

    fun parse(raw: String): RootDirection? = when (raw) {
        "training_recording" -> RootDirection.Training(StageState.Add)
        "training_draft" -> RootDirection.Training(StageState.Draft)
        "trainings" -> RootDirection.Trainings
        "profile" -> RootDirection.Profile
        "weight_history" -> RootDirection.WeightHistory
        "settings" -> RootDirection.Settings
        else -> null
    }
}
