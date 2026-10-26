package com.grippo.toolkit.localization.internal

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Bundle
import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.localization.AppLocale

internal actual class AppLocaleObserver actual constructor(nativeContext: NativeContext) {

    init {
        val application = nativeContext.context.applicationContext as Application
        application.registerComponentCallbacks(ConfigurationCallbacks())
        application.registerActivityLifecycleCallbacks(ActivityCallbacks())
    }

    private class ConfigurationCallbacks : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            AppLocale.reapply()
        }

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onLowMemory() {}
    }

    private class ActivityCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            AppLocale.reapply()
        }

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}
    }
}
