package com.grippo.android

import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.grippo.services.firebase.AndroidFirebaseAnalytics
import com.grippo.services.firebase.AndroidFirebaseCrashlytics
import com.grippo.services.firebase.AndroidFirebaseMessaging
import com.grippo.shared.Koin
import com.grippo.toolkit.analytics.AnalyticsCollection
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val bootstrap = Koin.init {
            androidContext(this@App)
            androidLogger()
        }

        val analytics = AndroidFirebaseAnalytics(FirebaseAnalytics.getInstance(this))
        analytics.setCollection(AnalyticsCollection.Unknown)
        bootstrap.analytics.attachSink(analytics)

        bootstrap.crashReporting.attachSink(AndroidFirebaseCrashlytics(FirebaseCrashlytics.getInstance()))
        bootstrap.messaging.attachSink(AndroidFirebaseMessaging())
    }
}
