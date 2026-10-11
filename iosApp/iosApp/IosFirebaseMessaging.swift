import FirebaseMessaging
import Foundation
import shared

final class IosFirebaseMessaging: MessagingSink {
    func getToken(completionHandler: @escaping (PushTokenResult?, Error?) -> Void) {
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
