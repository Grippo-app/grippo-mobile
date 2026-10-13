package com.grippo.shared.bootstrap

import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.lifecycle.AnalyticsBootstrap
import com.grippo.toolkit.analytics.lifecycle.AnalyticsCollection
import com.grippo.toolkit.crashreporting.lifecycle.CrashReportingBootstrap
import com.grippo.toolkit.logger.AppLogger
import com.grippo.toolkit.messaging.lifecycle.MessagingBootstrap
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen

/** Instance-bound startup dependencies. Contains no global container lookup or SDK logic. */
public class ApplicationBootstrap internal constructor(
    public val analytics: AnalyticsBootstrap,
    public val crashReporting: CrashReportingBootstrap,
    public val messaging: MessagingBootstrap,
    localSettingsFeature: LocalSettingsFeature,
    collectionController: Analytics,
) {
    // Application lifetime: consent applies before and after the platform sink is attached.
    private val collectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        localSettingsFeature.observeAnalyticsConsent()
            .distinctUntilChanged()
            .onEach { granted ->
                collectionController.updateCollection(
                    if (granted) AnalyticsCollection.Granted else AnalyticsCollection.Denied
                )
            }
            .retryWhen { error, attempt ->
                if (error is CancellationException || error !is Exception) throw error
                collectionController.updateCollection(AnalyticsCollection.Denied)
                AppLogger.General.error("Unable to read analytics consent")
                delay(1.seconds * (attempt + 1).coerceAtMost(30).toInt())
                true
            }
            .launchIn(collectionScope)
    }
}
