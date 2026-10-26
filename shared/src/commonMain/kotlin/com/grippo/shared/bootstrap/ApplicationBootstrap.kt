package com.grippo.shared.bootstrap

import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.local.settings.models.AnalyticsConsent
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.lifecycle.AnalyticsBootstrap
import com.grippo.toolkit.analytics.lifecycle.AnalyticsCollection
import com.grippo.toolkit.crashreporting.lifecycle.CrashReportingBootstrap
import com.grippo.toolkit.localization.AppLocale
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen

public class ApplicationBootstrap internal constructor(
    public val analytics: AnalyticsBootstrap,
    public val crashReporting: CrashReportingBootstrap,
    public val messaging: MessagingBootstrap,
    private val localSettingsFeature: LocalSettingsFeature,
    private val collectionController: Analytics,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        observeAnalyticsConsent()
        observeLanguage()
    }

    private fun observeAnalyticsConsent() {
        localSettingsFeature.observeAnalyticsConsent()
            .distinctUntilChanged()
            .onEach { consent ->
                collectionController.updateCollection(
                    if (consent == AnalyticsConsent.Granted) AnalyticsCollection.Granted else AnalyticsCollection.Denied
                )
            }
            .retryWhen { error, attempt ->
                if (error is CancellationException || error !is Exception) throw error
                collectionController.updateCollection(AnalyticsCollection.Denied)
                AppLogger.General.error("Unable to read analytics consent")
                delay(1.seconds * (attempt + 1).coerceAtMost(30).toInt())
                true
            }
            .launchIn(scope)
    }

    private fun observeLanguage() {
        localSettingsFeature.observeLanguage()
            .map { language -> language?.key }
            .distinctUntilChanged()
            .onEach(AppLocale::update)
            .retryWhen { error, attempt ->
                if (error is CancellationException || error !is Exception) {
                    throw error
                }
                if (!AppLocale.isResolved) {
                    AppLocale.update(null)
                }
                AppLogger.General.error("Unable to read app language")
                delay(1.seconds * (attempt + 1).coerceAtMost(30).toInt())
                true
            }
            .launchIn(scope)
    }
}
