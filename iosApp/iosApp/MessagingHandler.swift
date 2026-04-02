import FirebaseMessaging

/// Handles Firebase Messaging token lifecycle events.
extension AppDelegate: MessagingDelegate {

    /// Called whenever Firebase rotates the FCM registration token.
    /// Backend registration happens after a successful login/register call
    /// in AuthorizationRepositoryImpl — no forwarding needed here.
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {}
}
