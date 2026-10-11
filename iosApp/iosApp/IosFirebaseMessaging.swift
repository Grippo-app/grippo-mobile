import FirebaseMessaging
import Foundation
import shared

final class IosFirebaseMessaging: MessagingSink {
    func getToken(completionHandler: @escaping (PushTokenResult?, Error?) -> Void) {
        if Thread.isMainThread {
            fetchToken(completionHandler: completionHandler)
        } else {
            DispatchQueue.main.async {
                self.fetchToken(completionHandler: completionHandler)
            }
        }
    }

    // AppDelegate updates Firebase's APNs token on the main thread.
    private func fetchToken(completionHandler: @escaping (PushTokenResult?, Error?) -> Void) {
        guard FirebaseMessaging.Messaging.messaging().apnsToken != nil else {
            completionHandler(PushTokenResultUnavailable(reason: .platformnotready), nil)
            return
        }
        FirebaseMessaging.Messaging.messaging().token { token, error in
            if error != nil {
                completionHandler(PushTokenResultFailed(reason: .sdkfailure), nil)
            } else if let token {
                completionHandler(PushTokenDecoder.shared.decode(value: token), nil)
            } else {
                completionHandler(PushTokenResultFailed(reason: .invalidtoken), nil)
            }
        }
    }
}
