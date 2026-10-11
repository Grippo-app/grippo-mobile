package com.grippo.services.firebase

import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.grippo.toolkit.crashreporting.CrashAttribute
import com.grippo.toolkit.crashreporting.CrashCode
import com.grippo.toolkit.crashreporting.CrashContext
import com.grippo.toolkit.crashreporting.CrashSink

public class AndroidFirebaseCrashlytics(private val core: FirebaseCrashlytics) : CrashSink {
    override fun breadcrumb(code: CrashCode) {
        core.log(code.value)
    }

    override fun record(throwable: Throwable, context: CrashContext) {
        if (context.attributes.isEmpty()) {
            core.recordException(throwable)
            return
        }
        val keys = CustomKeysAndValues.Builder()
        context.attributes.forEach { attribute ->
            when (attribute) {
                is CrashAttribute.Code -> keys.putString(attribute.key.value, attribute.value.value)
                is CrashAttribute.Count -> keys.putLong(attribute.key.value, attribute.value)
                is CrashAttribute.Flag -> keys.putBoolean(attribute.key.value, attribute.value)
            }
        }
        core.recordException(throwable, keys.build())
    }
}
