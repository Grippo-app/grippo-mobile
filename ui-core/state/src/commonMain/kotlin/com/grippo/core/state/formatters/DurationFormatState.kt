package com.grippo.core.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.decimal_separator
import com.grippo.design.resources.provider.minutes_short
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.toolkit.number.utils.formatDecimal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class DurationFormatState : FormatState<Duration> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Duration
    ) : DurationFormatState(), FormatState.Valid<Duration>

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Duration?
    ) : DurationFormatState(), FormatState.Invalid<Duration>

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Duration? = null
    ) : DurationFormatState(), FormatState.Empty<Duration>

    @Composable
    public fun short(): String {
        val duration = value ?: return display
        return DateCompose.rememberFormat(value = duration)
    }

    public companion object {
        @Composable
        public fun shortMinutes(value: Duration?): String {
            if (value == null || !value.isFinite()) return "—"
            val number = formatDecimal(
                value = value.toDouble(DurationUnit.MINUTES),
                decimalSeparator = AppTokens.strings.res(Res.string.decimal_separator),
            )
            return "$number ${AppTokens.strings.res(Res.string.minutes_short)}"
        }

        /** Read-only accumulated time is allowed to be zero or span multiple days. */
        public fun ofTotal(value: Duration?): DurationFormatState {
            if (value == null) {
                return Empty()
            }
            if (!value.isFinite() || value.isNegative()) {
                return Invalid(display = value.toString(), value = null)
            }

            val normalized = normalize1m(value)
            return Valid(display = display(normalized), value = normalized)
        }

        public val DurationLimitation: ClosedRange<Duration> =
            1.minutes..(23.hours + 59.minutes)

        private fun normalize1m(value: Duration): Duration {
            val minutes = value.inWholeMinutes
            return minutes.minutes
        }

        private fun display(value: Duration): String {
            return DateTimeUtils.format(value)
        }

        public fun of(display: String): DurationFormatState {
            if (display.isEmpty()) return Empty()

            return try {
                val parsed = Duration.parse(display)
                val normalized = normalize1m(parsed)

                when {
                    normalized == Duration.ZERO -> Empty()

                    DurationValidator.isValid(normalized) -> Valid(
                        display = this.display(normalized),
                        value = normalized
                    )

                    else -> Invalid(
                        display = this.display(normalized),
                        value = normalized
                    )
                }
            } catch (_: IllegalArgumentException) {
                Invalid(display = display, value = null)
            }
        }

        public fun of(value: Duration?): DurationFormatState {
            val normalized = value?.let(::normalize1m)

            return when {
                normalized == null -> Empty()
                normalized == Duration.ZERO -> Empty()

                DurationValidator.isValid(normalized) -> Valid(
                    display = display(normalized),
                    value = normalized
                )

                else -> Invalid(
                    display = display(normalized),
                    value = normalized
                )
            }
        }
    }

    private object DurationValidator {
        fun isValid(value: Duration): Boolean {
            if (value.isNegative()) return false
            val withinRange = value in DurationLimitation
            val hasMinutePrecision = value == value.inWholeMinutes.minutes
            return withinRange && hasMinutePrecision
        }
    }
}
