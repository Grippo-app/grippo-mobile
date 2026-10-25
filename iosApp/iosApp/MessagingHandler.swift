import FirebaseMessaging
import shared

extension AppDelegate: MessagingDelegate {

    func messaging(_ messaging: FirebaseMessaging.Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        bootstrap.messaging.onTokenChanged(rawToken: fcmToken)
    }
}
