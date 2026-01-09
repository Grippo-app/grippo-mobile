import Foundation
import shared
import FirebaseAnalytics

final class IosFirebaseAnalytics: FirebaseAnalyticsProvider {

    func logEvent(name: String, params: [String: String]) {
        Analytics.logEvent(name, parameters: params)
    }
}
