package com.grippo.home.profile

import androidx.compose.runtime.Composable
import com.grippo.core.BaseComposeScreen
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun ProfileScreen(
    state: ProfileState,
    loaders: ImmutableSet<ProfileLoader>,
    contract: ProfileContract
) = BaseComposeScreen {
}