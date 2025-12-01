package com.grippo.authorization.profile.creation.body

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface BodyDirection : BaseDirection {
    data class Experience(val weight: Float, val height: Int) : BodyDirection
    data object Back : BodyDirection
}