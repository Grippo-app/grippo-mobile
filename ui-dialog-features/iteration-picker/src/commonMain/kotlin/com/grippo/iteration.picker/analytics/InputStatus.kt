package com.grippo.iteration.picker.analytics

import com.grippo.analytics.common.models.InputStatus
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.WeightFormatState

internal fun VolumeFormatState.inputStatus(): InputStatus = when (this) {
    is VolumeFormatState.Valid -> InputStatus.Valid
    is VolumeFormatState.Invalid -> InputStatus.Invalid
    is VolumeFormatState.Empty -> InputStatus.Empty
}

internal fun RepetitionsFormatState.inputStatus(): InputStatus = when (this) {
    is RepetitionsFormatState.Valid -> InputStatus.Valid
    is RepetitionsFormatState.Invalid -> InputStatus.Invalid
    is RepetitionsFormatState.Empty -> InputStatus.Empty
}

internal fun WeightFormatState.inputStatus(): InputStatus = when (this) {
    is WeightFormatState.Valid -> InputStatus.Valid
    is WeightFormatState.Invalid -> InputStatus.Invalid
    is WeightFormatState.Empty -> InputStatus.Empty
}
