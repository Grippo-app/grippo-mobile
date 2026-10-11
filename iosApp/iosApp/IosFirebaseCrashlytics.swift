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
        let name = FirebaseProvider.shared.exceptionTypeName(throwable: throwable)
        var reason = throwable.message ?? name
        var origin = throwable
        var visited = Set<ObjectIdentifier>([ObjectIdentifier(throwable)])
        while let cause = origin.cause, visited.insert(ObjectIdentifier(cause)).inserted {
            let causeName = FirebaseProvider.shared.exceptionTypeName(throwable: cause)
            reason += "\nCaused by: \(causeName): \(cause.message ?? "")"
            origin = cause
        }

        // ExceptionModel has no per-event custom-key API. Keep metadata with this event,
        // rather than overwriting global keys that can leak into another concurrent report.
        if !metadata.isEmpty,
           let data = try? JSONSerialization.data(withJSONObject: metadata, options: [.sortedKeys]),
           let json = String(data: data, encoding: .utf8) {
            reason += "\nMetadata: \(json)"
        }

        let model = ExceptionModel(name: name, reason: reason)
        let stack = origin.getStackTrace()
        model.stackTrace = (0..<stack.size).compactMap { index -> StackFrame? in
            guard let nativeLine = stack.get(index: index) else { return nil }
            let line = nativeLine as String
            // Kotlin/Native frames contain native instruction addresses. Let the matching
            // app dSYM symbolicate them; NSError would capture this Swift reporting bridge.
            if let token = line.split(whereSeparator: { $0.isWhitespace }).first(where: { $0.hasPrefix("0x") }),
               let address = UInt(token.dropFirst(2), radix: 16) {
                return StackFrame(address: address)
            }
            return StackFrame(symbol: line, file: "", line: 0)
        }
        Crashlytics.crashlytics().record(exceptionModel: model)
    }
}
