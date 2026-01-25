package com.grippo.authorization.profile.creation.user

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface UserDirection : BaseDirection {
    data class Experience(val name: String, val weight: Float, val height: Int) : UserDirection
    data object Back : UserDirection
}