package com.grippo.shared.deeplink

import com.grippo.core.state.stage.StageState
import com.grippo.shared.root.RootDirection

/**
 * Maps a raw deeplink string (carried as an Intent extra on Android or as
 * `userInfo["deeplink"]` on iOS) to a [RootDirection] the root navigator
 * can act on.
 *
 * Deeplink contract
 * -----------------
 * | String                  | Screen                                      |
 * |-------------------------|---------------------------------------------|
 * | `training_recording`    | Training recording — new session            |
 * | `training_draft`        | Training recording — resume draft           |
 * | `trainings`             | Trainings list                              |
 * | `profile`               | Profile                                     |
 * | `weight_history`        | Weight history (inside Profile)             |
 * | `settings`              | Settings (inside Profile)                   |
 *
 * Returns `null` for unknown strings so callers can silently ignore them.
 */
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
