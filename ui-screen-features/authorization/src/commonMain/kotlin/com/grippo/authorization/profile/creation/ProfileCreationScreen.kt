package com.grippo.authorization.profile.creation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.foundation.platform.platformAnimation
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.core.AppTokens
import com.grippo.screen.api.ProfileCreationRouter
import kotlinx.collections.immutable.ImmutableSet
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack as ChildStackCompose

@Composable
internal fun ProfileCreationScreen(
    component: ProfileCreationComponent,
    state: ProfileCreationState,
    loaders: ImmutableSet<ProfileCreationLoader>,
    contract: ProfileCreationContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    val stack by component.childStack.subscribeAsState()
    val stepsTotal = PROFILE_CREATION_FLOW.size.coerceAtLeast(1)
    val stepsCompleted = stack.items.size.coerceIn(0, stepsTotal)
    val progress = stepsCompleted / stepsTotal.toFloat()

    Box {
        ChildStackCompose(
            modifier = Modifier.fillMaxSize(),
            stack = component.childStack,
            animation = platformAnimation(),
            content = { child ->
                child.instance.component.Render()
            }
        )

        if (stepsCompleted < stepsTotal) {
            LineIndicator(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(vertical = AppTokens.dp.contentPadding.content)
                    .height(AppTokens.dp.screen.toolbar.height)
                    .fillMaxWidth()
                    .padding(horizontal = 100.dp)
                    .wrapContentHeight(),
                progress = progress,
            )
        }
    }
}

private val PROFILE_CREATION_FLOW = listOf(
    ProfileCreationRouter.Name,
    ProfileCreationRouter.Body,
    ProfileCreationRouter.Experience,
    ProfileCreationRouter.ExcludedMuscles,
    ProfileCreationRouter.MissingEquipments,
    ProfileCreationRouter.Completed
)
