package com.grippo.domain.state.user

import com.grippo.data.features.api.user.models.User
import com.grippo.state.profile.UserState

public fun User.toState(): UserState {
    return UserState(
        id = id,
        name = name,
        weight = weight,
        height = height,
        createdAt = createAt,
        experience = experience.toState(),
        trainingsCount = 0 // TODO: Add from backend
    )
}