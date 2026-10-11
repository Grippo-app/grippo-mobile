import FirebaseCrashlytics
import Foundation
import shared

final class IosFirebaseCrashlytics: CrashSink {

    func breadcrumb(code: CrashCode) {
        autoreleasepool { Crashlytics.crashlytics().log(code.value) }
    }

    func record(
        throwable: KotlinThrowable,
        context: CrashContext
    ) {
        autoreleasepool { recordNative(throwable: throwable, context: context) }
    }

    private func recordNative(throwable: KotlinThrowable, context: CrashContext) {
        let name = CrashExceptionInfo.shared.typeName(throwable: throwable)
        var reason = throwable.message ?? name
        var origin = throwable
        var visited = Set<ObjectIdentifier>([ObjectIdentifier(throwable)])
        while visited.count < 32, let cause = origin.cause, visited.insert(ObjectIdentifier(cause)).inserted {
            let causeName = CrashExceptionInfo.shared.typeName(throwable: cause)
            reason += "\nCaused by: \(causeName): \(cause.message ?? "")"
            origin = cause
        }

        // ExceptionModel has no per-event custom-key API. Keep metadata with this event,
        // rather than overwriting global keys that can leak into another concurrent report.
        var metadata: [String: Any] = [:]
        for attribute in context.attributes {
            switch attribute {
            case let code as CrashAttribute.Code: metadata[code.key.value] = code.value.value
            case let count as CrashAttribute.Count: metadata[count.key.value] = NSNumber(value: count.value)
            case let flag as CrashAttribute.Flag: metadata[flag.key.value] = flag.value
            default: preconditionFailure("Unsupported crash attribute")
            }
        }
        if !metadata.isEmpty {
            do {
                let data = try JSONSerialization.data(withJSONObject: metadata, options: [.sortedKeys])
                guard let json = String(data: data, encoding: .utf8) else {
                    preconditionFailure("Crash context must encode as UTF-8")
                }
                reason += "\nMetadata: \(json)"
            } catch { preconditionFailure("Invalid typed crash context") }
        }

        let model = ExceptionModel(name: name, reason: reason)
        let stack = origin.getStackTrace()
        model.stackTrace = (0..<min(stack.size, 128)).compactMap { index -> StackFrame? in
            guard let nativeLine = stack.get(index: index) else { return nil }
            let line = nativeLine as String
            // Kotlin/Native frames contain native instruction addresses. Let the matching
            // app dSYM symbolicate them; NSError would capture this Swift reporting bridge.
            if let token = line.split(whereSeparator: { $0.isWhitespace }).first(where: { $0.hasPrefix("0x") }),
                let address = UInt(token.dropFirst(2), radix: 16)
            {
                return StackFrame(address: address)
            }
            return StackFrame(symbol: line, file: "", line: 0)
        }
        Crashlytics.crashlytics().record(exceptionModel: model)
    }
}
