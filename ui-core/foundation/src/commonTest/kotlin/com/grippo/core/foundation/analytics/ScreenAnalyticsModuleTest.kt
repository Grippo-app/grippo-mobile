package com.grippo.core.foundation.analytics

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.grippo.core.foundation.CoreModule
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.koin.dsl.koinApplication
import org.koin.ksp.generated.module

class ScreenAnalyticsModuleTest {
    @Test fun generatedDiResolvesSingletonWithIndependentBindings() {
        val app = koinApplication { modules(CoreModule().module) }
        try {
            val binder = app.koin.get<ScreenAnalytics>()
            assertSame(binder, app.koin.get<ScreenAnalytics>())
            val first = LifecycleRegistry()
            val second = LifecycleRegistry()
            val closes = mutableListOf<Int>()
            binder.bind(first) { Presentation(closes) }
            binder.bind(second) { Presentation(closes) }
            first.resume()
            second.resume()
            first.destroy()
            assertEquals(listOf(1, 0), closes)
            second.pause()
            second.resume()
            second.destroy()
            assertEquals(listOf(1, 1, 1), closes)
        } finally {
            app.close()
        }
    }

    private class Presentation(private val closes: MutableList<Int>) : ScreenPresentation {
        private val index = closes.size

        init {
            closes.add(0)
        }

        override fun close() {
            closes[index]++
        }
    }
}
