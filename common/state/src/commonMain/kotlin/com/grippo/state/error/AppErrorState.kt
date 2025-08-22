package com.grippo.state.error

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

@Immutable
public sealed interface AppErrorState {

    @Immutable
    public sealed interface Network : AppErrorState {

        @Immutable
        public data class Expected(
            val title: String,
            val description: String?,
        ) : Network

        @Immutable
        public data class NoInternet(
            val description: String?,
        ) : Network

        @Immutable
        public data class Timeout(
            val description: String?,
        ) : Network

        @Immutable
        public data class Unexpected(
            val description: String?,
        ) : Network
    }

    @Immutable
    public data class Unknown(
        val description: String?,
    ) : AppErrorState

    @Composable
    public fun title(): String {
        return when (this) {
            is Network.Expected -> this.title
            is Network.NoInternet -> "No internet"
            is Network.Timeout -> "Timeout"
            is Network.Unexpected -> "Unexpected error"
            is Unknown -> "Unsupported error"
        }
    }

    @Composable
    public fun description(): String? {
        return when (this) {
            is Network.Expected -> this.description
            is Network.NoInternet -> this.description
            is Network.Timeout -> this.description
            is Network.Unexpected -> this.description
            is Unknown -> this.description
        }
    }
}
