package com.grippo.toolkit.analytics.core.internal

import com.grippo.toolkit.analytics.*
import kotlin.test.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*

class AnalyticsTest {
    @Test fun oversizedCatalogIsRejectedBeforeItsElementsAreRead() {
        val events = object : AbstractList<EventDefinition>() {
            override val size = 501
            override fun get(index: Int): EventDefinition = error("Oversized catalog must not be read")
        }
        val properties = object : AbstractList<UserPropertyKey<*>>() {
            override val size = 26
            override fun get(index: Int): UserPropertyKey<*> = error("Oversized properties must not be read")
        }
        assertFailsWith<IllegalArgumentException> { AnalyticsCatalog(definitions = events) }
        assertFailsWith<IllegalArgumentException> { AnalyticsCatalog(properties = properties) }
    }

    @Test fun featureCatalogIncludesBuiltInsInItsBudgetAndRejectsBeforeFlattening() {
        val accepted = AnalyticsDefinitions(List(498) { EventDefinition("fixture_$it", 1, emptyList()) })
        AnalyticsCatalog.fromDefinitions(listOf(accepted))
        val oversized = AnalyticsDefinitions(List(499) { ScreenViewed.Definition })
        val failure = assertFailsWith<IllegalArgumentException> {
            AnalyticsCatalog.fromDefinitions(listOf(oversized))
        }
        assertEquals("Too many analytics event definitions", failure.message)
        val first = AnalyticsDefinitions(emptyList(), List(13) { UserPropertyKey<Source>("first_$it") })
        val second = AnalyticsDefinitions(emptyList(), List(13) { UserPropertyKey<Source>("second_$it") })
        assertEquals("Too many analytics user properties", assertFailsWith<IllegalArgumentException> {
            AnalyticsCatalog.fromDefinitions(listOf(first, second))
        }.message)
    }

    private enum class Source(override val analyticsCode: String) : AnalyticsCode { Button("button") }
    private class Event(private val count: Long = 3) : AnalyticsEvent {
        override val definition = Definition
        override fun parameters() = listOf(Count.value(AnalyticsCount(count)), SourceKey.value(Source.Button),
            Duration.value(DurationMillis(1234)), Number.value(AnalyticsNumber(2.5)), Flag.value(true))
        companion object {
            val Count = ParameterKey.count("exercise_count")
            val SourceKey = ParameterKey.code<Source>("source")
            val Duration = ParameterKey.duration("duration_ms")
            val Number = ParameterKey.number("score")
            val Flag = ParameterKey.flag("confirmed")
            val Definition = EventDefinition("test_event", 1, listOf(Count, SourceKey, Duration, Number, Flag))
        }
    }
    private class Sink : AnalyticsSink {
        val payloads = mutableListOf<AnalyticsPayload>()
        val users = mutableListOf<AnalyticsUserId?>()
        val collections = mutableListOf<AnalyticsCollection>()
        var failSend = false
        var failConfigure = false
        var onSend: (() -> Unit)? = null
        override fun setCollection(collection: AnalyticsCollection) {
            if (failConfigure) error("transport")
            collections.add(collection)
        }
        override fun setIdentity(identity: AnalyticsIdentityPayload) { users.add(identity.userId) }
        override fun send(payload: AnalyticsPayload) {
            if (failSend) error("transport")
            payloads.add(payload)
            onSend?.invoke()
        }
    }
    private fun core(dispatcher: kotlinx.coroutines.CoroutineDispatcher, capacity: Int = 128) =
        DefaultAnalytics(AnalyticsCatalog(listOf(Event.Definition)), dispatcher, capacity, 100)

    @Test fun nativeValuesAndContextAreCapturedAtCallTime() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val sink = Sink()
        core.updateCollection(AnalyticsCollection.Granted)
        core.updateContext(AnalyticsContext(NavigationContext(ScreenId("home"), reason = NavigationReason.Initial)))
        core.track(Event(), FlowContext(FlowId("flow1"), EntryPoint("home_start"), ScreenId("home")), OperationId("op1"))
        core.updateContext(AnalyticsContext(NavigationContext(ScreenId("training"), ScreenId("home"), NavigationReason.Action)))
        core.track(Event(4), FlowContext(FlowId("flow2"), EntryPoint("library_start"), ScreenId("library")))
        core.attachSink(sink)
        advanceUntilIdle()
        assertEquals(2, sink.payloads.size)
        val first = sink.payloads.first().parameters
        assertEquals(AnalyticsValue.Integer(3), first["exercise_count"])
        assertEquals(AnalyticsValue.Integer(1234), first["duration_ms"])
        assertEquals(AnalyticsValue.Decimal(2.5), first["score"])
        assertEquals(AnalyticsValue.Integer(1), first["confirmed"])
        assertEquals(AnalyticsValue.Text("home"), first["screen_id"])
        assertEquals(AnalyticsValue.Text("home_start"), first["entry_point"])
        assertEquals(AnalyticsValue.Text("op1"), first["operation_id"])
        assertEquals(AnalyticsValue.Text("library_start"), sink.payloads.last().parameters["entry_point"])
        assertEquals(AnalyticsValue.Text("training"), sink.payloads.last().parameters["screen_id"])
        assertNull(sink.payloads.last().parameters["operation_id"])
        core.close()
    }

    @Test fun unknownAndDeniedNeverBuffer() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        core.track(Event())
        core.updateCollection(AnalyticsCollection.Denied)
        core.track(Event())
        assertEquals(0, core.queuedEventCount())
        val sink = Sink(); core.attachSink(sink)
        core.updateCollection(AnalyticsCollection.Granted)
        advanceUntilIdle()
        assertTrue(sink.payloads.isEmpty())
        core.close()
    }

    @Test fun boundedQueuePreservesOrderAndDoesNotDeduplicateRealActions() = runTest {
        val core = core(StandardTestDispatcher(testScheduler), 2)
        core.updateCollection(AnalyticsCollection.Granted)
        core.track(Event(1)); core.track(Event(1)); core.track(Event(3))
        assertEquals(2, core.queuedEventCount())
        assertTrue(core.diagnostics().any { it.disposition == AnalyticsDisposition.QueueFull })
        val sink = Sink(); core.attachSink(sink); advanceUntilIdle()
        assertEquals(listOf(AnalyticsValue.Integer(1), AnalyticsValue.Integer(1)), sink.payloads.map { it.parameters["exercise_count"] })
        core.close()
    }

    @Test fun identityChangeAndRevocationDiscardPendingEvents() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val sink = Sink(); core.attachSink(sink)
        core.updateCollection(AnalyticsCollection.Granted)
        core.track(Event())
        core.updateIdentity(AnalyticsIdentity(AnalyticsUserId("user2")))
        core.track(Event())
        core.updateCollection(AnalyticsCollection.Denied)
        advanceUntilIdle()
        assertTrue(sink.payloads.isEmpty())
        assertNull(sink.users.last())
        assertEquals(AnalyticsCollection.Denied, sink.collections.last())
        core.updateCollection(AnalyticsCollection.Granted)
        core.track(Event()); advanceUntilIdle()
        assertEquals(AnalyticsUserId("user2"), sink.users.last())
        assertEquals(1, sink.payloads.size)
        core.close()
    }

    @Test fun revocationDuringSendDropsRemainingQueue() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val sink = Sink(); sink.onSend = { core.updateCollection(AnalyticsCollection.Denied) }
        core.attachSink(sink); core.updateCollection(AnalyticsCollection.Granted)
        core.track(Event()); core.track(Event()); advanceUntilIdle()
        assertEquals(1, sink.payloads.size)
        assertEquals(AnalyticsCollection.Denied, sink.collections.last())
        core.close()
    }

    @Test fun transportFailureDoesNotKillNextEventOrLoopOnConfiguration() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val sink = Sink(); sink.failConfigure = true
        core.attachSink(sink); core.updateCollection(AnalyticsCollection.Granted)
        core.track(Event()); advanceUntilIdle()
        assertEquals(0, core.queuedEventCount())
        assertTrue(core.diagnostics().any { it.disposition == AnalyticsDisposition.TransportFailure })
        sink.failConfigure = false
        core.track(Event())
        assertEquals(0, core.queuedEventCount())
        core.retrySinkConfiguration()
        sink.failSend = true; core.track(Event()); advanceUntilIdle()
        sink.failSend = false; core.track(Event()); advanceUntilIdle()
        assertEquals(1, sink.payloads.size)
        core.close()
    }

    @Test fun catalogRejectsCollisionsReservedNamesAndParameterBudget() {
        assertFailsWith<IllegalArgumentException> { AnalyticsCatalog(listOf(Event.Definition, Event.Definition)) }
        for (name in listOf("ga_event", "app_exception", "bad-name", "1event")) {
            assertFailsWith<IllegalArgumentException> { AnalyticsCatalog(listOf(EventDefinition(name, 1, emptyList()))) }
        }
        assertFailsWith<IllegalArgumentException> { AnalyticsCatalog(listOf(EventDefinition("event", 0, emptyList()))) }
        assertFailsWith<IllegalArgumentException> { AnalyticsCatalog(listOf(EventDefinition("event", 1, listOf(ParameterKey.count("flow_id"))))) }
        assertFailsWith<IllegalArgumentException> { AnalyticsCatalog(listOf(EventDefinition("event", 1, (1..16).map { ParameterKey.count("p_$it") }))) }
        // Reserved event names are not reserved parameter/property names.
        AnalyticsCatalog(
            listOf(EventDefinition("event", 1, listOf(ParameterKey.count("session_start")))),
            listOf(UserPropertyKey<Source>("app_update")),
        )
    }

    @Test fun malformedPayloadsAndWrongDefinitionInstancesAreRejected() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val sink = Sink(); core.attachSink(sink); core.updateCollection(AnalyticsCollection.Granted)
        fun malformed(definition: EventDefinition, parameters: List<AnalyticsParameter>) = object : AnalyticsEvent {
            override val definition = definition
            override fun parameters() = parameters
        }
        core.track(malformed(Event.Definition, emptyList()))
        core.track(malformed(Event.Definition, Event().parameters() + Event.Count.value(AnalyticsCount(1))))
        core.track(malformed(Event.Definition, Event().parameters() + ParameterKey.count("other").value(AnalyticsCount(1))))
        core.track(malformed(EventDefinition("test_event", 1, Event.Definition.parameters), Event().parameters()))
        advanceUntilIdle()
        assertTrue(sink.payloads.isEmpty())
        assertEquals(4, core.diagnostics().count { it.disposition == AnalyticsDisposition.InvalidEvent })
        core.close()
    }

    @Test fun invalidEnumValuesAndIdentityPropertiesAreRejected() = runTest {
        val key = ParameterKey.code<AnalyticsCode>("source")
        val definition = EventDefinition("event", 1, listOf(key))
        val core = DefaultAnalytics(AnalyticsCatalog(listOf(definition)), StandardTestDispatcher(testScheduler), diagnosticCapacity = 10)
        core.updateCollection(AnalyticsCollection.Granted)
        core.track(object : AnalyticsEvent {
            override val definition = definition
            override fun parameters() = listOf(key.value(object : AnalyticsCode { override val analyticsCode = "email@example.com" }))
        })
        core.updateIdentity(AnalyticsIdentity(properties = listOf(UserPropertyKey<Source>("undeclared").value(Source.Button))))
        assertEquals(listOf(AnalyticsDisposition.InvalidEvent, AnalyticsDisposition.InvalidIdentity), core.diagnostics().map { it.disposition })
        core.close()
    }

    @Test fun valueTypesValidateAndDiagnosticsAreBounded() = runTest {
        assertFailsWith<IllegalArgumentException> { AnalyticsCount(-1) }
        assertFailsWith<IllegalArgumentException> { DurationMillis(-1) }
        assertFailsWith<IllegalArgumentException> { AnalyticsNumber(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { AnalyticsUserId("email@example.com") }
        val core = DefaultAnalytics(dispatcher = StandardTestDispatcher(testScheduler), diagnosticCapacity = 2)
        repeat(10) { core.track(Event()) }
        assertEquals(2, core.diagnostics().size)
        core.close(); core.track(Event())
        assertEquals(AnalyticsDisposition.Closed, core.diagnostics().last().disposition)
        assertFailsWith<IllegalStateException> { core.attachSink(Sink()) }
    }

    @Test fun propertiesAreExplicitlyClearedIncludingAfterRestart() {
        val key = UserPropertyKey<Source>("source")
        val catalog = AnalyticsCatalog(properties = listOf(key))
        val identity = AnalyticsIdentity(AnalyticsUserId("user1"), listOf(key.value(Source.Button)))
        catalog.validateIdentity(identity)
        assertEquals(mapOf("source" to "button"), catalog.encodeIdentity(identity).properties)
        val cleared = catalog.encodeIdentity(AnalyticsIdentity())
        assertNull(cleared.userId)
        assertEquals(listOf("source"), cleared.clearedPropertyNames)
        assertFailsWith<IllegalArgumentException> { AnalyticsCatalog(properties = listOf(UserPropertyKey<Source>("user_id"))) }
        assertFailsWith<IllegalArgumentException> { catalog.validateIdentity(AnalyticsIdentity(properties = listOf(key.value(Source.Button), key.value(Source.Button)))) }
    }

    @Test fun identityUpdatesDoNotTemporarilyRevokeGrantedConsent() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val sink = Sink()
        core.attachSink(sink); core.updateCollection(AnalyticsCollection.Granted); advanceUntilIdle()
        core.updateIdentity(AnalyticsIdentity(AnalyticsUserId("user1"))); advanceUntilIdle()
        core.updateIdentity(AnalyticsIdentity()); advanceUntilIdle()
        assertTrue(sink.collections.all { it == AnalyticsCollection.Granted })
        core.close()
    }

    @Test fun debugModeFailsFastAndRecordingSinkRemainsBounded() = runTest {
        val core = DefaultAnalytics(AnalyticsCatalog(listOf(Event.Definition)), StandardTestDispatcher(testScheduler), validationMode = AnalyticsValidationMode.Throw)
        core.updateCollection(AnalyticsCollection.Granted)
        assertFailsWith<IllegalArgumentException> { core.track(ScreenViewed(ScreenId("home"))) }
        val sink = RecordingAnalyticsSink(1)
        core.attachSink(sink); core.track(Event(1)); core.track(Event(2)); advanceUntilIdle()
        assertEquals(AnalyticsValue.Integer(2), sink.payloads().single().parameters["exercise_count"])
        core.close()
        assertEquals(AnalyticsCollection.Denied, sink.collection())
    }

    @Test fun staleConfigurationFailureDoesNotDiscardNewIdentityEvents() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val payloads = mutableListOf<AnalyticsPayload>()
        var identityCalls = 0
        val sink = object : AnalyticsSink {
            override fun setCollection(collection: AnalyticsCollection) = Unit
            override fun setIdentity(identity: AnalyticsIdentityPayload) {
                if (identityCalls++ == 0) {
                    core.updateIdentity(AnalyticsIdentity(AnalyticsUserId("next_user")))
                    core.track(Event(42))
                    error("Old configuration failed after identity changed")
                }
            }
            override fun send(payload: AnalyticsPayload) { payloads.add(payload) }
        }
        core.attachSink(sink)
        core.updateCollection(AnalyticsCollection.Granted)
        advanceUntilIdle()
        assertEquals(AnalyticsValue.Integer(42), payloads.single().parameters["exercise_count"])
        core.close()
    }

    @Test fun transportCancellationExceptionDoesNotSilentlyKillWorker() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val payloads = mutableListOf<AnalyticsPayload>()
        var configureCalls = 0
        var sendCalls = 0
        val sink = object : AnalyticsSink {
            override fun setCollection(collection: AnalyticsCollection) = Unit
            override fun setIdentity(identity: AnalyticsIdentityPayload) {
                if (configureCalls++ == 0) throw CancellationException("SDK configuration failure")
            }
            override fun send(payload: AnalyticsPayload) {
                if (sendCalls++ == 0) throw CancellationException("SDK send failure")
                payloads.add(payload)
            }
        }
        core.attachSink(sink)
        core.updateCollection(AnalyticsCollection.Granted)
        core.track(Event())
        advanceUntilIdle()
        assertEquals(0, core.queuedEventCount())
        core.retrySinkConfiguration()
        core.track(Event(1))
        core.track(Event(2))
        advanceUntilIdle()
        assertEquals(AnalyticsValue.Integer(2), payloads.single().parameters["exercise_count"])
        core.close()
    }

    @Test fun sameIdentityWithReorderedPropertiesPreservesPendingEvents() = runTest {
        val first = UserPropertyKey<Source>("first")
        val second = UserPropertyKey<Source>("second")
        val core = DefaultAnalytics(
            AnalyticsCatalog(listOf(Event.Definition), listOf(first, second)),
            StandardTestDispatcher(testScheduler),
        )
        val user = AnalyticsUserId("user")
        core.updateCollection(AnalyticsCollection.Granted)
        core.updateIdentity(AnalyticsIdentity(user, listOf(first.value(Source.Button), second.value(Source.Button))))
        core.track(Event())
        core.updateIdentity(AnalyticsIdentity(user, listOf(second.value(Source.Button), first.value(Source.Button))))
        assertEquals(1, core.queuedEventCount())
        core.close()
    }

    @Test fun workerYieldsToOtherMainDispatcherWorkDuringLargeDrain() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val core = core(dispatcher)
        val sink = Sink()
        core.attachSink(sink)
        core.updateCollection(AnalyticsCollection.Granted)
        repeat(100) { core.track(Event()) }
        var sentBeforeOtherWork = -1
        launch(dispatcher) { sentBeforeOtherWork = sink.payloads.size }
        advanceUntilIdle()
        assertTrue(sentBeforeOtherWork in 1 until 100)
        assertEquals(100, sink.payloads.size)
        core.close()
    }

    @Test fun cancelledCallerStillClosesWorkerAndDisablesSink() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val sink = Sink()
        core.attachSink(sink)
        core.updateCollection(AnalyticsCollection.Granted)
        advanceUntilIdle()
        launch {
            cancel()
            core.close()
        }.join()
        assertEquals(AnalyticsCollection.Denied, sink.collections.last())
        core.track(Event())
        assertEquals(0, core.queuedEventCount())
    }

    @Test fun shutdownAccountsForPendingEvents() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        core.updateCollection(AnalyticsCollection.Granted)
        core.track(Event())
        core.track(Event())
        core.close()
        assertEquals(0, core.queuedEventCount())
        assertEquals(2, core.diagnostics().count { it.disposition == AnalyticsDisposition.Closed && it.eventName == "test_event" })
    }

    @Test fun initialGrantConfiguresIdentityWhileSinkIsEnabled() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val usersAtSend = mutableListOf<AnalyticsUserId?>()
        val user = AnalyticsUserId("user1")
        var enabled = false
        var sdkUser: AnalyticsUserId? = null
        val sink = object : AnalyticsSink {
            override fun setCollection(collection: AnalyticsCollection) {
                enabled = collection == AnalyticsCollection.Granted
            }
            override fun setIdentity(identity: AnalyticsIdentityPayload) {
                // SDKs may ignore property writes while measurement is disabled.
                if (enabled) sdkUser = identity.userId
            }
            override fun send(payload: AnalyticsPayload) { usersAtSend.add(sdkUser) }
        }
        try {
            core.attachSink(sink)
            core.updateIdentity(AnalyticsIdentity(user))
            core.updateCollection(AnalyticsCollection.Granted)
            core.track(Event())
            advanceUntilIdle()
            assertEquals(listOf<AnalyticsUserId?>(user), usersAtSend)
        } finally {
            core.close()
        }
    }
}
