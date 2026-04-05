import FirebaseMessaging
import Foundation
import shared

/// Implements the KMP `FirebaseMessagingProvider` interface using the native Firebase Messaging SDK.
/// The FCM token is available only after APNs registration is complete (handled in AppDelegate).
final class IosFirebaseMessaging: FirebaseMessagingProvider {

    func getToken(completionHandler: @escaping (String?, Error?) -> Void) {
        Messaging.messaging().token { token, error in
            if let token {
                completionHandler(token, nil)
                return
            }

            // On iOS Firebase may be asked for the FCM token before APNs registration finishes.
            // Treat that as "token is not ready yet" and let MessagingDelegate deliver it later.
            _ = error
            completionHandler(nil, nil)
        }
    }
}
