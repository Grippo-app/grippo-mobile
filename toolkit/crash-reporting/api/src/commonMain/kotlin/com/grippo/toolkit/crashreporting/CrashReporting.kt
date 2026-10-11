package com.grippo.toolkit.crashreporting

private val codePattern = Regex("[a-z][a-z0-9_]{0,63}")

/** Stable diagnostic codes only; never user input, tokens or personal data. */
public data class CrashCode(public val value: String) {
    init {
        require(codePattern.matches(value))
    }
}

public sealed class CrashAttribute(public val key: CrashCode) {
    public class Code(
        key: CrashCode,
        public val value: CrashCode
    ) : CrashAttribute(key)

    public class Count(
        key: CrashCode,
        public val value: Long
    ) : CrashAttribute(key) {

        init {
            require(value >= 0)
        }
    }

    public class Flag(
        key: CrashCode,
        public val value: Boolean
    ) : CrashAttribute(key)
}

public class CrashContext(attributes: List<CrashAttribute> = emptyList()) {
    public val attributes: List<CrashAttribute>

    init {
        require(attributes.size <= 32)
        this.attributes = attributes.toList()
        if (this.attributes.isNotEmpty()) {
            val keys = HashSet<CrashCode>(this.attributes.size)
            require(this.attributes.all { keys.add(it.key) })
        }
    }

    public companion object {
        public val Empty: CrashContext = CrashContext()
    }
}

/** Nonfatal errors retain their native stack. Cancellation is never a crash report. */
public interface CrashReporting {
    public fun record(throwable: Throwable, context: CrashContext = CrashContext.Empty)
    public fun breadcrumb(code: CrashCode)
}

/** SDK-only boundary; synchronous submission, not remote delivery. Core invokes on its background worker; local SDK persistence may block. No core callbacks. */
public interface CrashSink {
    public fun record(throwable: Throwable, context: CrashContext)
    public fun breadcrumb(code: CrashCode)
}

public object CrashExceptionInfo {
    public fun typeName(throwable: Throwable): String =
        throwable::class.qualifiedName ?: throwable::class.simpleName ?: "Throwable"
}
