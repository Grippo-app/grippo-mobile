import FirebaseMessaging
import Foundation
import shared

final class IosFirebaseMessaging: PushTokenProvider {
    func fetchToken(completionHandler: @escaping (PushTokenState?, Error?) -> Void) {
        if Thread.isMainThread {
            fetchFromFirebase(completionHandler: completionHandler)
        } else {
            DispatchQueue.main.async {
                self.fetchFromFirebase(completionHandler: completionHandler)
            }
        }
    }

    // AppDelegate updates Firebase's APNs token on the main thread.
    private func fetchFromFirebase(completionHandler: @escaping (PushTokenState?, Error?) -> Void) {
        guard FirebaseMessaging.Messaging.messaging().apnsToken != nil else {
            completionHandler(PushTokenStateUnavailable(reason: .platformnotready), nil)
            return
        }
        FirebaseMessaging.Messaging.messaging().token { token, error in
            if error != nil {
                completionHandler(PushTokenStateFailed(reason: .providerfailure), nil)
            } else if let token {
                completionHandler(PushTokenParser.shared.parse(rawToken: token), nil)
            } else {
                completionHandler(PushTokenStateFailed(reason: .invalidtoken), nil)
            }
        }
    }
}
