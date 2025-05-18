package com.grippo.home.bottom.navigation

internal interface BottomNavigationContract {

    fun selectPage(index: Int) {}

    companion object Empty : BottomNavigationContract
}