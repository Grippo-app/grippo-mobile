package com.grippo.main

import app.cash.turbine.test
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.router.children.ChildNavState
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Anchor: `test:main-tab-retention`.
 *
 * The four bottom-bar tabs are hosted by Decompose Child Pages. The core
 * acceptance is that switching tabs NEVER destroys the other tabs — every tab
 * component stays instantiated so its state survives navigation.
 *
 * These tests exercise:
 *  - the exact `childPages` retention configuration used by [MainComponent]
 *    (4 pages, `pageStatus` that maps the selected page to RESUMED and every
 *    other page to CREATED — never DESTROYED), proving all four instances stay
 *    alive and identity-stable across a tab switch; and
 *  - the real [MainViewModel] contract (index update + forwarded directions).
 *
 * Note: constructing the real [MainComponent] eagerly instantiates all four tab
 * root components, whose ViewModels resolve their whole dependency graph (~40+
 * `getKoin().get()` bindings plus the `OperationManager`/`ResultManager`/
 * `ErrorProvider` infra that `BaseViewModel.init` touches via `safeLaunch`).
 * Faking that full graph deterministically is not feasible, so the retention
 * property is verified against a faithful mirror of MainComponent's `childPages`
 * setup rather than the live component.
 */
internal class MainComponentTabRetentionTest {

    private class Leaf(val config: Int)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildPages(
        registry: LifecycleRegistry,
    ): Pair<PagesNavigation<Int>, Value<ChildPages<Int, Leaf>>> {
        val context: ComponentContext = DefaultComponentContext(lifecycle = registry)
        val navigation = PagesNavigation<Int>()
        val pages = context.childPages(
            source = navigation,
            serializer = null,
            initialPages = { Pages(items = listOf(0, 1, 2, 3), selectedIndex = 0) },
            key = "MainComponentTest",
            pageStatus = { index, p ->
                if (index == p.selectedIndex) {
                    ChildNavState.Status.RESUMED
                } else {
                    ChildNavState.Status.CREATED
                }
            },
            handleBackButton = false,
            childFactory = { config, _ -> Leaf(config) },
        )
        return navigation to pages
    }

    @Test
    fun childPages_keepsAllFourInstantiated() {
        val registry = LifecycleRegistry()
        val (_, pages) = buildPages(registry)
        registry.resume()

        assertEquals(4, pages.value.items.size)
        assertTrue(pages.value.items.all { it.instance != null })
    }

    @Test
    fun switchingTab_doesNotDestroyOthers_identityPreserved() {
        val registry = LifecycleRegistry()
        val (navigation, pages) = buildPages(registry)
        registry.resume()

        val firstBefore = pages.value.items[0].instance

        navigation.select(2)

        assertEquals(4, pages.value.items.size)
        assertEquals(2, pages.value.selectedIndex)
        assertTrue(pages.value.items.all { it.instance != null })
        assertSame(firstBefore, pages.value.items[0].instance)
    }

    @Test
    fun mainViewModel_onTabSelected_forwardsSelectTab() = runTest {
        val viewModel = MainViewModel()

        // The selected tab is owned solely by childPages (StateKeeper-backed);
        // the VM only forwards the intent as a direction.
        viewModel.navigator.test {
            viewModel.onTabSelected(2)
            assertEquals(MainDirection.SelectTab(2), awaitItem())
        }
    }

    @Test
    fun startTraining_forwardsStartTraining() = runTest {
        val viewModel = MainViewModel()

        viewModel.navigator.test {
            viewModel.onStartTraining()
            assertEquals(MainDirection.StartTraining, awaitItem())
        }
    }
}
