package com.grippo.profile.muscles

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.core.AppTokens
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun ProfileMusclesScreen(
    state: ProfileMusclesState,
    loaders: ImmutableSet<ProfileMusclesLoader>,
    contract: ProfileMusclesContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

}
