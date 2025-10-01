package com.grippo.design.components.connection.snackbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.WifiOff
import com.grippo.design.resources.provider.no_internet_connection

@Immutable
public sealed interface ConnectionSnackbarState {
    @Immutable
    public object Visible : ConnectionSnackbarState

    @Immutable
    public object Hidden : ConnectionSnackbarState
}

@Composable
public fun ConnectionSnackbar(
    modifier: Modifier = Modifier,
    state: ConnectionSnackbarState,
) {
    val visibilityState = remember {
        MutableTransitionState(false)
    }.apply {
        targetState = state is ConnectionSnackbarState.Visible
    }

    AnimatedVisibility(
        modifier = modifier,
        visibleState = visibilityState,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = 300)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTokens.colors.semantic.error)
                .statusBarsPadding()
                .padding(
                    horizontal = AppTokens.dp.connectionSnackbar.horizontalPadding,
                    vertical = AppTokens.dp.connectionSnackbar.verticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.connectionSnackbar.icon),
                imageVector = AppTokens.icons.WifiOff,
                contentDescription = null,
                tint = AppTokens.colors.icon.inverted,
            )

            Spacer(modifier = Modifier.width(AppTokens.dp.connectionSnackbar.padding))

            Text(
                text = AppTokens.strings.res(Res.string.no_internet_connection),
                style = AppTokens.typography.b12Semi(),
                color = AppTokens.colors.text.inverted
            )
        }
    }
}

@Composable
@AppPreview
private fun ConnectionSnackbarPreview() {
    PreviewContainer {
        ConnectionSnackbar(
            state = ConnectionSnackbarState.Visible,
        )
    }
}
