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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
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
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min
import kotlin.random.Random

private const val AUTH_HOST = "accounts.google.com"
private const val AUTH_PATH = "/o/oauth2/v2/auth"
private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
private val SCOPES = listOf("openid", "profile", "email")

public actual class GoogleAuthUiProvider internal constructor(
    private val configuration: GoogleAuthConfiguration,
    httpClient: HttpClient,
    json: Json,
    private val anchorProvider: () -> ASPresentationAnchor? = { currentPresentationAnchor() },
) {
    private val tokenClient = GoogleTokenClient(httpClient, json)
    private var currentSession: ASWebAuthenticationSession? = null

    @OptIn(ExperimentalForeignApi::class)
    public actual suspend fun signIn(): GoogleAccount? {
        val pkce = GooglePkce.create()
        val state = randomState()
        val authUrl = authorizationUrl(configuration, pkce, state) ?: return null
        val callback = beginSession(authUrl) ?: return null
        val authorizationCode = callback.extractAuthorizationCode(state) ?: return null
        return tokenClient.exchange(authorizationCode, pkce.verifier, configuration)
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun beginSession(authUrl: NSURL): NSURL? =
        suspendCancellableCoroutine { continuation ->
            var createdSession: ASWebAuthenticationSession? = null
            runOnMain {
                val session = ASWebAuthenticationSession(
                    authUrl,
                    configuration.redirectScheme,
                    completionHandler = { url: NSURL?, error: NSError? ->
                        runOnMain {
                            if (currentSession === createdSession) {
                                currentSession = null
                            }
                            continuation.resume(if (error != null) null else url)
                        }
                    },
                )
                session.setPrefersEphemeralWebBrowserSession(true)
                session.setPresentationContextProvider(SessionPresentationProvider(anchorProvider))
                currentSession = session
                createdSession = session
                session.start()
            }
            continuation.invokeOnCancellation {
                val session = createdSession
                if (session != null) {
                    runOnMain {
                        session.cancel()
                        if (currentSession === session) {
                            currentSession = null
                        }
                    }
                }
            }
        }
}

public actual class GoogleAuthProvider actual constructor(
    @Suppress("UNUSED_PARAMETER") nativeContext: NativeContext,
    private val httpClient: HttpClient,
    private val json: Json,
) {
    private val configuration: GoogleAuthConfiguration? = loadConfiguration()

    public actual val isSupported: Boolean
        get() = configuration != null

    public actual fun getUiProvider(context: GoogleAuthUiContext): GoogleAuthUiProvider {
        val config = requireNotNull(configuration) {
            "Google auth is not configured on iOS"
        }
        return GoogleAuthUiProvider(config, httpClient, json)
    }

    public actual suspend fun signOut() {
        clearGoogleCookies()
    }

    private fun loadConfiguration(): GoogleAuthConfiguration? {
        val iosClientId = infoValue("GIDClientID") ?: return null
        infoValue("GIDServerClientID") ?: return null
        val redirectUri =
            infoValue("GIDRedirectURI") ?: defaultRedirectUri(iosClientId) ?: return null
        val redirectScheme = redirectScheme(redirectUri) ?: return null
        return GoogleAuthConfiguration(
            iosClientId = iosClientId,
            redirectUri = redirectUri,
            redirectScheme = redirectScheme,
        )
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
    ): GoogleAccount? {
        val parameters = Parameters.build {
            append("grant_type", "authorization_code")
            append("code", authorizationCode)
            append("client_id", configuration.iosClientId)
            append("code_verifier", codeVerifier)
            append("redirect_uri", configuration.redirectUri)
        }

        val responseText = runCatching {
            client.post(TOKEN_URL) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(parameters))
            }.bodyAsText()
        }.getOrNull() ?: return null

        val payload = runCatching { json.parseToJsonElement(responseText).jsonObject }.getOrNull()
            ?: return null
        val idToken = payload["id_token"]?.jsonPrimitive?.contentOrNull ?: return null
        val profile = decodeProfile(json, idToken)
        return GoogleAccount(
            token = idToken,
            displayName = profile?.first.orEmpty(),
            profileImageUrl = profile?.second,
        )
    }
}

private data class GooglePkce(
    val verifier: String,
    val challenge: String,
) {
    companion object {
        fun create(): GooglePkce {
            val verifier = randomUrlSafeString(64)
            return GooglePkce(verifier = verifier, challenge = verifier)
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
            NSURLQueryItem(name = "code_challenge_method", value = "plain"),
            NSURLQueryItem(name = "prompt", value = "select_account"),
            NSURLQueryItem(name = "access_type", value = "offline"),
            NSURLQueryItem(name = "include_granted_scopes", value = "true"),
        )
    )
    return components.URL
}

private fun NSURL.extractAuthorizationCode(expectedState: String): String? {
    val components = NSURLComponents(this, false)
    val params =
        queryItems(components.queryItems) + parseFragment(components.percentEncodedFragment)
    val state = params["state"] ?: return null
    if (state != expectedState) return null
    return params["code"]
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

private fun randomState(): String = randomUrlSafeString(32)

@OptIn(ExperimentalEncodingApi::class, ExperimentalForeignApi::class)
private fun randomUrlSafeString(minLength: Int): String {
    repeat(6) {
        val candidate = encodeToUrlSafe(secureRandomBytes(64)).ifEmpty {
            Base64.UrlSafe.encode(ByteArray(64)).replace("=", "")
        }
        if (candidate.length >= minLength) {
            return candidate.take(min(candidate.length, 128))
        }
    }
    val fallback = encodeToUrlSafe(secureRandomBytes(128)).ifEmpty {
        encodeToUrlSafe(Random.nextBytes(128))
    }
    return fallback.take(min(fallback.length, 128))
}

@OptIn(ExperimentalForeignApi::class)
private fun secureRandomBytes(count: Int): ByteArray {
    val data = ByteArray(count)
    val status = data.usePinned {
        SecRandomCopyBytes(kSecRandomDefault, count.toULong(), it.addressOf(0))
    }
    return if (status == errSecSuccess) data else Random.nextBytes(count)
}

@OptIn(ExperimentalEncodingApi::class)
private fun encodeToUrlSafe(bytes: ByteArray): String {
    return Base64.UrlSafe.encode(bytes).replace("=", "")
}

private fun decodeProfile(json: Json, idToken: String): Pair<String, String?>? {
    val payload = decodePayload(json, idToken) ?: return null
    val name = payload["name"]?.jsonPrimitive?.contentOrNull ?: ""
    val picture = payload["picture"]?.jsonPrimitive?.contentOrNull
    return name to picture
}

@OptIn(ExperimentalEncodingApi::class)
private fun decodePayload(json: Json, token: String): JsonObject? {
    val segments = token.split('.')
    if (segments.size < 2) return null
    val payload = padBase64(segments[1])
    val decoded = runCatching { Base64.UrlSafe.decode(payload) }.getOrNull() ?: return null
    val jsonString = runCatching { decoded.decodeToString() }.getOrNull() ?: return null
    return runCatching { json.parseToJsonElement(jsonString).jsonObject }.getOrNull()
}

private fun padBase64(value: String): String {
    val remainder = value.length % 4
    return if (remainder == 0) value else value + "====".substring(remainder)
}

private fun infoValue(key: String): String? {
    val raw = NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String ?: return null
    val trimmed = raw.trim()
    return trimmed.takeIf { it.isNotEmpty() }
}

private fun defaultRedirectUri(clientId: String): String? {
    val suffix = ".apps.googleusercontent.com"
    if (!clientId.endsWith(suffix)) return null
    val identifier = clientId.removeSuffix(suffix)
    return "com.googleusercontent.apps.$identifier:/oauthredirect"
}

private fun redirectScheme(uri: String): String? {
    val url = NSURL(string = uri)
    return NSURLComponents(url, false).scheme
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
