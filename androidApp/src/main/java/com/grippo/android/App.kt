package com.grippo.android

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.grippo.services.firebase.AndroidFirebaseAnalytics
import com.grippo.services.firebase.AndroidFirebaseCrashlytics
import com.grippo.services.firebase.AndroidFirebaseMessaging
import com.grippo.services.firebase.FirebaseProvider
import com.grippo.shared.Koin
import com.grippo.shared.AnalyticsRuntime
import com.grippo.toolkit.analytics.AnalyticsCollection
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        Koin.init(analyticsDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            androidContext(this@App)
            androidLogger()
        }

        val analytics = AndroidFirebaseAnalytics(FirebaseAnalytics.getInstance(this))
        analytics.setCollection(AnalyticsCollection.Unknown)
        AnalyticsRuntime.attachSink(analytics)

        FirebaseProvider.setup(
            crashlytics = AndroidFirebaseCrashlytics(
                core = FirebaseCrashlytics.getInstance()
            ),
            messaging = AndroidFirebaseMessaging()
        )
    }
}
