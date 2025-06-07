package com.grippo.shared.root

public interface RootContract {
    public fun back()

    public companion object Empty : RootContract {
        override fun back() {}
    }
}