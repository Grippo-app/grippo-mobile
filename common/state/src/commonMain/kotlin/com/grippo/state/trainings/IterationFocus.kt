package com.grippo.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.iteration_focus_repetitions
import com.grippo.design.resources.provider.iteration_focus_unidentified
import com.grippo.design.resources.provider.iteration_focus_volume
import com.grippo.state.formatters.UiText
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class IterationFocus {
    VOLUME,
    REPETITIONS,
    UNIDENTIFIED;

    public fun title(): UiText {
        val r = when (this) {
            VOLUME -> Res.string.iteration_focus_volume
            REPETITIONS -> Res.string.iteration_focus_repetitions
            UNIDENTIFIED -> Res.string.iteration_focus_unidentified
        }
        return UiText.Res(r)
    }
}