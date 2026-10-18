package com.grippo.services.google.auth

import com.grippo.toolkit.context.NativeContext
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorCodeCanceledLogin
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorDomain
import platform.Foundation.NSBundle
import platform.Foundation.NSError
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSHTTPCookieStorage
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val AUTH_HOST = "accounts.google.com"
private const val AUTH_PATH = "/o/oauth2/v2/auth"
private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
private val SCOPES = listOf("openid", "profile", "email")
private const val CLIENT_ID_SUFFIX = ".apps.googleusercontent.com"

public actual class GoogleAuthUiProvider internal constructor(
    private val configuration: GoogleAuthConfiguration,
    httpClient: HttpClient,
    json: Json,
    anchorProvider: () -> ASPresentationAnchor? = { currentPresentationAnchor() },
) {
    private val tokenClient = GoogleTokenClient(httpClient, json)
    private val presentationContextProvider = SessionPresentationProvider(anchorProvider)
    private var currentSession: ASWebAuthenticationSession? = null

    @OptIn(ExperimentalForeignApi::class)
    public actual suspend fun signIn(): GoogleAccount {
        return try {
            validateConfiguration(configuration)

            val pkce = GooglePkce.create()
            val state = randomState()

            val authUrl = authorizationUrl(configuration = configuration, pkce = pkce, state = state)
                ?: throw GoogleAuthException.ProviderMisconfigured("Failed to build Google OAuth URL")

            val callback = beginSessionOrThrow(authUrl)

            throwIfOAuthError(callback)

            val authorizationCode = extractAuthorizationCode(callback = callback, expectedState = state)
                ?: throw GoogleAuthException.CredentialManagerFailed(
                    "Missing authorization code in Google OAuth callback",
                )

            tokenClient.exchange(
                authorizationCode = authorizationCode,
                codeVerifier = pkce.verifier,
                configuration = configuration,
            )
        } catch (error: GoogleAuthException) {
            throw error
        } catch (error: Exception) {
            if (error is CancellationException) {
                throw error
            }
            throw GoogleAuthException.Unknown(message = "Google sign-in failed", cause = error)
        }
    }

    private fun validateConfiguration(config: GoogleAuthConfiguration) {
        val clientId = config.iosClientId.trim()
        if (clientId.isEmpty()) {
            throw GoogleAuthException.ProviderMisconfigured("Google auth misconfigured: iosClientId is empty")
        }
        if (!clientId.endsWith(CLIENT_ID_SUFFIX)) {
            throw GoogleAuthException.InvalidServerClientId(
                "Google auth misconfigured: iosClientId must end with .apps.googleusercontent.com",
            )
        }

        val redirectUri = config.redirectUri.trim()
        val redirectScheme = config.redirectScheme.trim()

        if (redirectUri.isEmpty()) {
            throw GoogleAuthException.ProviderMisconfigured("Google auth misconfigured: redirectUri is empty")
        }
        if (redirectScheme.isEmpty()) {
            throw GoogleAuthException.ProviderMisconfigured("Google auth misconfigured: redirectScheme is empty")
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun beginSessionOrThrow(authUrl: NSURL): NSURL =
        suspendCancellableCoroutine { continuation ->
            var createdSession: ASWebAuthenticationSession? = null
            var finished = false

            val complete: (Result<NSURL>) -> Unit = { result ->
                if (!finished) {
                    finished = true
                    continuation.resumeWith(result)
                }
            }

            runOnMain {
                if (!continuation.isActive) {
                    return@runOnMain
                }
                try {
                    val session = ASWebAuthenticationSession(
                        authUrl,
                        configuration.redirectScheme,
                        completionHandler = { url: NSURL?, error: NSError? ->
                            runOnMain {
                                if (currentSession === createdSession) {
                                    currentSession = null
                                }

                                if (error != null) {
                                    complete(Result.failure(asGoogleAuthException(error)))
                                    return@runOnMain
                                }

                                if (url == null) {
                                    complete(
                                        Result.failure(
                                            GoogleAuthException.CredentialManagerFailed(
                                                "Google sign-in session finished without callback URL",
                                            ),
                                        ),
                                    )
                                    return@runOnMain
                                }

                                complete(Result.success(url))
                            }
                        },
                    )

                    session.setPrefersEphemeralWebBrowserSession(true)
                    session.setPresentationContextProvider(presentationContextProvider)

                    currentSession = session
                    createdSession = session

                    val started = session.start()
                    if (!started) {
                        if (currentSession === session) {
                            currentSession = null
                        }
                        complete(
                            Result.failure(
                                GoogleAuthException.CredentialManagerFailed("ASWebAuthenticationSession failed to start"),
                            ),
                        )
                    }
                } catch (error: Throwable) {
                    if (currentSession === createdSession) {
                        currentSession = null
                    }
                    complete(Result.failure(error))
                }
            }

            continuation.invokeOnCancellation {
                runOnMain {
                    val session = createdSession ?: return@runOnMain
                    session.cancel()
                    if (currentSession === session) {
                        currentSession = null
                    }
                }
            }
        }

    private fun asGoogleAuthException(error: NSError): GoogleAuthException {
        val codeInt = error.code.toInt()
        val domainString = error.domain
        val description = error.localizedDescription ?: "NSError(domain=$domainString code=$codeInt)"
        val cause = Throwable("$description (domain=$domainString code=$codeInt)")

        if (domainString == ASWebAuthenticationSessionErrorDomain &&
            codeInt == ASWebAuthenticationSessionErrorCodeCanceledLogin.toInt()
        ) {
            return GoogleAuthException.Cancelled(
                message = "Google sign-in cancelled by user",
                cause = cause,
            )
        }

        val message =
            "ASWebAuthenticationSession failed: $description (domain=$domainString code=$codeInt)"

        return GoogleAuthException.CredentialManagerFailed(message = message, cause = cause)
    }

    private fun throwIfOAuthError(callback: NSURL) {
        val components = NSURLComponents(callback, false)
        val params = queryItems(components.queryItems) + parseFragment(components.percentEncodedFragment)

        val error = params["error"]?.takeIf { it.isNotBlank() } ?: return
        val description = params["error_description"]?.takeIf { it.isNotBlank() }

        val message = if (description != null) {
            "Google OAuth error: $error ($description)"
        } else {
            "Google OAuth error: $error"
        }

        val mapped = when {
            error.equals("access_denied", ignoreCase = true) -> GoogleAuthException.Cancelled(message)
            error.contains("invalid_client", ignoreCase = true) -> GoogleAuthException.ProviderMisconfigured(message)
            else -> GoogleAuthException.CredentialManagerFailed(message)
        }

        throw mapped
    }

    private fun extractAuthorizationCode(callback: NSURL, expectedState: String): String? {
        val components = NSURLComponents(callback, false)
        val params = queryItems(components.queryItems) + parseFragment(components.percentEncodedFragment)

        val state = params["state"] ?: return null
        if (state != expectedState) {
            throw GoogleAuthException.CredentialManagerFailed("Google OAuth state mismatch")
        }

        return params["code"]
    }
}

public actual class GoogleAuthProvider actual constructor(
    @Suppress("UNUSED_PARAMETER") nativeContext: NativeContext,
    private val httpClient: HttpClient,
    private val json: Json,
) {
    public actual val isSupported: Boolean
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("GIDClientID") != null

    public actual fun getUiProvider(context: GoogleAuthUiContext): GoogleAuthUiProvider {
        val config = loadConfiguration()
        return GoogleAuthUiProvider(
            configuration = config,
            httpClient = httpClient,
            json = json,
        )
    }

    public actual suspend fun signOut() {
        clearGoogleCookies()
    }

    private fun loadConfiguration(): GoogleAuthConfiguration {
        val iosClientId = infoValue("GIDClientID")
            ?: throw GoogleAuthException.ProviderMisconfigured("Google auth is not configured on iOS")
        if (!iosClientId.endsWith(CLIENT_ID_SUFFIX)) {
            throw GoogleAuthException.InvalidServerClientId(
                "Google auth misconfigured: iosClientId must end with .apps.googleusercontent.com",
            )
        }

        val redirectUri = infoValue("GIDRedirectURI") ?: defaultRedirectUri(iosClientId)
        val redirectScheme = redirectScheme(redirectUri)

        return GoogleAuthConfiguration(
            iosClientId = iosClientId,
            redirectUri = redirectUri,
            redirectScheme = redirectScheme,
        )
    }

    private fun infoValue(key: String): String? {
        val raw = NSBundle.mainBundle.objectForInfoDictionaryKey(key) ?: return null
        val value = raw as? String ?: throw GoogleAuthException.ProviderMisconfigured(
            "Google auth configuration '$key' must be a string",
        )
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            throw GoogleAuthException.ProviderMisconfigured("Google auth configuration '$key' is empty")
        }
        return trimmed
    }

    private fun defaultRedirectUri(clientId: String): String {
        val identifier = clientId.removeSuffix(CLIENT_ID_SUFFIX)
        return "com.googleusercontent.apps.$identifier:/oauthredirect"
    }

    private fun redirectScheme(uri: String): String {
        val url = NSURL(string = uri)
        return NSURLComponents(url, false).scheme?.takeIf { it.isNotBlank() }
            ?: throw GoogleAuthException.ProviderMisconfigured("Google auth redirect URI must contain a scheme")
    }
}

internal data class GoogleAuthConfiguration(
    val iosClientId: String,
    val redirectUri: String,
    val redirectScheme: String,
)

private class GoogleTokenClient(
    private val client: HttpClient,
    private val json: Json,
) {

    suspend fun exchange(
        authorizationCode: String,
        codeVerifier: String,
        configuration: GoogleAuthConfiguration,
    ): GoogleAccount {
        val parameters = Parameters.build {
            append("grant_type", "authorization_code")
            append("code", authorizationCode)
            append("client_id", configuration.iosClientId)
            append("code_verifier", codeVerifier)
            append("redirect_uri", configuration.redirectUri)
        }

        val responseText = try {
            client.post(TOKEN_URL) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(parameters))
            }.bodyAsText()
        } catch (error: Exception) {
            if (error is CancellationException) {
                throw error
            }
            throw GoogleAuthException.CredentialManagerFailed(
                message = "Failed to exchange Google authorization code",
                cause = error,
            )
        }

        return parseAccount(responseText)
    }

    private fun parseAccount(responseText: String): GoogleAccount {
        return try {
            val payload = json.parseToJsonElement(responseText).jsonObject

            val errorCode = payload["error"]?.jsonPrimitive?.contentOrNull
            if (!errorCode.isNullOrBlank()) {
                val errorDescription = payload["error_description"]?.jsonPrimitive?.contentOrNull
                val message = "Google token endpoint rejected the authorization request"

                val mapped = when {
                    errorCode.contains("invalid_grant", ignoreCase = true) &&
                            (errorDescription?.contains("reauth", ignoreCase = true) == true ||
                                    errorDescription?.contains("re-auth", ignoreCase = true) == true) ->
                        GoogleAuthException.ReauthFailed(message)

                    errorCode.contains("invalid_client", ignoreCase = true) ->
                        GoogleAuthException.ProviderMisconfigured(message)

                    else -> GoogleAuthException.CredentialManagerFailed(message)
                }

                throw mapped
            }

            val tokenValue = payload["id_token"]?.jsonPrimitive
                ?: throw GoogleAuthException.TokenParseFailed("Google token response is missing id_token")
            require(tokenValue.isString) { "Google id_token must be a string" }

            val idToken = tokenValue.contentOrNull?.takeIf { it.isNotBlank() }
                ?: throw GoogleAuthException.TokenParseFailed("Google token response is missing id_token")
            val profile = decodeProfile(idToken)

            GoogleAccount(
                token = idToken,
                displayName = profile.first,
                profileImageUrl = profile.second,
            )
        } catch (error: GoogleAuthException) {
            throw error
        } catch (error: Exception) {
            if (error is CancellationException) {
                throw error
            }
            throw GoogleAuthException.TokenParseFailed(
                message = "Google token endpoint returned unexpected payload",
                cause = error,
            )
        }
    }

    private fun decodeProfile(idToken: String): Pair<String, String?> {
        val payload = decodePayload(idToken)
        val name = profileValue(payload = payload, key = "name").orEmpty()
        val picture = profileValue(payload = payload, key = "picture")
        return name to picture
    }

    private fun profileValue(payload: JsonObject, key: String): String? {
        val value = payload[key]?.jsonPrimitive ?: return null
        val content = value.contentOrNull ?: return null
        require(value.isString) { "Google profile $key must be a string" }
        return content
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodePayload(token: String): JsonObject {
        val segments = token.split('.')
        require(segments.size == 3 && segments.all { it.isNotBlank() }) {
            "Google ID token has invalid JWT structure"
        }

        val payload = padBase64(segments[1])
        val decoded = Base64.UrlSafe.decode(payload)
        val jsonString = decoded.decodeToString(throwOnInvalidSequence = true)
        return json.parseToJsonElement(jsonString).jsonObject
    }

    private fun padBase64(value: String): String {
        val remainder = value.length % 4
        return if (remainder == 0) value else value + "====".substring(remainder)
    }
}

private data class GooglePkce(
    val verifier: String,
    val challenge: String,
    val method: String,
) {
    companion object {
        fun create(): GooglePkce {
            val verifier = randomUrlSafeString()
            val (challenge, method) = pkceChallenge(verifier)
            return GooglePkce(verifier = verifier, challenge = challenge, method = method)
        }
    }
}

private fun authorizationUrl(
    configuration: GoogleAuthConfiguration,
    pkce: GooglePkce,
    state: String,
): NSURL? {
    val components = NSURLComponents()
    components.scheme = "https"
    components.host = AUTH_HOST
    components.path = AUTH_PATH
    components.setQueryItems(
        listOf(
            NSURLQueryItem(name = "client_id", value = configuration.iosClientId),
            NSURLQueryItem(name = "redirect_uri", value = configuration.redirectUri),
            NSURLQueryItem(name = "response_type", value = "code"),
            NSURLQueryItem(name = "scope", value = SCOPES.joinToString(" ")),
            NSURLQueryItem(name = "state", value = state),
            NSURLQueryItem(name = "code_challenge", value = pkce.challenge),
            NSURLQueryItem(name = "code_challenge_method", value = pkce.method),
            NSURLQueryItem(name = "prompt", value = "select_account"),
            NSURLQueryItem(name = "access_type", value = "offline"),
            NSURLQueryItem(name = "include_granted_scopes", value = "true"),
        ),
    )
    return components.URL
}

private fun queryItems(items: List<*>?): Map<String, String> {
    if (items == null) return emptyMap()
    val result = mutableMapOf<String, String>()
    items.forEach { entry ->
        val item = entry as? NSURLQueryItem ?: return@forEach
        val name = item.name
        val value = item.value ?: ""
        result[name] = value
    }
    return result
}

private fun parseFragment(fragment: String?): Map<String, String> {
    if (fragment.isNullOrBlank()) return emptyMap()
    val parser = NSURLComponents()
    parser.setPercentEncodedQuery(fragment)
    return queryItems(parser.queryItems)
}

private fun randomState(): String = randomUrlSafeString()

@OptIn(ExperimentalEncodingApi::class)
private fun pkceChallenge(verifier: String): Pair<String, String> {
    val hash = Sha256.digest(verifier.encodeToByteArray())
    return encodeToUrlSafe(hash) to "S256"
}

@OptIn(ExperimentalEncodingApi::class, ExperimentalForeignApi::class)
private fun randomUrlSafeString(): String {
    return encodeToUrlSafe(secureRandomBytes(64))
}

@OptIn(ExperimentalForeignApi::class)
private fun secureRandomBytes(count: Int): ByteArray {
    val data = ByteArray(count)
    val status = data.usePinned {
        SecRandomCopyBytes(kSecRandomDefault, count.toULong(), it.addressOf(0))
    }
    if (status != errSecSuccess) {
        throw GoogleAuthException.CredentialManagerFailed("Unable to generate secure bytes for Google sign-in")
    }
    return data
}

@OptIn(ExperimentalEncodingApi::class)
private fun encodeToUrlSafe(bytes: ByteArray): String {
    return Base64.UrlSafe.encode(bytes).replace("=", "")
}

private object Sha256 {
    private val initialHash = uintArrayOf(
        0x6a09e667u,
        0xbb67ae85u,
        0x3c6ef372u,
        0xa54ff53au,
        0x510e527fu,
        0x9b05688cu,
        0x1f83d9abu,
        0x5be0cd19u,
    )

    private val roundConstants = uintArrayOf(
        0x428a2f98u, 0x71374491u, 0xb5c0fbcfu, 0xe9b5dba5u, 0x3956c25bu, 0x59f111f1u, 0x923f82a4u, 0xab1c5ed5u,
        0xd807aa98u, 0x12835b01u, 0x243185beu, 0x550c7dc3u, 0x72be5d74u, 0x80deb1feu, 0x9bdc06a7u, 0xc19bf174u,
        0xe49b69c1u, 0xefbe4786u, 0x0fc19dc6u, 0x240ca1ccu, 0x2de92c6fu, 0x4a7484aau, 0x5cb0a9dcu, 0x76f988dau,
        0x983e5152u, 0xa831c66du, 0xb00327c8u, 0xbf597fc7u, 0xc6e00bf3u, 0xd5a79147u, 0x06ca6351u, 0x14292967u,
        0x27b70a85u, 0x2e1b2138u, 0x4d2c6dfcu, 0x53380d13u, 0x650a7354u, 0x766a0abbu, 0x81c2c92eu, 0x92722c85u,
        0xa2bfe8a1u, 0xa81a664bu, 0xc24b8b70u, 0xc76c51a3u, 0xd192e819u, 0xd6990624u, 0xf40e3585u, 0x106aa070u,
        0x19a4c116u, 0x1e376c08u, 0x2748774cu, 0x34b0bcb5u, 0x391c0cb3u, 0x4ed8aa4au, 0x5b9cca4fu, 0x682e6ff3u,
        0x748f82eeu, 0x78a5636fu, 0x84c87814u, 0x8cc70208u, 0x90befffau, 0xa4506cebu, 0xbef9a3f7u, 0xc67178f2u,
    )

    fun digest(input: ByteArray): ByteArray {
        val message = padMessage(input)
        val hash = initialHash.copyOf()
        val words = UIntArray(64)

        var offset = 0
        while (offset < message.size) {
            var i = 0
            while (i < 16) {
                val index = offset + i * 4
                words[i] = readUIntBigEndian(message, index)
                i++
            }
            i = 16
            while (i < 64) {
                val s0 = smallSigma0(words[i - 15])
                val s1 = smallSigma1(words[i - 2])
                words[i] = words[i - 16] + s0 + words[i - 7] + s1
                i++
            }

            var a = hash[0]
            var b = hash[1]
            var c = hash[2]
            var d = hash[3]
            var e = hash[4]
            var f = hash[5]
            var g = hash[6]
            var h = hash[7]

            i = 0
            while (i < 64) {
                val t1 = h + bigSigma1(e) + ch(e, f, g) + roundConstants[i] + words[i]
                val t2 = bigSigma0(a) + maj(a, b, c)
                h = g
                g = f
                f = e
                e = d + t1
                d = c
                c = b
                b = a
                a = t1 + t2
                i++
            }

            hash[0] += a
            hash[1] += b
            hash[2] += c
            hash[3] += d
            hash[4] += e
            hash[5] += f
            hash[6] += g
            hash[7] += h

            offset += 64
        }

        val out = ByteArray(32)
        for (index in hash.indices) {
            writeUIntBigEndian(hash[index], out, index * 4)
        }
        return out
    }

    private fun padMessage(input: ByteArray): ByteArray {
        val bitLength = input.size.toLong() * 8L
        val paddingSize = ((56 - (input.size + 1) % 64) + 64) % 64
        val totalSize = input.size + 1 + paddingSize + 8
        val output = ByteArray(totalSize)
        input.copyInto(output)
        output[input.size] = 0x80.toByte()
        writeLongBigEndian(bitLength, output, totalSize - 8)
        return output
    }

    private fun readUIntBigEndian(bytes: ByteArray, offset: Int): UInt {
        return ((bytes[offset].toUInt() and 0xffu) shl 24) or
            ((bytes[offset + 1].toUInt() and 0xffu) shl 16) or
            ((bytes[offset + 2].toUInt() and 0xffu) shl 8) or
            (bytes[offset + 3].toUInt() and 0xffu)
    }

    private fun writeUIntBigEndian(value: UInt, output: ByteArray, offset: Int) {
        output[offset] = (value shr 24).toByte()
        output[offset + 1] = (value shr 16).toByte()
        output[offset + 2] = (value shr 8).toByte()
        output[offset + 3] = value.toByte()
    }

    private fun writeLongBigEndian(value: Long, output: ByteArray, offset: Int) {
        output[offset] = (value ushr 56).toByte()
        output[offset + 1] = (value ushr 48).toByte()
        output[offset + 2] = (value ushr 40).toByte()
        output[offset + 3] = (value ushr 32).toByte()
        output[offset + 4] = (value ushr 24).toByte()
        output[offset + 5] = (value ushr 16).toByte()
        output[offset + 6] = (value ushr 8).toByte()
        output[offset + 7] = value.toByte()
    }

    private fun rotr(value: UInt, bits: Int): UInt {
        return (value shr bits) or (value shl (32 - bits))
    }

    private fun ch(x: UInt, y: UInt, z: UInt): UInt = (x and y) xor (x.inv() and z)
    private fun maj(x: UInt, y: UInt, z: UInt): UInt = (x and y) xor (x and z) xor (y and z)
    private fun bigSigma0(x: UInt): UInt = rotr(x, 2) xor rotr(x, 13) xor rotr(x, 22)
    private fun bigSigma1(x: UInt): UInt = rotr(x, 6) xor rotr(x, 11) xor rotr(x, 25)
    private fun smallSigma0(x: UInt): UInt = rotr(x, 7) xor rotr(x, 18) xor (x shr 3)
    private fun smallSigma1(x: UInt): UInt = rotr(x, 17) xor rotr(x, 19) xor (x shr 10)
}

private fun clearGoogleCookies() {
    val storage = NSHTTPCookieStorage.sharedHTTPCookieStorage
    val cookies = storage.cookies ?: return
    cookies.forEach { element ->
        val cookie = element as? NSHTTPCookie ?: return@forEach
        val domain = cookie.domain
        if (domain.contains("google")) {
            storage.deleteCookie(cookie)
        }
    }
}

private fun runOnMain(block: () -> Unit) {
    if (NSThread.isMainThread()) {
        block()
    } else {
        dispatch_async(dispatch_get_main_queue(), block)
    }
}

private class SessionPresentationProvider(
    private val anchorProvider: () -> ASPresentationAnchor?,
) : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): ASPresentationAnchor {
        return anchorProvider() ?: run {
            val application = UIApplication.sharedApplication
            application.keyWindow ?: UIWindow()
        }
    }
}

private fun currentPresentationAnchor(): ASPresentationAnchor? {
    val application = UIApplication.sharedApplication
    application.keyWindow?.let { return it }

    val windowList = application.windows
    val typedWindows = windowList.mapNotNull { it as? UIWindow }
    val keyWindow = typedWindows.firstOrNull { it.isKeyWindow() }
    if (keyWindow != null) return keyWindow

    return typedWindows.firstOrNull()
}
