package com.grippo.authorization.registration.body

import com.grippo.core.models.BaseDirection

internal sealed interface BodyDirection : BaseDirection {
    data class Experience(val weight: Float, val height: Int) : BodyDirection
    data object Back : BodyDirection
}