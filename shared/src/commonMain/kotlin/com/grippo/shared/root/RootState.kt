package com.grippo.shared.root

import androidx.compose.runtime.Immutable
import com.grippo.design.components.connection.snackbar.ConnectionSnackbarState
import com.grippo.state.settings.LocaleState
import com.grippo.state.settings.ThemeState

@Immutable
public data class RootState(
    val connection: ConnectionSnackbarState = ConnectionSnackbarState.Hidden,
    val theme: ThemeState? = null,
    val locale: LocaleState? = null
)