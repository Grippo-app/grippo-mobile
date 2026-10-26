package com.grippo.toolkit.date.utils

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

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
        public data object DateDdMmmYyyy : DateOnly {
            override val pattern: String = "d MMM yyyy"
        }

        @Immutable
        @Serializable
        public data object MmmYyyy : DateOnly {
            override val pattern: String = "MMM yyyy"
        }

        @Immutable
        @Serializable
        public data object DateDdMmm : DateOnly {
            override val pattern: String = "d MMM"
        }

        @Immutable
        @Serializable
        public data object DateDdMmmm : DateOnly {
            override val pattern: String = "d MMMM"
        }

        @Immutable
        @Serializable
        public data object WeekdayShort : DateOnly {
            override val pattern: String = "EEE"
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
