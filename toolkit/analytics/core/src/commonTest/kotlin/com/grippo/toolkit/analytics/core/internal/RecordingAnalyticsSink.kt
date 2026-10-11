package com.grippo.toolkit.analytics.core.internal

import com.grippo.toolkit.analytics.lifecycle.AnalyticsCollection
import com.grippo.toolkit.analytics.transport.AnalyticsIdentityPayload
import com.grippo.toolkit.analytics.transport.AnalyticsPayload
import com.grippo.toolkit.analytics.transport.AnalyticsSink

/** Bounded test fixture; excluded from production source sets. */
internal class RecordingAnalyticsSink(private val capacity: Int = 128) : AnalyticsSink {
    private val lock = AnalyticsLock()
    private val events = ArrayDeque<AnalyticsPayload>()
    private var collection = AnalyticsCollection.Unknown
    private var identity = AnalyticsIdentityPayload(null, emptyMap(), emptyList())

    init {
        require(capacity > 0)
    }

    override fun setCollection(collection: AnalyticsCollection): Unit = lock.withLock {
        this.collection = collection
    }

    override fun setIdentity(identity: AnalyticsIdentityPayload): Unit = lock.withLock {
        this.identity = identity
    }

    override fun send(payload: AnalyticsPayload): Unit = lock.withLock {
        check(collection == AnalyticsCollection.Granted)
        if (events.size == capacity) events.removeFirst()
        events.addLast(payload)
    }

    fun payloads(): List<AnalyticsPayload> = lock.withLock { events.toList() }
    fun identity(): AnalyticsIdentityPayload = lock.withLock { identity }
    fun collection(): AnalyticsCollection = lock.withLock { collection }
}
