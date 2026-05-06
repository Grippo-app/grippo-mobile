package com.grippo.toolkit.date.utils

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

// https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime.format/by-unicode-pattern.html
@Immutable
@Serializable
public sealed interface DateFormat {
    public val pattern: String

    @Serializable
    public sealed interface DateOnly : DateFormat {
        @Immutable
        @Serializable
        public data object MonthFull : DateOnly {
            override val pattern: String = "MMMM"
        }

        @Immutable
        @Serializable
        public data object MonthFullStandalone : DateOnly {
            override val pattern: String = "LLLL"
        }

        @Immutable
        @Serializable
        public data object MonthShort : DateOnly {
            override val pattern: String = "MMM"
        }

        @Immutable
        @Serializable
        public data object MonthShortStandalone : DateOnly {
            override val pattern: String = "LLL"
        }

        @Immutable
        @Serializable
        public data object DateMmmDdYyyy : DateOnly {
            override val pattern: String = "MMM dd, yyyy"
        }

        @Immutable
        @Serializable
        public data object MmmYyyy : DateOnly {
            override val pattern: String = "MMM yyyy"
        }

        @Immutable
        @Serializable
        public data object DateMmmDdComma : DateOnly {
            override val pattern: String = "MMM, dd"
        }

        @Immutable
        @Serializable
        public data object DateDdMmm : DateOnly {
            override val pattern: String = "dd MMM"
        }

        @Immutable
        @Serializable
        public data object Mmmm : DateOnly {
            override val pattern: String = "MMMM"
        }

        @Immutable
        @Serializable
        public data object DateDdMmmm : DateOnly {
            override val pattern: String = "dd MMMM"
        }

        @Immutable
        @Serializable
        public data object WeekdayShort : DateOnly {
            override val pattern: String = "EEE"
        }

        @Immutable
        @Serializable
        public data object WeekdayLong : DateOnly {
            override val pattern: String = "EEEE"
        }
    }

    @Serializable
    public sealed interface TimeOnly : DateFormat {
        @Immutable
        @Serializable
        public data object Time24hHm : TimeOnly {
            override val pattern: String = "H:mm"
        }
    }
}
