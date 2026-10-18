package com.grippo.services.apple.auth

import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationErrorCanceled
import platform.AuthenticationServices.ASAuthorizationErrorDomain
import platform.AuthenticationServices.ASPresentationAnchor
import platform.Foundation.NSData
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

public actual class AppleAuthUiProvider internal constructor(
    private val anchorProvider: () -> ASPresentationAnchor? = { currentPresentationAnchor() },
) {
    private var currentController: ASAuthorizationController? = null
    private var currentDelegate: AppleAuthorizationDelegate? = null

    public actual suspend fun signIn(): AppleAccount {
        return beginSignIn()
    }

    private suspend fun beginSignIn(): AppleAccount =
        suspendCancellableCoroutine { continuation ->
            var completed = false
            var createdController: ASAuthorizationController? = null
            val complete: (Result<AppleAccount>) -> Unit = { result ->
                if (!completed) {
                    completed = true
                    if (continuation.isActive) {
                        continuation.resumeWith(result)
                    }
                }
            }

            runOnMain {
                if (!continuation.isActive) {
                    return@runOnMain
                }
                if (currentController != null) {
                    complete(Result.failure(AppleAuthException("Apple sign-in is already in progress")))
                    return@runOnMain
                }

                try {
                    val request = ASAuthorizationAppleIDProvider().createRequest()
                    request.requestedScopes = listOf(platform.AuthenticationServices.ASAuthorizationScopeEmail)
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
                    createdController = controller

                    controller.delegate = delegate
                    controller.presentationContextProvider = delegate
                    controller.performRequests()
                } catch (error: Throwable) {
                    if (currentController === createdController) {
                        currentController = null
                        currentDelegate = null
                    }
                    complete(Result.failure(error))
                }
            }
            continuation.invokeOnCancellation {
                runOnMain {
                    createdController?.cancel()
                    if (currentController === createdController) {
                        currentController = null
                        currentDelegate = null
                    }
                }
            }
        }
}

public actual class AppleAuthProvider actual constructor() {
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
    private val onFailure: (Throwable) -> Unit,
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
        val account = try {
            parseAccount(didCompleteWithAuthorization)
        } catch (error: Throwable) {
            onFailure(asAppleAuthException(error))
            return
        }

        onSuccess(account)
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError,
    ) {
        val reason = if (
            didCompleteWithError.domain == ASAuthorizationErrorDomain &&
            didCompleteWithError.code == ASAuthorizationErrorCanceled.toLong()
        ) {
            AppleAuthFailureReason.Cancelled
        } else {
            AppleAuthFailureReason.ProviderFailure
        }
        val description = didCompleteWithError.localizedDescription ?: "Apple sign-in failed"

        onFailure(
            AppleAuthException(
                message = "$description (domain=${didCompleteWithError.domain}, code=${didCompleteWithError.code})",
                reason = reason,
            )
        )
    }

    private fun parseAccount(authorization: ASAuthorization): AppleAccount {
        val credential = authorization.credential as? ASAuthorizationAppleIDCredential
            ?: throw AppleAuthException("Unsupported Apple credential")
        val authorizationCode = credential.authorizationCode?.let(::toUtf8String)
            ?.takeIf { it.isNotBlank() }
            ?: throw AppleAuthException("Apple authorization code is missing")

        return AppleAccount(authorizationCode = authorizationCode)
    }

    private fun asAppleAuthException(error: Throwable): Throwable {
        if (error is CancellationException || error !is Exception) {
            return error
        }
        return error as? AppleAuthException ?: AppleAuthException(
            message = error.message ?: "Apple sign-in failed",
            cause = error,
        )
    }

    @OptIn(BetaInteropApi::class)
    private fun toUtf8String(value: NSData): String? {
        return NSString.create(data = value, encoding = NSUTF8StringEncoding)?.toString()
    }
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
