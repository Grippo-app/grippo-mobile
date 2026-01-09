import shared

final class IosGrippoAnalyticsLogger: FirebaseAnalyticsProvider {

    func logEvent(
        name: String,
        params: [String: String]
    ) {
        Analytics.logEvent(name, parameters: params)
    }
}
