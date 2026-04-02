import FirebaseMessaging
import shared

/// Handles Firebase Messaging token lifecycle events.
extension AppDelegate: MessagingDelegate {

    /// Called whenever Firebase rotates the FCM registration token.
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        MessagingEventBridge.shared.onTokenRefresh(token: fcmToken)
    }
}
