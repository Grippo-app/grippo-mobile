package com.grippo.core.foundation.models

import kotlinx.serialization.KSerializer

/** Stable archive address and serializer for one typed, component-owned draft. */
public class PersistenceKey<T : Any>(
    internal val key: String,
    internal val serializer: KSerializer<T>,
)
