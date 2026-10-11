package com.grippo.toolkit.analytics.core

import com.grippo.toolkit.analytics.*

/** Opt-in test/preview sink. Stores raw fixtures in memory; never attach in production. */
public class RecordingAnalyticsSink(private val capacity: Int = 128) : AnalyticsSink {
    private val lock = AnalyticsLock()
    private val events = ArrayDeque<AnalyticsPayload>()
    private var collection = AnalyticsCollection.Unknown
    private var identity = AnalyticsIdentityPayload(null, emptyMap(), emptyList())

    init { require(capacity > 0) }

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

    public fun payloads(): List<AnalyticsPayload> = lock.withLock { events.toList() }
    public fun identity(): AnalyticsIdentityPayload = lock.withLock { identity }
    public fun collection(): AnalyticsCollection = lock.withLock { collection }
}
