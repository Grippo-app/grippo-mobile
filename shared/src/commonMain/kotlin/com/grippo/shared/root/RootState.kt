package com.grippo.shared.root

import androidx.compose.runtime.Immutable

@Immutable
public data class RootState(
    val deeplink: String? = null,
)