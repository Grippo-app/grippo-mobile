package com.grippo.profile.menu.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.menu.SettingsMenu
import com.grippo.core.state.profile.GoalState
import com.grippo.core.state.profile.RoleEnumState
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.profile.stubUser
import com.grippo.design.components.menu.MenuSection
import com.grippo.design.components.menu.MenuSectionStyle
import com.grippo.design.components.menu.MenuValueRow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.cm
import com.grippo.design.resources.provider.goal_picker_primary_title
import com.grippo.design.resources.provider.goal_title
import com.grippo.design.resources.provider.icons.Body
import com.grippo.design.resources.provider.icons.Muscle
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.icons.Tune
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.profile_account
import com.grippo.design.resources.provider.profile_body_section
import com.grippo.design.resources.provider.profile_height
import com.grippo.design.resources.provider.profile_restrictions_section
import com.grippo.design.resources.provider.profile_value_not_set
import com.grippo.design.resources.provider.settings
import com.grippo.design.resources.provider.weight

@Composable
internal fun ProfileMenuContent(
    user: UserState?,
    goal: GoalState?,
    excludedMusclesCount: Int?,
    excludedEquipmentsCount: Int?,
    onProfileMenuClick: (ProfileMenu) -> Unit,
    onSettingsMenuClick: (SettingsMenu) -> Unit,
    modifier: Modifier = Modifier,
    userLoading: Boolean = false,
    goalLoading: Boolean = false,
    musclesLoading: Boolean = false,
    equipmentsLoading: Boolean = false,
) {
    val notSet = AppTokens.strings.res(Res.string.profile_value_not_set)
    val weight = (user?.weight as? WeightFormatState.Valid)?.let {
        "${it.display} ${AppTokens.strings.res(Res.string.kg)}"
    } ?: notSet
    val height = (user?.height as? HeightFormatState.Valid)?.let {
        "${it.display} ${AppTokens.strings.res(Res.string.cm)}"
    } ?: notSet

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        MenuSection(
            title = AppTokens.strings.res(Res.string.profile_body_section),
            icon = AppTokens.icons.Body,
            iconColor = AppTokens.colors.context.body,
        ) {
            MenuValueRow(
                title = AppTokens.strings.res(Res.string.weight),
                value = weight,
                loading = userLoading,
                onClick = { onProfileMenuClick(ProfileMenu.Body) },
            )
            MenuValueRow(
                title = AppTokens.strings.res(Res.string.profile_height),
                value = height,
                loading = userLoading,
                onClick = { onProfileMenuClick(ProfileMenu.Body) },
            )
            MenuValueRow(
                title = ProfileMenu.Experience.text().text(),
                value = user?.experience?.title()?.text() ?: notSet,
                loading = userLoading,
                showDivider = false,
                onClick = { onProfileMenuClick(ProfileMenu.Experience) },
            )
        }
        MenuSection(
            title = AppTokens.strings.res(Res.string.goal_title),
            icon = AppTokens.icons.Trophy,
            iconColor = AppTokens.colors.context.goal,
        ) {
            MenuValueRow(
                title = AppTokens.strings.res(Res.string.goal_picker_primary_title),
                value = goal?.primaryGoal?.label() ?: notSet,
                loading = goalLoading,
                showDivider = false,
                onClick = { onProfileMenuClick(ProfileMenu.Goal) },
            )
        }
        MenuSection(
            title = AppTokens.strings.res(Res.string.profile_restrictions_section),
            icon = AppTokens.icons.Muscle,
            iconColor = AppTokens.colors.context.muscle,
        ) {
            MenuValueRow(
                title = ProfileMenu.Muscles.text().text(),
                value = excludedMusclesCount?.toString() ?: notSet,
                loading = musclesLoading,
                onClick = { onProfileMenuClick(ProfileMenu.Muscles) },
            )
            MenuValueRow(
                title = ProfileMenu.Equipment.text().text(),
                value = excludedEquipmentsCount?.toString() ?: notSet,
                loading = equipmentsLoading,
                showDivider = false,
                onClick = { onProfileMenuClick(ProfileMenu.Equipment) },
            )
        }
        MenuSection(
            title = AppTokens.strings.res(Res.string.settings),
            style = MenuSectionStyle.Subtle,
            icon = AppTokens.icons.Tune,
            iconColor = AppTokens.colors.icon.tertiary,
        ) {
            val settingsMenu = SettingsMenu.entries
                .filter { it != SettingsMenu.Debug || user?.role == RoleEnumState.ADMIN }
            settingsMenu.forEachIndexed { index, menu ->
                MenuValueRow(
                    title = if (menu == SettingsMenu.Settings) {
                        AppTokens.strings.res(Res.string.profile_account)
                    } else {
                        menu.text().text()
                    },
                    titleColor = if (menu == SettingsMenu.Debug) menu.textColor() else AppTokens.colors.text.secondary,
                    showDivider = index < settingsMenu.lastIndex,
                    onClick = { onSettingsMenuClick(menu) },
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun ProfileMenuContentPreview() {
    PreviewContainer {
        ProfileMenuContent(
            user = stubUser(),
            goal = null,
            excludedMusclesCount = 2,
            excludedEquipmentsCount = 3,
            onProfileMenuClick = {},
            onSettingsMenuClick = {},
        )
    }
}
