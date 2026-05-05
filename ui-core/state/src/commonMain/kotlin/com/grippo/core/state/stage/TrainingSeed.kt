package com.grippo.core.state.stage

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class TrainingSeed {
    @Serializable
    public data object Blank : TrainingSeed()

    @Serializable
    public data object FromPreset : TrainingSeed()
}
