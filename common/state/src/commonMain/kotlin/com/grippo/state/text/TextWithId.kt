package com.grippo.state.text

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public data class TextWithId(
    val id: String,
    val text: String,
)