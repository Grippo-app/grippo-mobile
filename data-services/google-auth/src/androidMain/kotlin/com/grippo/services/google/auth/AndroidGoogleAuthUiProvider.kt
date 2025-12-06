package com.grippo.services.google.auth

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
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
            val response = credentialManager.getCredential(
                context = activityContext,
                request = credentialRequest(),
            )
            handleCredential(response.credential)
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error -> Result.failure(error.asGoogleAuthException()) },
        )
    }

    private fun Throwable.asGoogleAuthException(): GoogleAuthException {
        if (this is GoogleAuthException) return this
        return when (this) {
            is GetCredentialException ->
                GoogleAuthException("Unable to retrieve Google credential", this)

            else -> GoogleAuthException("Google sign-in failed", this)
        }
    }

    private fun credentialRequest(): GetCredentialRequest {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(true)
            .setServerClientId(serverClientId)
            .build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    private fun handleCredential(credential: Credential): GoogleAccount {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return parseGoogleCredential(credential)
        }
        throw GoogleAuthException("Unsupported credential type ${credential::class.simpleName}")
    }

    private fun parseGoogleCredential(credential: CustomCredential): GoogleAccount {
        return try {
            val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleAccount(
                token = tokenCredential.idToken,
                displayName = tokenCredential.displayName.orEmpty(),
                profileImageUrl = tokenCredential.profilePictureUri?.toString(),
            )
        } catch (error: GoogleIdTokenParsingException) {
            throw GoogleAuthException("Unable to parse Google token credential", error)
        }
    }
}
