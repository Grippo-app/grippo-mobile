package com.grippo.authorization.registration.name

import com.grippo.core.models.BaseDirection

internal sealed interface NameDirection : BaseDirection {
    data class Body(val name: String) : NameDirection
}