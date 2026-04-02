package com.grippo.android

import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.services.firebase.AndroidFirebaseAnalytics
import com.grippo.services.firebase.AndroidFirebaseCrashlytics
import com.grippo.services.firebase.AndroidFirebaseMessaging
import com.grippo.services.firebase.FirebaseProvider
import com.grippo.services.firebase.MessagingEventHandler
import com.grippo.shared.Koin
import com.grippo.toolkit.local.notification.NotificationManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.dsl.module

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        Koin.init {
            androidContext(this@App)
            androidLogger()
            modules(
                module {
                    single<MessagingEventHandler> {
                        MessagingEventHandlerImpl(
                            authorizationFeature = get<AuthorizationFeature>(),
                            notificationManager = get<NotificationManager>(),
                        )
                    }
                }
            )
        }

        FirebaseProvider.setup(
            analytics = AndroidFirebaseAnalytics(
                core = FirebaseAnalytics.getInstance(this)
            ),
            crashlytics = AndroidFirebaseCrashlytics(
                core = FirebaseCrashlytics.getInstance()
            ),
            messaging = AndroidFirebaseMessaging()
        )
    }
}
