package com.grippo.debug

import com.grippo.core.foundation.BaseViewModel

public class DebugViewModel : BaseViewModel<DebugState, DebugDirection, DebugLoader>(DebugState()),
    DebugContract {

    override fun onBack() {
        navigateTo(DebugDirection.Back)
    }

    override fun onSelect(value: DebugMenu) {
        update { it.copy(selected = value) }
    }
}