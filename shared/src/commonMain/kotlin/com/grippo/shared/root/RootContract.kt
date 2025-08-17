package com.grippo.shared.root

public interface RootContract {
    public fun onBack()

    public companion object Empty : RootContract {
        override fun onBack() {}
    }
}