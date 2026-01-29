package com.grippo.services.google.auth

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

public actual typealias GoogleAuthUiProvider = AndroidGoogleAuthUiProvider

public class AndroidGoogleAuthUiProvider(
    private val activityContext: Context,
    private val credentialManager: CredentialManager,
    private val serverClientId: String,
) {
    public suspend fun signIn(): Result<GoogleAccount> {
        return runCatching {
            requireValidServerClientId(serverClientId)

            val credential = requestCredentialStable()

            handleCredential(credential)
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error -> Result.failure(error.asGoogleAuthException()) },
        )
    }

    private fun requireValidServerClientId(clientId: String) {
        val trimmed = clientId.trim()
        if (trimmed.isEmpty()) {
            throw GoogleAuthException.InvalidServerClientId(
                message = "Google sign-in misconfigured: serverClientId is empty",
            )
        }
        if (!trimmed.endsWith(".apps.googleusercontent.com")) {
            throw GoogleAuthException.InvalidServerClientId(
                message = "Google sign-in misconfigured: serverClientId must be a Web OAuth client id (*.apps.googleusercontent.com)",
            )
        }
    }

    private suspend fun requestCredentialStable(): Credential {
        val a = runCatching {
            credentialManager.getCredential(
                context = activityContext,
                request = requestGoogleId(
                    filterByAuthorizedAccounts = true,
                    autoSelectEnabled = true,
                ),
            ).credential
        }.getOrElse { error ->
            if (!shouldFallbackToNextStrategy(error)) throw error
            null
        }

        if (a != null) return a

        val b = runCatching {
            credentialManager.getCredential(
                context = activityContext,
                request = requestGoogleId(
                    filterByAuthorizedAccounts = false,
                    autoSelectEnabled = false,
                ),
            ).credential
        }.getOrElse { error ->
            if (!shouldFallbackToNextStrategy(error)) throw error
            null
        }

        if (b != null) return b

        val c = credentialManager.getCredential(
            context = activityContext,
            request = requestSiwgButton(),
        )

        return c.credential
    }

    private fun shouldFallbackToNextStrategy(error: Throwable): Boolean {
        return when (error) {
            is NoCredentialException -> true
            is GetCredentialInterruptedException -> true

            is GetCredentialCancellationException -> {
                val msg = (error.message ?: "").lowercase()
                val isExplicitUserCancel =
                    msg.contains("user canceled") ||
                            msg.contains("user cancelled") ||
                            msg.contains("canceled by the user") ||
                            msg.contains("cancelled by the user")

                !isExplicitUserCancel
            }

            is GetCredentialException -> true
            else -> false
        }
    }

    private fun requestGoogleId(
        filterByAuthorizedAccounts: Boolean,
        autoSelectEnabled: Boolean,
    ): GetCredentialRequest {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(autoSelectEnabled)
            .setServerClientId(serverClientId)
            .build()

        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    private fun requestSiwgButton(): GetCredentialRequest {
        val option = GetSignInWithGoogleOption.Builder(serverClientId = serverClientId)
            .build()

        return GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
    }

    private fun handleCredential(credential: Credential): GoogleAccount {
        if (credential !is CustomCredential) {
            throw GoogleAuthException.UnsupportedCredential(
                message = "Unsupported credential type: ${credential::class.java.name}",
            )
        }

        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw GoogleAuthException.UnsupportedCredential(
                message = "Unsupported custom credential type: ${credential.type}",
            )
        }

        return parseGoogleCredential(credential)
    }

    private fun parseGoogleCredential(credential: CustomCredential): GoogleAccount {
        return try {
            val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val token = tokenCredential.idToken

            if (token.isBlank()) {
                throw GoogleAuthException.TokenParseFailed(
                    message = "Google token is empty",
                )
            }

            GoogleAccount(
                token = token,
                displayName = tokenCredential.displayName.orEmpty(),
                profileImageUrl = tokenCredential.profilePictureUri?.toString(),
            )
        } catch (error: GoogleIdTokenParsingException) {
            throw GoogleAuthException.TokenParseFailed(
                message = "Unable to parse Google token credential",
                cause = error,
            )
        } catch (error: GoogleAuthException) {
            throw error
        } catch (error: Throwable) {
            throw GoogleAuthException.TokenParseFailed(
                message = "Unexpected error while parsing Google credential",
                cause = error,
            )
        }
    }

    private fun Throwable.asGoogleAuthException(): GoogleAuthException {
        if (this is GoogleAuthException) return this

        return when (this) {
            is NoCredentialException -> GoogleAuthException.NoCredential(
                message = "No available Google credentials on this device",
                cause = this,
            )

            is GetCredentialCancellationException -> {
                val msg = (message ?: "").lowercase()
                val isReauth = msg.contains("reauth")
                val isUserCancel =
                    msg.contains("canceled") || msg.contains("cancelled") || msg.contains("user canceled")

                when {
                    isReauth -> GoogleAuthException.ReauthFailed(
                        message = "Google account re-auth failed on device",
                        cause = this,
                    )

                    isUserCancel -> GoogleAuthException.Cancelled(
                        message = "Google sign-in cancelled",
                        cause = this,
                    )

                    else -> GoogleAuthException.Cancelled(
                        message = "Google sign-in interrupted/cancelled",
                        cause = this,
                    )
                }
            }

            is GetCredentialInterruptedException -> GoogleAuthException.Interrupted(
                message = "Google sign-in interrupted",
                cause = this,
            )

            is GetCredentialProviderConfigurationException -> GoogleAuthException.ProviderMisconfigured(
                message = "Credential provider misconfigured or unavailable",
                cause = this,
            )

            is GetCredentialException -> GoogleAuthException.CredentialManagerFailed(
                message = "Unable to retrieve Google credential",
                cause = this,
            )

            else -> GoogleAuthException.Unknown(
                message = "Google sign-in failed",
                cause = this,
            )
        }
    }
}