package com.grippo.analytics.authorization

import com.grippo.analytics.authorization.internal.AuthAnalyticsImpl
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AuthScreenTest {
    @Test
    fun allLeafScreensHaveStableExplicitCodesWithoutFlowOrHiddenEvents() {
        val recorder = RecordingAuthAnalytics()
        val analytics: AuthAnalytics = AuthAnalyticsImpl(recorder)
        AuthScreen.entries.forEach { analytics.presentScreen(it) }
        assertEquals(
            listOf(
                "auth_splash",
                "auth_login",
                "auth_registration",
                "profile_creation_user",
                "profile_creation_experience",
                "profile_creation_excluded_muscles",
                "profile_creation_missing_equipments",
                "profile_creation_completed",
            ),
            recorder.views.map { it.navigation.screen.analyticsCode },
        )
        assertTrue(recorder.events.isEmpty())
        recorder.views.forEach { view ->
            assertEquals(NavigationReason.Initial, view.navigation.reason)
            assertNull(view.navigation.referrer)
            assertNull(view.flow)
        }
    }

    @Test
    fun observedReasonAndPresentationOwnershipPassThroughUnchanged() {
        val recorder = RecordingAuthAnalytics()
        var closed = false
        val presentation = object : ScreenPresentation {
            override fun close() {
                closed = true
            }
        }
        val observer = object : Analytics by recorder {
            override fun presentScreen(navigation: NavigationContext, flow: FlowContext?): ScreenPresentation {
                recorder.presentScreen(navigation, flow)
                return presentation
            }
        }
        val actual = AuthAnalyticsImpl(observer).presentScreen(AuthScreen.Registration, NavigationReason.Back)
        assertSame(presentation, actual)
        assertEquals(NavigationReason.Back, recorder.views.single().navigation.reason)
        assertTrue(!closed)
        actual.close()
        assertTrue(closed)
    }
}
