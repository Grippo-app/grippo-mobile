package com.grippo.domain.state.user

import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.profile.UserState
import com.grippo.data.features.api.user.models.User

public fun User.toState(): UserState {
    return UserState(
        id = id,
        name = name,
        email = email,
        weight = WeightFormatState.of(weight),
        height = HeightFormatState.of(height),
        createdAt = createAt,
        experience = experience.toState(),
        role = role.toState()
    )
}