package com.grippo.services.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.grippo.toolkit.analytics.AnalyticsCollection
import com.grippo.toolkit.analytics.AnalyticsIdentityPayload
import com.grippo.toolkit.analytics.AnalyticsPayload
import com.grippo.toolkit.analytics.AnalyticsSink
import com.grippo.toolkit.analytics.AnalyticsValue

public class AndroidFirebaseAnalytics(
    private val core: FirebaseAnalytics
) : AnalyticsSink {

    override fun send(payload: AnalyticsPayload) {
        val bundle = Bundle().apply {
            payload.parameters.forEach { (key, value) ->
                when (value) {
                    is AnalyticsValue.Text -> putString(key, value.value)
                    is AnalyticsValue.Integer -> putLong(key, value.value)
                    is AnalyticsValue.Decimal -> putDouble(key, value.value)
                }
            }
        }
        core.logEvent(payload.name, bundle)
    }

    override fun setCollection(collection: AnalyticsCollection) {
        val enabled = collection == AnalyticsCollection.Granted
        if (!enabled) core.setAnalyticsCollectionEnabled(false)
        core.setConsent(
            mapOf(
                FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to if (enabled) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED,
                FirebaseAnalytics.ConsentType.AD_STORAGE to FirebaseAnalytics.ConsentStatus.DENIED,
                FirebaseAnalytics.ConsentType.AD_USER_DATA to FirebaseAnalytics.ConsentStatus.DENIED,
                FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED,
            )
        )
        if (enabled) core.setAnalyticsCollectionEnabled(true)
    }

    override fun setIdentity(identity: AnalyticsIdentityPayload) {
        core.setUserId(identity.userId?.value)
        identity.clearedPropertyNames.forEach { core.setUserProperty(it, null) }
        identity.properties.forEach { (name, value) -> core.setUserProperty(name, value) }
    }
}
