package com.grippo.main

import com.grippo.core.foundation.models.BaseDirection

public sealed interface MainDirection : BaseDirection {
    public data class SelectTab(val index: Int) : MainDirection
    public data object StartTraining : MainDirection
}
