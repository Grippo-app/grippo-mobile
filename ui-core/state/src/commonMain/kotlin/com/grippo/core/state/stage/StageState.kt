package com.grippo.core.state.stage

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class StageState {
    public abstract val id: String?

    @Serializable
    public data object Add : StageState() {
        override val id: String? = null
    }

    @Serializable
    public data class Edit(override val id: String) : StageState()

    @Serializable
    public data object Draft : StageState() {
        override val id: String? = null
    }
}
