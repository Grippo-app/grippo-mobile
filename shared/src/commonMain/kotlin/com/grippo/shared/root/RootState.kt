package com.grippo.shared.root

import androidx.compose.runtime.Immutable
import com.grippo.design.components.connection.snackbar.ConnectionSnackbarState

@Immutable
public data class RootState(
    val connection: ConnectionSnackbarState = ConnectionSnackbarState.Hidden,
)