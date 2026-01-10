import Foundation
import shared
import FirebaseCrashlytics

final class IosFirebaseCrashlytics: FirebaseCrashlyticsProvider {

    func log(message: String) {
        Crashlytics.crashlytics().log(message)
    }

    func recordException(
        throwable: KotlinThrowable,
        metadata: [String: String]
    ) {
        let crashlytics = Crashlytics.crashlytics()

        metadata.forEach { key, value in
            crashlytics.setCustomValue(value, forKey: key)
        }

        let stackTraceArray = throwable.getStackTrace()

        var stackTraceLines: [String] = []
        stackTraceLines.reserveCapacity(Int(stackTraceArray.size))

        for i in 0..<stackTraceArray.size {
            if let element = stackTraceArray.get(index: i) {
                stackTraceLines.append(String(describing: element))
            }
        }

        let stackTrace = stackTraceLines.joined(separator: "\n")

        let description =
            throwable.message ??
                String(describing: type(of: throwable))

        let nsError = NSError(
            domain: "KotlinException.\(String(describing: type(of: throwable)))",
            code: 1,
            userInfo: [
                NSLocalizedDescriptionKey: description,
                "kotlin_stacktrace": stackTrace
            ]
        )

        crashlytics.record(error: nsError)
    }
}
