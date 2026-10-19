package com.grippo.core.state.metrics.engagement

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import kotlin.time.Duration.Companion.hours

@Immutable
public sealed interface DigestState {

    @Immutable
    public data class Content(
        val trainingsCount: Int,
        val duration: DurationFormatState,
        val total: VolumeFormatState,
        val totalSets: Int,
    ) : DigestState

    @Immutable
    public data object Empty : DigestState
}

public fun stubDigest(): DigestState.Content {
    return DigestState.Content(
        trainingsCount = 5,
        duration = DurationFormatState.ofTotal(7.hours),
        total = VolumeFormatState.ofTotal(540F),
        totalSets = 32,
    )
}
