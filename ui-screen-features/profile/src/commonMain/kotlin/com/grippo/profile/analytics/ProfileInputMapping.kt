package com.grippo.profile.analytics

import com.grippo.analytics.common.models.InputStatus
import com.grippo.analytics.profile.models.ExperienceLevel
import com.grippo.analytics.profile.models.ProfileMenuTarget
import com.grippo.analytics.profile.models.SocialChannel
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.menu.SettingsMenu
import com.grippo.core.state.menu.SocialMenu
import com.grippo.core.state.profile.ExperienceEnumState

internal object ProfileInputMapping {
    fun status(value: HeightFormatState): InputStatus = when (value) {
        is HeightFormatState.Empty -> InputStatus.Empty
        is HeightFormatState.Invalid -> InputStatus.Invalid
        is HeightFormatState.Valid -> InputStatus.Valid
    }

    fun status(value: WeightFormatState): InputStatus = when (value) {
        is WeightFormatState.Empty -> InputStatus.Empty
        is WeightFormatState.Invalid -> InputStatus.Invalid
        is WeightFormatState.Valid -> InputStatus.Valid
    }

    fun status(value: DateTimeFormatState): InputStatus = when (value) {
        is DateTimeFormatState.Empty -> InputStatus.Empty
        is DateTimeFormatState.Invalid -> InputStatus.Invalid
        is DateTimeFormatState.Valid -> InputStatus.Valid
    }

    fun experience(value: ExperienceEnumState): ExperienceLevel = when (value) {
        ExperienceEnumState.BEGINNER -> ExperienceLevel.Beginner
        ExperienceEnumState.INTERMEDIATE -> ExperienceLevel.Intermediate
        ExperienceEnumState.ADVANCED -> ExperienceLevel.Advanced
        ExperienceEnumState.PRO -> ExperienceLevel.Pro
    }

    fun target(value: ProfileMenu): ProfileMenuTarget = when (value) {
        ProfileMenu.Body -> ProfileMenuTarget.Body
        ProfileMenu.Goal -> ProfileMenuTarget.Goal
        ProfileMenu.Experience -> ProfileMenuTarget.Experience
        ProfileMenu.Muscles -> ProfileMenuTarget.Muscles
        ProfileMenu.Equipment -> ProfileMenuTarget.Equipments
    }

    fun target(value: SettingsMenu): ProfileMenuTarget = when (value) {
        SettingsMenu.Settings -> ProfileMenuTarget.Settings
        SettingsMenu.Social -> ProfileMenuTarget.Social
        SettingsMenu.Debug -> ProfileMenuTarget.Debug
    }

    fun channel(value: SocialMenu): SocialChannel = when (value) {
        SocialMenu.Instagram -> SocialChannel.Instagram
        SocialMenu.TikTok -> SocialChannel.TikTok
        SocialMenu.YouTube -> SocialChannel.YouTube
        SocialMenu.Threads -> SocialChannel.Threads
    }
}
