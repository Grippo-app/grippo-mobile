package com.grippo.state.error

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.no_internet_connection
import com.grippo.design.resources.provider.timeout_error
import com.grippo.design.resources.provider.unexpected_network_error
import com.grippo.design.resources.provider.unsupported_error

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
            is Network.NoInternet -> AppTokens.strings.res(Res.string.no_internet_connection)
            is Network.Timeout -> AppTokens.strings.res(Res.string.timeout_error)
            is Network.Unexpected -> AppTokens.strings.res(Res.string.unexpected_network_error)
            is Unknown -> AppTokens.strings.res(Res.string.unsupported_error)
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
