package com.grippo.core.state.stage

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class StageState(public open val id: String?) {
    public data object Add : StageState(null)
    public data class Edit(override val id: String) : StageState(id)
    public data object Draft : StageState(null)
}