package com.grippo.core.foundation.analytics

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.internal.analytics.ScreenAnalyticsImpl
import com.grippo.core.foundation.internal.result.ResultEmitter
import com.grippo.core.foundation.internal.result.ResultManager
import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.foundation.models.BaseLoader
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewModelScreenAnalyticsTest {

    @Test
    fun aViewModelWithoutAScreenDeclarationDoesNotResolveTheBinder() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val app = koinApplication {}
        val viewModel = GraphViewModel(app.koin)
        val lifecycle = LifecycleRegistry()
        try {
            viewModel.bindScreen(lifecycle)
            lifecycle.resume()
            lifecycle.destroy()
        } finally {
            viewModel.onDestroy()
            app.close()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun aRetainedViewModelBindsEachNewComponentLifecycle() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val app = application()
        val recorder = Recording()
        val viewModel = ScreenViewModel(app.koin, recorder)
        val first = LifecycleRegistry()
        val second = LifecycleRegistry()
        try {
            ScreenComponent(first, viewModel, app.koin)
            first.resume()
            first.pause()
            first.resume()
            first.destroy()

            ScreenComponent(second, viewModel, app.koin)
            second.resume()
            second.destroy()

            assertEquals(
                listOf(NavigationReason.Initial, NavigationReason.Restore, NavigationReason.Initial),
                recorder.reasons,
            )
            assertEquals(listOf(1, 1, 1), recorder.closes)
        } finally {
            first.destroy()
            second.destroy()
            viewModel.onDestroy()
            app.close()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun navigationQueuedInInitCannotDestroyTheComponentBeforeScreenBinding() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val app = application()
        val recorder = Recording()
        val viewModel = ScreenViewModel(app.koin, recorder, navigateImmediately = true)
        val lifecycle = LifecycleRegistry()
        var bindings = 0
        app.koin.loadModules(listOf(module {
            single<ScreenAnalytics> {
                object : ScreenAnalytics {
                    override fun bind(
                        lifecycle: Lifecycle,
                        present: (NavigationReason) -> ScreenPresentation,
                    ) {
                        bindings++
                        ScreenAnalyticsImpl().bind(lifecycle, present)
                    }
                }
            }
        }))
        try {
            ScreenComponent(lifecycle, viewModel, app.koin) {
                assertEquals(1, bindings)
                lifecycle.destroy()
            }
            lifecycle.create()
            assertEquals(1, bindings)
            assertEquals(Lifecycle.State.DESTROYED, lifecycle.state)
            assertEquals(emptyList(), recorder.reasons)
        } finally {
            lifecycle.destroy()
            viewModel.onDestroy()
            app.close()
            Dispatchers.resetMain()
        }
    }

    private fun application(): KoinApplication = koinApplication {
        modules(module {
            single<ScreenAnalytics> { ScreenAnalyticsImpl() }
            single { ResultEmitter() }
            factory { parameters -> ResultManager(get(), parameters.get()) }
        })
    }

    private class Recording {
        val reasons = mutableListOf<NavigationReason>()
        val closes = mutableListOf<Int>()

        fun present(reason: NavigationReason): ScreenPresentation {
            reasons += reason
            val index = closes.size
            closes += 0
            return object : ScreenPresentation {
                override fun close() {
                    closes[index]++
                }
            }
        }
    }

    private class GraphViewModel(private val koin: Koin) :
        BaseViewModel<Unit, Direction, Loader>(Unit) {
        override fun getKoin(): Koin = koin
    }

    private class ScreenViewModel(
        private val koin: Koin,
        recorder: Recording,
        navigateImmediately: Boolean = false,
    ) : BaseViewModel<Unit, Direction, Loader>(Unit) {
        override val screenPresentation = recorder::present

        init {
            if (navigateImmediately) navigateTo(Direction.Exit)
        }

        override fun getKoin(): Koin = koin
    }

    private class ScreenComponent(
        lifecycle: LifecycleRegistry,
        override val viewModel: ScreenViewModel,
        private val koin: Koin,
        private val onNavigate: () -> Unit = {},
    ) : BaseComponent<Direction>(DefaultComponentContext(lifecycle)) {
        override fun getKoin(): Koin = koin

        override suspend fun eventListener(direction: Direction) {
            onNavigate()
        }

        @Composable
        override fun Render() = Unit
    }

    private enum class Direction : BaseDirection {
        Exit,
    }

    private sealed interface Loader : BaseLoader
}
