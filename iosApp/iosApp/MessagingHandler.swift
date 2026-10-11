import FirebaseMessaging
import shared

/// Handles Firebase Messaging token lifecycle events.
extension AppDelegate: MessagingDelegate {

    /// Called whenever Firebase rotates the FCM registration token.
    func messaging(_ messaging: FirebaseMessaging.Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        bootstrap.messaging.onTokenChanged(rawToken: fcmToken)
    }
}
