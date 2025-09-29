package com.grippo.state.menu

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
public data class MenuItemState(
    val id: String,
    val text: String,
)