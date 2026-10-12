package com.grippo.authorization.analytics

import com.grippo.analytics.authorization.AuthAnalytics.FailureClassifier
import com.grippo.analytics.authorization.models.FailureCategory
import com.grippo.core.error.provider.AppError
import com.grippo.services.apple.auth.AppleAuthException
import com.grippo.services.apple.auth.AppleAuthFailureReason
import com.grippo.services.google.auth.GoogleAuthException

internal object AuthFailureClassifierImpl : FailureClassifier {

    override fun isProviderCancellation(error: Exception): Boolean =
        error is GoogleAuthException.Cancelled ||
                (error is AppleAuthException && error.reason == AppleAuthFailureReason.Cancelled)

    override fun category(error: Exception): FailureCategory = when (error) {
        is AppError.Network.NoInternet -> FailureCategory.Offline
        is AppError.Network.Timeout -> FailureCategory.Timeout
        is AppError.Network.Expected -> FailureCategory.Rejected
        is AppError.Network.Unexpected -> FailureCategory.Server
        is GoogleAuthException.InvalidServerClientId,
        is GoogleAuthException.ProviderMisconfigured -> FailureCategory.ProviderConfiguration

        is GoogleAuthException.NoCredential,
        is GoogleAuthException.UnsupportedCredential,
        is GoogleAuthException.TokenParseFailed -> FailureCategory.ProviderCredential

        is GoogleAuthException.Interrupted -> FailureCategory.ProviderInterrupted
        else -> FailureCategory.Unexpected
    }
}
