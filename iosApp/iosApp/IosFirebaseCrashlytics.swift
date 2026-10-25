import FirebaseCrashlytics
import Foundation
import shared

final class IosFirebaseCrashlytics: CrashReportingSink {

    func logBreadcrumb(code: CrashDiagnosticCode) {
        autoreleasepool { Crashlytics.crashlytics().log(code.value) }
    }

    func reportException(
        throwable: KotlinThrowable,
        context: CrashReportContext
    ) {
        autoreleasepool { recordNative(throwable: throwable, context: context) }
    }

    private func recordNative(throwable: KotlinThrowable, context: CrashReportContext) {
        let stackPolicy = CrashStackTracePolicy.shared
        let maximumCauseDepth = Int(stackPolicy.maximumCauseDepth)
        let maximumStackFrames = stackPolicy.maximumStackFrames
        let name = CrashExceptionType.shared.nameOf(throwable: throwable)
        var reason = throwable.message ?? name
        var origin = throwable
        var visited = Set<ObjectIdentifier>([ObjectIdentifier(throwable)])
        while visited.count < maximumCauseDepth, let cause = origin.cause, visited.insert(ObjectIdentifier(cause)).inserted {
            let causeName = CrashExceptionType.shared.nameOf(throwable: cause)
            reason += "\nCaused by: \(causeName): \(cause.message ?? "")"
            origin = cause
        }

        // ExceptionModel has no per-event keys; global custom keys would leak into concurrent reports.
        var metadata: [String: Any] = [:]
        for attribute in context.metadata {
            switch attribute {
            case let code as CrashMetadataEntry.Code: metadata[code.key.name] = code.value.value
            case let count as CrashMetadataEntry.Count: metadata[count.key.name] = NSNumber(value: count.value)
            case let flag as CrashMetadataEntry.Flag: metadata[flag.key.name] = flag.value
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
        model.stackTrace = (0..<min(stack.size, maximumStackFrames)).compactMap { index -> StackFrame? in
            guard let nativeLine = stack.get(index: index) else { return nil }
            let line = nativeLine as String
            // Pass raw Kotlin/Native addresses for dSYM symbolication; an NSError would capture this bridge's stack.
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
