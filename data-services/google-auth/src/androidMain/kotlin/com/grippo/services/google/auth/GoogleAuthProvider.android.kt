package com.grippo.services.google.auth

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.grippo.toolkit.context.NativeContext
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

public actual class GoogleAuthProvider actual constructor(
    nativeContext: NativeContext,
    @Suppress("UNUSED_PARAMETER") httpClient: HttpClient,
    @Suppress("UNUSED_PARAMETER") json: Json,
) {
    private companion object {
        private const val META_SERVER_CLIENT_ID = "com.grippo.google.SERVER_CLIENT_ID"
        private const val CLIENT_ID_SUFFIX = ".apps.googleusercontent.com"
    }

    private val applicationContext: Context = nativeContext.context
    private val credentialManager: CredentialManager = CredentialManager.create(applicationContext)

    public actual val isSupported: Boolean
        get() = readMetaData()?.containsKey(META_SERVER_CLIENT_ID) == true

    public actual fun getUiProvider(context: GoogleAuthUiContext): GoogleAuthUiProvider {
        val activityContext = context.asAndroidContext()

        val clientId = readServerClientId()

        if (!clientId.endsWith(CLIENT_ID_SUFFIX)) {
            throw GoogleAuthException.InvalidServerClientId(
                message = "Google server client id is invalid. Expected suffix '$CLIENT_ID_SUFFIX'.",
            )
        }

        return AndroidGoogleAuthUiProvider(
            activityContext = activityContext,
            credentialManager = credentialManager,
            serverClientId = clientId,
        )
    }

    public actual suspend fun signOut() {
        if (!isSupported) {
            return
        }

        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (error: Exception) {
            if (error is CancellationException) {
                throw error
            }
            throw GoogleAuthException.CredentialManagerFailed(
                message = "Failed to clear Google credential state",
                cause = error,
            )
        }
    }

    private fun readServerClientId(): String {
        val metadata = readMetaData()
        if (metadata == null || !metadata.containsKey(META_SERVER_CLIENT_ID)) {
            throw GoogleAuthException.ProviderMisconfigured(
                message = "Google server client id is missing. Add meta-data '$META_SERVER_CLIENT_ID' to AndroidManifest.",
            )
        }

        val rawClientId = metadata.getString(META_SERVER_CLIENT_ID)
            ?: throw GoogleAuthException.ProviderMisconfigured(
                message = "Google server client id must be a string.",
            )
        val clientId = rawClientId.trim()
        if (clientId.isEmpty()) {
            throw GoogleAuthException.ProviderMisconfigured(
                message = "Google server client id is empty.",
            )
        }

        return clientId
    }

    private fun readMetaData(): Bundle? {
        val info = applicationContext.packageManager.getApplicationInfo(
            applicationContext.packageName,
            PackageManager.GET_META_DATA,
        )

        return info.metaData
    }
}
