package com.grippo.shared.root

import androidx.compose.runtime.Immutable
import com.grippo.state.settings.ThemeState

@Immutable
public data class RootState(
    val isConnectedToInternet: Boolean = true,
    val theme: ThemeState? = null
)