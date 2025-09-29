package com.grippo.state.item

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
public data class ItemState(
    val id: String,
    val text: String,
)