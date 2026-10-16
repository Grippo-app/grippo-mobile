package com.grippo.profile.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.RoleEnumState
import com.grippo.core.state.profile.stubGoal
import com.grippo.core.state.profile.stubUser
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.components.user.UserProfileHeader
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.profile
import com.grippo.profile.menu.components.ProfileMenuContent
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileMenuScreen(
    state: ProfileMenuState,
    loaders: ImmutableSet<ProfileMenuLoader>,
    contract: ProfileMenuContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    val userLoading = state.user == null && ProfileMenuLoader.User in loaders
    val goalLoading = state.goal == null && ProfileMenuLoader.Goal in loaders
    val musclesLoading = state.excludedMusclesCount == null && ProfileMenuLoader.Muscles in loaders
    val equipmentsLoading = state.excludedEquipmentsCount == null && ProfileMenuLoader.Equipments in loaders

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.profile),
        style = ToolbarStyle.Transparent,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block),
        contentPadding = PaddingValues(
            start = AppTokens.dp.screen.horizontalPadding,
            end = AppTokens.dp.screen.horizontalPadding,
            top = AppTokens.dp.contentPadding.content,
            bottom = AppTokens.dp.screen.verticalPadding,
        ),
    ) {
        item(key = "user", contentType = "user") {
            UserProfileHeader(
                value = state.user,
                loading = userLoading,
            )
        }
        item(key = "menu", contentType = "menu") {
            ProfileMenuContent(
                user = state.user,
                goal = state.goal,
                excludedMusclesCount = state.excludedMusclesCount,
                excludedEquipmentsCount = state.excludedEquipmentsCount,
                userLoading = userLoading,
                goalLoading = goalLoading,
                musclesLoading = musclesLoading,
                equipmentsLoading = equipmentsLoading,
                onProfileMenuClick = contract::onProfileMenuClick,
                onSettingsMenuClick = contract::onSettingsMenuClick,
            )
        }
    }
}

private fun profileMenuPreviewState(): ProfileMenuState = ProfileMenuState(
    user = stubUser().copy(
        name = "Alex",
        email = "alex@grippo.app",
        height = HeightFormatState.of(178),
        weight = WeightFormatState.of(82f),
        experience = ExperienceEnumState.INTERMEDIATE,
        role = RoleEnumState.DEFAULT,
    ),
    goal = stubGoal().copy(primaryGoal = GoalPrimaryGoalEnumState.GET_STRONGER),
    excludedMusclesCount = 2,
    excludedEquipmentsCount = 3,
)

@AppPreview
@Composable
private fun ProfileMenuScreenPreview() {
    PreviewContainer {
        ProfileMenuScreen(
            state = profileMenuPreviewState(),
            loaders = persistentSetOf(),
            contract = ProfileMenuContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileMenuScreenLoadingPreview() {
    PreviewContainer {
        ProfileMenuScreen(
            state = ProfileMenuState(),
            loaders = persistentSetOf(
                ProfileMenuLoader.User,
                ProfileMenuLoader.Goal,
                ProfileMenuLoader.Muscles,
                ProfileMenuLoader.Equipments,
            ),
            contract = ProfileMenuContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileMenuScreenEmptyPreview() {
    PreviewContainer {
        ProfileMenuScreen(
            state = ProfileMenuState(),
            loaders = persistentSetOf(),
            contract = ProfileMenuContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileMenuScreenPartialPreview() {
    PreviewContainer {
        ProfileMenuScreen(
            state = profileMenuPreviewState().copy(goal = null),
            loaders = persistentSetOf(ProfileMenuLoader.Goal),
            contract = ProfileMenuContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileMenuScreenRefreshingPreview() {
    PreviewContainer {
        ProfileMenuScreen(
            state = profileMenuPreviewState(),
            loaders = persistentSetOf(
                ProfileMenuLoader.User,
                ProfileMenuLoader.Goal,
                ProfileMenuLoader.Muscles,
                ProfileMenuLoader.Equipments,
            ),
            contract = ProfileMenuContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileMenuScreenLongIdentityPreview() {
    PreviewContainer {
        val state = profileMenuPreviewState()

        ProfileMenuScreen(
            state = state.copy(
                user = state.user?.copy(
                    name = "Олександр Костянтинович",
                    email = "oleksandr.kostiantynovych@grippo.app",
                ),
            ),
            loaders = persistentSetOf(),
            contract = ProfileMenuContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileMenuScreenAdminPreview() {
    PreviewContainer {
        val state = profileMenuPreviewState()

        ProfileMenuScreen(
            state = state.copy(
                user = state.user?.copy(role = RoleEnumState.ADMIN),
                excludedMusclesCount = 0,
                excludedEquipmentsCount = 0,
            ),
            loaders = persistentSetOf(),
            contract = ProfileMenuContract.Empty,
        )
    }
}
