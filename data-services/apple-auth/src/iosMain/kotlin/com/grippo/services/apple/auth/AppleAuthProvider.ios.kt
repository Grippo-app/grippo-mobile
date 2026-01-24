package com.grippo.services.apple.auth

import com.grippo.toolkit.context.NativeContext
import io.ktor.client.HttpClient
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationErrorCanceled
import platform.AuthenticationServices.ASAuthorizationErrorDomain
import platform.AuthenticationServices.ASPresentationAnchor
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSThread
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

public actual class AppleAuthUiProvider internal constructor(
    private val anchorProvider: () -> ASPresentationAnchor? = { currentPresentationAnchor() },
) {
    private var currentController: ASAuthorizationController? = null
    private var currentDelegate: AppleAuthorizationDelegate? = null

    public actual suspend fun signIn(): Result<AppleAccount> {
        return runCatching { beginSignIn() }
    }

    private suspend fun beginSignIn(): AppleAccount =
        suspendCancellableCoroutine { continuation ->
            var completed = false
            fun complete(result: Result<AppleAccount>) {
                if (completed) return
                completed = true
                if (!continuation.isActive) return
                result.fold(
                    onSuccess = continuation::resume,
                    onFailure = continuation::resumeWithException,
                )
            }

            runOnMain {
                if (currentController != null) {
                    complete(Result.failure(AppleAuthException("Apple sign-in is already in progress")))
                    return@runOnMain
                }

                val request = ASAuthorizationAppleIDProvider().createRequest()
                val controller = ASAuthorizationController(listOf(request))
                val delegate = AppleAuthorizationDelegate(
                    anchorProvider = anchorProvider,
                    onSuccess = { account ->
                        runOnMain {
                            if (currentController === controller) {
                                currentController = null
                                currentDelegate = null
                            }
                            complete(Result.success(account))
                        }
                    },
                    onFailure = { error ->
                        runOnMain {
                            if (currentController === controller) {
                                currentController = null
                                currentDelegate = null
                            }
                            complete(Result.failure(error))
                        }
                    },
                )

                currentController = controller
                currentDelegate = delegate

                controller.delegate = delegate
                controller.presentationContextProvider = delegate
                controller.performRequests()
            }
            continuation.invokeOnCancellation {
                runOnMain {
                    currentController = null
                    currentDelegate = null
                }
            }
        }
}

public actual class AppleAuthProvider actual constructor(
    @Suppress("UNUSED_PARAMETER") nativeContext: NativeContext,
    @Suppress("UNUSED_PARAMETER") httpClient: HttpClient,
    @Suppress("UNUSED_PARAMETER") json: Json,
) {
    public actual val isSupported: Boolean
        get() = true

    public actual fun getUiProvider(context: AppleAuthUiContext): AppleAuthUiProvider {
        return AppleAuthUiProvider()
    }

    public actual suspend fun signOut() {
        return
    }
}

private class AppleAuthorizationDelegate(
    private val anchorProvider: () -> ASPresentationAnchor?,
    private val onSuccess: (AppleAccount) -> Unit,
    private val onFailure: (AppleAuthException) -> Unit,
) : NSObject(),
    ASAuthorizationControllerDelegateProtocol,
    ASAuthorizationControllerPresentationContextProvidingProtocol {

    override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor {
        return anchorProvider() ?: run {
            val application = UIApplication.sharedApplication
            application.keyWindow ?: UIWindow()
        }
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization,
    ) {
        runCatching {
            val credential =
                didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
                    ?: throw AppleAuthException("Unsupported Apple credential")

            val authorizationCode = credential.authorizationCode?.toUtf8String()
                ?: throw AppleAuthException("Apple authorization code is missing")

            AppleAccount(
                authorizationCode = authorizationCode,
            )
        }.fold(onSuccess = onSuccess, onFailure = { error ->
            onFailure(error.asAppleAuthException())
        })
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError,
    ) {
        if (didCompleteWithError.domain == ASAuthorizationErrorDomain &&
            didCompleteWithError.code == ASAuthorizationErrorCanceled.toLong()
        ) {
            onFailure(AppleAuthException("Apple sign-in was cancelled"))
            return
        }

        val description = didCompleteWithError.localizedDescription ?: "Apple sign-in failed"
        onFailure(AppleAuthException(description))
    }

}

private fun Throwable.asAppleAuthException(): AppleAuthException {
    return this as? AppleAuthException ?: AppleAuthException(
        message ?: "Apple sign-in failed",
        this
    )
}

@OptIn(BetaInteropApi::class)
private fun platform.Foundation.NSData.toUtf8String(): String? {
    return NSString.create(data = this, encoding = NSUTF8StringEncoding)?.toString()
}

private fun runOnMain(block: () -> Unit) {
    if (NSThread.isMainThread()) {
        block()
    } else {
        dispatch_async(dispatch_get_main_queue(), block)
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
