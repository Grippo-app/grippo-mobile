package com.grippo.core.state.formatters

import androidx.compose.runtime.Immutable
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Immutable
@Serializable
public sealed class DurationFormatState : FormatState<Duration> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Duration
    ) : DurationFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Duration?
    ) : DurationFormatState()

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Duration? = null
    ) : DurationFormatState()

    public companion object {
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
