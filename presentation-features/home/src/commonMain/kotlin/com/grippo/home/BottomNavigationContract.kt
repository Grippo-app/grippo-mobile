package com.grippo.home

internal interface BottomNavigationContract {

    fun selectTab(origin: Int)
    fun back()

    companion object Empty : BottomNavigationContract {
        override fun selectTab(origin: Int) {}
        override fun back() {}
    }
}