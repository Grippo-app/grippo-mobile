package com.grippo.toolkit.date.utils

import androidx.compose.runtime.Immutable

// https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime.format/by-unicode-pattern.html
@Immutable
public sealed interface DateFormat {
    public val pattern: String

    public sealed interface DateOnly : DateFormat {
        @Immutable
        public data object MonthFull : DateOnly {
            override val pattern: String = "MMMM"
        }

        @Immutable
        public data object MonthFullStandalone : DateOnly {
            override val pattern: String = "LLLL"
        }

        @Immutable
        public data object MonthShort : DateOnly {
            override val pattern: String = "MMM"
        }

        @Immutable
        public data object MonthShortStandalone : DateOnly {
            override val pattern: String = "LLL"
        }

        @Immutable
        public data object DateMmmDdYyyy : DateOnly {
            override val pattern: String = "MMM dd, yyyy"
        }

        @Immutable
        public data object DateMmmDdComma : DateOnly {
            override val pattern: String = "MMM, dd"
        }

        @Immutable
        public data object DateDdMmm : DateOnly {
            override val pattern: String = "dd MMM"
        }

        @Immutable
        public data object Mmmm : DateOnly {
            override val pattern: String = "MMMM"
        }

        @Immutable
        public data object DateDdMmmm : DateOnly {
            override val pattern: String = "dd MMMM"
        }

        @Immutable
        public data object WeekdayShort : DateOnly {
            override val pattern: String = "EEE"
        }

        @Immutable
        public data object WeekdayLong : DateOnly {
            override val pattern: String = "EEEE"
        }
    }

    public sealed interface TimeOnly : DateFormat {
        @Immutable
        public data object Time24hHhMm : TimeOnly {
            override val pattern: String = "HH:mm"
        }

        @Immutable
        public data object Time24hHm : TimeOnly {
            override val pattern: String = "H:mm"
        }
    }
}