package com.grippo.state.error

import androidx.compose.runtime.Immutable
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.no_internet_connection
import com.grippo.design.resources.provider.timeout_error
import com.grippo.design.resources.provider.unexpected_network_error
import com.grippo.design.resources.provider.unsupported_error
import com.grippo.state.formatters.UiText

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
    public data object Unknown : AppErrorState

    public fun title(): UiText {
        return when (this) {
            is Network.Expected -> UiText.Str(this.title)
            is Network.NoInternet -> UiText.Res(Res.string.no_internet_connection)
            is Network.Timeout -> UiText.Res(Res.string.timeout_error)
            is Network.Unexpected -> UiText.Res(Res.string.unexpected_network_error)
            is Unknown -> UiText.Res(Res.string.unsupported_error) // todo rename (Something went wrong)
        }
    }

    public fun description(): UiText? {
        return when (this) {
            is Network.Expected -> this.description?.let { UiText.Str(it) }
            is Network.NoInternet -> this.description?.let { UiText.Str(it) }
            is Network.Timeout -> this.description?.let { UiText.Str(it) }
            is Network.Unexpected -> this.description?.let { UiText.Str(it) }
            is Unknown -> null
        }
    }
}
