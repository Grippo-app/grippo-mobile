package com.grippo.domain.state.user

import com.grippo.core.state.profile.UserState
import com.grippo.data.features.api.user.models.User

public fun User.toState(): UserState {
    return UserState(
        id = id,
        name = name,
        weight = weight,
        height = height,
        createdAt = createAt,
        experience = experience.toState(),
    )
}