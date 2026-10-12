package com.grippo.authorization.analytics

import com.grippo.analytics.common.models.InputStatus
import com.grippo.analytics.authorization.models.ProfileExperience
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.NameFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.profile.ExperienceEnumState

internal object AuthInputMapping {
    fun status(value: EmailFormatState): InputStatus = when (value) {
        is EmailFormatState.Empty -> InputStatus.Empty
        is EmailFormatState.Invalid -> InputStatus.Invalid
        is EmailFormatState.Valid -> InputStatus.Valid
    }

    fun status(value: PasswordFormatState): InputStatus = when (value) {
        is PasswordFormatState.Empty -> InputStatus.Empty
        is PasswordFormatState.Invalid -> InputStatus.Invalid
        is PasswordFormatState.Valid -> InputStatus.Valid
    }

    fun status(value: NameFormatState): InputStatus = when (value) {
        is NameFormatState.Empty -> InputStatus.Empty
        is NameFormatState.Invalid -> InputStatus.Invalid
        is NameFormatState.Valid -> InputStatus.Valid
    }

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

    fun experience(value: ExperienceEnumState): ProfileExperience = when (value) {
        ExperienceEnumState.BEGINNER -> ProfileExperience.Beginner
        ExperienceEnumState.INTERMEDIATE -> ProfileExperience.Intermediate
        ExperienceEnumState.ADVANCED -> ProfileExperience.Advanced
        ExperienceEnumState.PRO -> ProfileExperience.Pro
    }
}
