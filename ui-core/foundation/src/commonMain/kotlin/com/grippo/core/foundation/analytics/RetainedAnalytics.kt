package com.grippo.core.foundation.analytics

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedSimpleInstance
import kotlinx.serialization.KSerializer

/** Owner-bound restore/retain/save mechanism. Keep route mapping and keys in the feature provider. */
public class RetainedAnalytics(
    @PublishedApi internal val context: ComponentContext,
) {
    /** One scenario of type T per owner. Validation precedes save registration and never replaces state. */
    public inline fun <reified T : Any, S : Any> provide(
        key: String,
        serializer: KSerializer<S>,
        crossinline create: (S?) -> T,
        crossinline validate: (T) -> Unit = {},
        crossinline save: (T) -> S,
    ): T {
        val restored = context.stateKeeper.consume(key, serializer)
        val analytics = context.retainedSimpleInstance<T> { create(restored) }
        validate(analytics)
        context.stateKeeper.register(key, serializer) { save(analytics) }
        return analytics
    }
}
