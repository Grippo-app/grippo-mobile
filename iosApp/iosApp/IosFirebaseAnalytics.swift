import Foundation
import shared
import FirebaseAnalytics

final class IosFirebaseAnalytics: AnalyticsSink {

    func send(payload: AnalyticsPayload) {
        var parameters: [String: Any] = [:]
        for (key, value) in payload.parameters {
            switch value {
            case let text as AnalyticsValue.Text:
                parameters[key] = text.value
            case let integer as AnalyticsValue.Integer:
                parameters[key] = NSNumber(value: integer.value)
            case let decimal as AnalyticsValue.Decimal:
                parameters[key] = NSNumber(value: decimal.value)
            default:
                preconditionFailure("Unsupported analytics value")
            }
        }
        Analytics.logEvent(payload.name, parameters: parameters)
    }

    func setCollection(collection: AnalyticsCollection) {
        let enabled = collection == .granted
        if !enabled { Analytics.setAnalyticsCollectionEnabled(false) }
        Analytics.setConsent([
            .analyticsStorage: enabled ? .granted : .denied,
            .adStorage: .denied,
            .adUserData: .denied,
            .adPersonalization: .denied,
        ])
        if enabled { Analytics.setAnalyticsCollectionEnabled(true) }
    }

    func setIdentity(identity: AnalyticsIdentityPayload) {
        Analytics.setUserID(identity.userId?.value)
        for name in identity.clearedPropertyNames { Analytics.setUserProperty(nil, forName: name) }
        for (name, value) in identity.properties { Analytics.setUserProperty(value, forName: name) }
    }
}
