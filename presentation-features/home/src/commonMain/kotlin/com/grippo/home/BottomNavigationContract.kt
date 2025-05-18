package com.grippo.home

internal interface BottomNavigationContract {

    fun selectPage(index: Int) {}

    companion object Empty : BottomNavigationContract
}