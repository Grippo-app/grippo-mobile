package com.grippo.duration.picker

import com.grippo.core.foundation.models.BaseDirection
import kotlin.time.Duration

public sealed interface DurationPickerDirection : BaseDirection {
    public data class BackWithResult(val value: Duration) : DurationPickerDirection
    public data object Back : DurationPickerDirection
}
