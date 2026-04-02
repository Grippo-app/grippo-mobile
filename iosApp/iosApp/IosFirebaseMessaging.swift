import FirebaseMessaging
import Foundation
import shared

/// Implements the KMP `FirebaseMessagingProvider` interface using the native Firebase Messaging SDK.
/// The FCM token is available only after APNs registration is complete (handled in AppDelegate).
final class IosFirebaseMessaging: FirebaseMessagingProvider {

    func getToken(completionHandler: @escaping (String?, Error?) -> Void) {
        Messaging.messaging().token { token, error in
            completionHandler(token, error)
        }
    }
}
