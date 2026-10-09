package com.grippo.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.grippo.screen.api.MainRouter

internal class MainTabNavigation<CHILD : Any>(
    componentContext: ComponentContext,
    childFactory: (MainRouter, ComponentContext) -> CHILD,
) {
    private val navigation = StackNavigation<MainRouter>()

    val childStack = componentContext.childStack(
        source = navigation,
        serializer = MainRouter.serializer(),
        initialConfiguration = MainRouter.Home,
        handleBackButton = false,
        key = "MainTabs",
        childFactory = childFactory,
    )

    fun select(tab: MainRouter) {
        navigation.bringToFront(tab)
    }

    /** Returns false when Home is already selected and the host should handle Back. */
    fun back(): Boolean {
        if (childStack.value.active.configuration == MainRouter.Home) return false
        select(MainRouter.Home)
        return true
    }
}
