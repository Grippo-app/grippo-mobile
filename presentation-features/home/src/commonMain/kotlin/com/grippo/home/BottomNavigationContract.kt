package com.grippo.home

internal interface BottomNavigationContract {

    fun selectPage(index: Int)
    fun back()

    companion object Empty : BottomNavigationContract {
        override fun selectPage(index: Int) {}
        override fun back() {}
    }
}