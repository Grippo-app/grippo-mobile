package com.grippo.core.foundation.models

import kotlinx.serialization.KSerializer

public class PersistenceKey<T : Any>(
    internal val key: String,
    internal val serializer: KSerializer<T>,
)
