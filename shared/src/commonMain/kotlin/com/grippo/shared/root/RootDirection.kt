package com.grippo.shared.root

import com.grippo.core.models.BaseDirection

public sealed interface RootDirection : BaseDirection {
    public data object Login : RootDirection
    public data object Back : RootDirection
}