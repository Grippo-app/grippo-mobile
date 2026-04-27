package com.grippo.core.state.error

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.no_internet_connection
import com.grippo.design.resources.provider.something_went_wrong
import com.grippo.design.resources.provider.timeout_error
import com.grippo.design.resources.provider.unexpected_network_error
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed interface AppErrorState {

    @Immutable
    @Serializable
    public sealed interface Network : AppErrorState {

        @Immutable
        @Serializable
        public data class Expected(
            val title: String,
            val description: String?,
        ) : Network

        @Immutable
        @Serializable
        public data class NoInternet(
            val description: String?,
        ) : Network

        @Immutable
        @Serializable
        public data class Timeout(
            val description: String?,
        ) : Network

        @Immutable
        @Serializable
        public data class Unexpected(
            val description: String?,
        ) : Network
    }

    @Immutable
    @Serializable
    public data class Expected(
        val title: String,
        val description: String?,
    ) : Network

    @Immutable
    @Serializable
    public data object Unknown : AppErrorState

    public fun title(): UiText {
        return when (this) {
            is Network.Expected -> UiText.Str(this.title)
            is Network.NoInternet -> TITLE_NO_INTERNET
            is Network.Timeout -> TITLE_TIMEOUT
            is Network.Unexpected -> TITLE_UNEXPECTED_NETWORK
            is Expected -> UiText.Str(this.title)
            is Unknown -> TITLE_UNKNOWN
        }
    }

    public fun description(): UiText? {
        return when (this) {
            is Network.Expected -> this.description?.let { UiText.Str(it) }
            is Network.NoInternet -> this.description?.let { UiText.Str(it) }
            is Network.Timeout -> this.description?.let { UiText.Str(it) }
            is Network.Unexpected -> this.description?.let { UiText.Str(it) }
            is Expected -> this.description?.let { UiText.Str(it) }
            is Unknown -> null
        }
    }

    public companion object {
        private val TITLE_NO_INTERNET: UiText = UiText.Res(Res.string.no_internet_connection)
        private val TITLE_TIMEOUT: UiText = UiText.Res(Res.string.timeout_error)
        private val TITLE_UNEXPECTED_NETWORK: UiText =
            UiText.Res(Res.string.unexpected_network_error)
        private val TITLE_UNKNOWN: UiText = UiText.Res(Res.string.something_went_wrong)
    }
}
