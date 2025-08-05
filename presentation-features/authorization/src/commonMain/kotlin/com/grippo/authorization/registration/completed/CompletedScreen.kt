package com.grippo.authorization.registration.completed

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.konfetti.KonfettiParade
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.components.user.UserCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.get_started_btn
import com.grippo.design.resources.icons.Check
import com.grippo.design.resources.registration_completed_title
import com.grippo.state.profile.stubUser
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.delay

@Composable
internal fun CompletedScreen(
    state: CompletedState,
    loaders: ImmutableSet<CompletedLoader>,
    contract: CompletedContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    if (loaders.contains(CompletedLoader.Registration)) {
        Loader(modifier = Modifier.fillMaxSize())
        return@BaseComposeScreen
    }

    val cardVisible = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        cardVisible.value = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (cardVisible.value) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "alpha"
    )

    val offsetY by animateDpAsState(
        targetValue = if (cardVisible.value) 0.dp else 40.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "offsetY"
    )

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        style = ToolbarStyle.Transparent
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxSize()
                .padding(
                    horizontal = AppTokens.dp.screen.horizontalPadding,
                    vertical = AppTokens.dp.contentPadding.content
                ).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = AppTokens.strings.res(Res.string.registration_completed_title),
                style = AppTokens.typography.h2(),
                color = AppTokens.colors.text.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Spacer(modifier = Modifier.weight(0.5f))

            if (state.user != null) {
                UserCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = offsetY)
                        .alpha(alpha),
                    value = state.user
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Button(
                modifier = Modifier.fillMaxWidth(),
                text = AppTokens.strings.res(Res.string.get_started_btn),
                style = ButtonStyle.Primary,
                startIcon = AppTokens.icons.Check,
                onClick = contract::onCompleteClick
            )
        }

        if (state.user != null) {
            KonfettiParade()
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        CompletedScreen(
            state = CompletedState(
                user = stubUser()
            ),
            loaders = persistentSetOf(),
            contract = CompletedContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewLoading() {
    PreviewContainer {
        CompletedScreen(
            state = CompletedState(
                user = stubUser()
            ),
            loaders = persistentSetOf(CompletedLoader.Registration),
            contract = CompletedContract.Empty
        )
    }
}