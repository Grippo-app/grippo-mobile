package com.grippo.analytics.profile.internal

import com.grippo.analytics.profile.ProfileAnalytics
import com.grippo.analytics.profile.internal.encoding.ProfileEncoding
import com.grippo.analytics.profile.internal.operation.ProfileOperationEvents
import com.grippo.analytics.profile.internal.schema.ProfileEventMatrix
import com.grippo.analytics.profile.models.ProfileEvent
import com.grippo.analytics.profile.models.ProfileOperation
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import com.grippo.toolkit.analytics.operation.trackOperation
import org.koin.core.annotation.Single

@Single(binds = [ProfileAnalytics::class])
internal class ProfileAnalyticsImpl(private val analytics: Analytics) : ProfileAnalytics {
    override fun trackEvent(event: ProfileEvent) {
        try {
            analytics.track(ProfileEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block the observed user action.
        }
    }

    override suspend fun <T> trackOperation(operation: ProfileOperation, block: suspend () -> T): T {
        return analytics.trackOperation(ProfileOperationEvents<T>(operation), block = block)
    }

    override fun presentScreen(screen: ProfileScreen, reason: NavigationReason): ScreenPresentation {
        return analytics.presentScreen(
            NavigationContext(screen = ScreenId(ProfileEncoding.code(screen)), reason = reason)
        )
    }
}
