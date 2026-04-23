package com.grippo.domain.state.user

import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.profile.UserState
import com.grippo.data.features.api.user.models.User
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets

public fun User.toState(): UserState {
    return UserState(
        id = id,
        name = name,
        email = email,
        weight = WeightFormatState.of(weight),
        height = HeightFormatState.of(height),
        createdAt = DateTimeFormatState.of(
            value = createdAt,
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateMmmDdYyyy
        ),
        experience = experience.toState(),
        role = role.toState(),
        stats = stats.toState()
    )
}
