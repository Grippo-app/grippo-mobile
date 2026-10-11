package com.grippo.toolkit.analytics.schema

import com.grippo.toolkit.analytics.transport.AnalyticsValue
import com.grippo.toolkit.analytics.values.AnalyticsCount
import com.grippo.toolkit.analytics.values.AnalyticsNumber
import com.grippo.toolkit.analytics.values.DurationMillis

/** Only declaration/encoding code creates keys. Event constructors carry business types. */
public class AnalyticsParameterKey<T> private constructor(
    public val name: String,
    public val required: Boolean,
    private val encode: (T) -> AnalyticsValue,
) {
    public fun value(value: T): AnalyticsParameter = AnalyticsParameter(this, encode(value))

    public companion object {
        public fun <T : AnalyticsCode> code(name: String, required: Boolean = true): AnalyticsParameterKey<T> =
            AnalyticsParameterKey(name, required) { AnalyticsValue.Text(it.analyticsCode) }
        public fun count(name: String, required: Boolean = true): AnalyticsParameterKey<AnalyticsCount> =
            AnalyticsParameterKey(name, required) { AnalyticsValue.Integer(it.value) }
        public fun duration(name: String, required: Boolean = true): AnalyticsParameterKey<DurationMillis> =
            AnalyticsParameterKey(name, required) { AnalyticsValue.Integer(it.value) }
        public fun number(name: String, required: Boolean = true): AnalyticsParameterKey<AnalyticsNumber> =
            AnalyticsParameterKey(name, required) { AnalyticsValue.Decimal(it.value) }
        public fun flag(name: String, required: Boolean = true): AnalyticsParameterKey<Boolean> =
            AnalyticsParameterKey(name, required) { AnalyticsValue.Integer(if (it) 1 else 0) }
    }
}
