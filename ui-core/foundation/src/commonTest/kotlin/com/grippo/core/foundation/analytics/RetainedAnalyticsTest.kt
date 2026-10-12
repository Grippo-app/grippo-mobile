package com.grippo.core.foundation.analytics

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class RetainedAnalyticsTest {
    private class Scenario(var value: Int)
    private class AnotherScenario(val value: Int)
    private var creations = 0

    @Test fun retentionUsesScenarioTypeAndSaveReadsItsLatestValue() {
        val instances = InstanceKeeperDispatcher()
        val firstState = StateKeeperDispatcher()
        val firstOwner = DefaultComponentContext(LifecycleRegistry(), firstState, instances)
        val first = provide(firstOwner)
        val nextState = StateKeeperDispatcher(firstState.save())
        val next = provide(DefaultComponentContext(LifecycleRegistry(), nextState, instances))
        assertSame(first, next)
        assertEquals(1, creations)
        first.value = 42
        val restored = provide(DefaultComponentContext(LifecycleRegistry(), StateKeeperDispatcher(nextState.save())))
        assertNotSame(first, restored)
        assertEquals(42, restored.value)
        assertEquals(2, creations)
        val another = RetainedAnalytics(firstOwner).provide<AnotherScenario, Int>(
            key = "another",
            serializer = Int.serializer(),
            create = { AnotherScenario(it ?: 7) },
            save = { it.value },
        )
        assertEquals(7, another.value)
    }

    @Test fun validationFailureDoesNotRegisterSaveOrReplaceRetainedScenario() {
        val owner = DefaultComponentContext(LifecycleRegistry())
        val retained = RetainedAnalytics(owner)
        lateinit var original: Scenario
        assertFailsWith<IllegalArgumentException> {
            retained.provide<Scenario, Int>(
                key = "scenario",
                serializer = Int.serializer(),
                create = { Scenario(1).also { original = it } },
                validate = { require(it.value == 2) },
                save = { it.value },
            )
        }
        // This succeeds only if validation did not register the previous supplier.
        val valid = retained.provide<Scenario, Int>(
            key = "scenario",
            serializer = Int.serializer(),
            create = { error("Retained scenario must be reused") },
            validate = { require(it.value == 1) },
            save = { it.value },
        )
        assertSame(original, valid)
    }

    private fun provide(owner: DefaultComponentContext): Scenario =
        RetainedAnalytics(owner).provide<Scenario, Int>(
            key = "scenario",
            serializer = Int.serializer(),
            create = {
                creations++
                Scenario(it ?: 0)
            },
            save = { it.value },
        )
}
