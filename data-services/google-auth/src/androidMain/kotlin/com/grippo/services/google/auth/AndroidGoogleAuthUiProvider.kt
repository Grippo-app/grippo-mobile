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
import kotlinx.coroutines.CancellationException

public actual typealias GoogleAuthUiProvider = AndroidGoogleAuthUiProvider

public class AndroidGoogleAuthUiProvider(
    private val activityContext: Context,
    private val credentialManager: CredentialManager,
    private val serverClientId: String,
) {
    public suspend fun signIn(): GoogleAccount {
        return try {
            requireValidServerClientId(serverClientId)

            val credential = requestCredentialStable()

            handleCredential(credential)
        } catch (error: Exception) {
            if (error is CancellationException) {
                throw error
            }
            throw asGoogleAuthException(error)
        }
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
        val authorizedCredential = try {
            credentialManager.getCredential(
                context = activityContext,
                request = requestGoogleId(
                    filterByAuthorizedAccounts = true,
                    autoSelectEnabled = true,
                ),
            ).credential
        } catch (error: GetCredentialException) {
            if (!shouldFallbackToNextStrategy(error)) {
                throw error
            }
            null
        }

        if (authorizedCredential != null) {
            return authorizedCredential
        }

        val availableCredential = try {
            credentialManager.getCredential(
                context = activityContext,
                request = requestGoogleId(
                    filterByAuthorizedAccounts = false,
                    autoSelectEnabled = false,
                ),
            ).credential
        } catch (error: GetCredentialException) {
            if (!shouldFallbackToNextStrategy(error)) {
                throw error
            }
            null
        }

        if (availableCredential != null) {
            return availableCredential
        }

        val signInCredential = credentialManager.getCredential(
            context = activityContext,
            request = requestSiwgButton(),
        )

        return signInCredential.credential
    }

    private fun shouldFallbackToNextStrategy(error: Throwable): Boolean {
        return when (error) {
            is NoCredentialException -> true
            is GetCredentialInterruptedException -> true

            is GetCredentialCancellationException -> false

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
        } catch (error: Exception) {
            if (error is CancellationException) {
                throw error
            }
            throw GoogleAuthException.TokenParseFailed(
                message = "Unexpected error while parsing Google credential",
                cause = error,
            )
        }
    }

    private fun asGoogleAuthException(error: Exception): GoogleAuthException {
        if (error is GoogleAuthException) {
            return error
        }

        return when (error) {
            is NoCredentialException -> GoogleAuthException.NoCredential(
                message = "No available Google credentials on this device",
                cause = error,
            )

            is GetCredentialCancellationException -> GoogleAuthException.Cancelled(
                message = "Google sign-in cancelled",
                cause = error,
            )

            is GetCredentialInterruptedException -> GoogleAuthException.Interrupted(
                message = "Google sign-in interrupted",
                cause = error,
            )

            is GetCredentialProviderConfigurationException -> GoogleAuthException.ProviderMisconfigured(
                message = "Credential provider misconfigured or unavailable",
                cause = error,
            )

            is GetCredentialException -> GoogleAuthException.CredentialManagerFailed(
                message = "Unable to retrieve Google credential",
                cause = error,
            )

            else -> GoogleAuthException.Unknown(
                message = "Google sign-in failed",
                cause = error,
            )
        }
    }
}
