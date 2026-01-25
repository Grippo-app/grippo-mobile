package com.grippo.domain.state.user

import com.grippo.core.state.profile.RoleEnumState
import com.grippo.data.features.api.user.models.RoleEnum

public fun RoleEnum.toState(): RoleEnumState {
    return when (this) {
        RoleEnum.DEFAULT -> RoleEnumState.DEFAULT
        RoleEnum.ADMIN -> RoleEnumState.ADMIN
    }
}