package com.grippo.android

import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics
import com.grippo.services.firebase.AndroidFirebaseAnalytics
import com.grippo.services.firebase.FirebaseProvider
import com.grippo.shared.Koin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        Koin.init {
            androidContext(this@App)
            androidLogger()
        }

        FirebaseProvider.init(
            analytics = AndroidFirebaseAnalytics(
                firebase = FirebaseAnalytics.getInstance(this)
            )
        )
    }
}