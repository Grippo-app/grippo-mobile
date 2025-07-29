package com.grippo.domain.mapper.user

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
        records = 0, // todo add from BE
        workouts = 0 // todo add from BE
    )
}