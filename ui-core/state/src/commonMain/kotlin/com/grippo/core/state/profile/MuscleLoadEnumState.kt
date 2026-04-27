package com.grippo.core.state.profile

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscle_load_high
import com.grippo.design.resources.provider.muscle_load_low
import com.grippo.design.resources.provider.muscle_load_medium

@Immutable
public enum class MuscleLoadEnumState {
    HIGH,
    MEDIUM,
    LOW;

    public fun title(): UiText = TITLES.getValue(this)

    public companion object {
        private val TITLES: Map<MuscleLoadEnumState, UiText> = entries.associateWith {
            UiText.Res(
                when (it) {
                    HIGH -> Res.string.muscle_load_high
                    MEDIUM -> Res.string.muscle_load_medium
                    LOW -> Res.string.muscle_load_low
                }
            )
        }
    }
}