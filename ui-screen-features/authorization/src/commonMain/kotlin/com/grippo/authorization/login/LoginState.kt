package com.grippo.authorization.login

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.data.features.api.local.settings.models.AnalyticsConsent
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Immutable
@Serializable
internal data class LoginState(
    val analyticsConsentGranted: Boolean = false,
    @Transient
    val analyticsConsent: AnalyticsConsent? = null,
    val email: EmailFormatState = EmailFormatState.Empty(),
    @Transient
    val password: PasswordFormatState = PasswordFormatState.Empty(),
    @Transient
    val isGoogleLoginAvailable: Boolean = false,
    @Transient
    val isAppleLoginAvailable: Boolean = false,
)
