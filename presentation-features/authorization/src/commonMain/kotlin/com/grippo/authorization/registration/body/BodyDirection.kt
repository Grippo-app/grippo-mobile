package com.grippo.authorization.registration.body

import com.grippo.core.models.BaseDirection

internal sealed interface BodyDirection : BaseDirection {
    data object Experience : BodyDirection
}