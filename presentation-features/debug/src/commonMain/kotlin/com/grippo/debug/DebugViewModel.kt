package com.grippo.debug

import com.grippo.core.BaseViewModel

public class DebugViewModel : BaseViewModel<DebugState, DebugDirection, DebugLoader>(DebugState),
    DebugContract {

    override fun back() {
        navigateTo(DebugDirection.Back)
    }
}