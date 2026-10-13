package com.grippo.analytics.profile

import com.grippo.analytics.profile.internal.ProfileAnalyticsImpl
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ProfileScreenTest {
    @Test
    fun allLeafScreensHaveStableUniqueIdentifiersWithoutFlowOrReferrer() {
        val recorder = RecordingProfileAnalytics()
        val analytics: ProfileAnalytics = ProfileAnalyticsImpl(recorder)
        ProfileScreen.entries.forEach { analytics.presentScreen(it) }
        val identifiers = recorder.views.map { it.first.screen.analyticsCode }
        assertEquals(
            listOf(
                "profile_menu",
                "profile_body",
                "profile_goal",
                "profile_experience",
                "profile_muscles",
                "profile_equipments",
                "profile_settings",
                "profile_social",
            ),
            identifiers,
        )
        assertEquals(ProfileScreen.entries.size, identifiers.distinct().size)
        recorder.views.forEach { (navigation, flow) ->
            assertEquals(NavigationReason.Initial, navigation.reason)
            assertNull(navigation.referrer)
            assertNull(flow)
        }
    }

    @Test
    fun observedReasonAndPresentationOwnershipPassThroughUnchanged() {
        val recorder = RecordingProfileAnalytics()
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
        val actual = ProfileAnalyticsImpl(observer).presentScreen(ProfileScreen.Body, NavigationReason.Restore)
        assertSame(presentation, actual)
        assertEquals(NavigationReason.Restore, recorder.views.single().first.reason)
        assertFalse(closed)
        actual.close()
        assertTrue(closed)
    }

    @Test
    fun ordinaryPresentationFailureReachesExistingLifecycleBoundaryUnchanged() {
        val error = IllegalStateException("Reporter unavailable")
        val observer = object : Analytics by RecordingProfileAnalytics() {
            override fun presentScreen(navigation: NavigationContext, flow: FlowContext?): ScreenPresentation =
                throw error
        }
        assertSame(error, assertFailsWith<IllegalStateException> {
            ProfileAnalyticsImpl(observer).presentScreen(ProfileScreen.Goal)
        })
    }

    @Test
    fun fatalPresentationFailureRemainsFatal() {
        val error = AssertionError("Fatal reporter failure")
        val observer = object : Analytics by RecordingProfileAnalytics() {
            override fun presentScreen(navigation: NavigationContext, flow: FlowContext?): ScreenPresentation =
                throw error
        }
        assertSame(error, assertFailsWith<AssertionError> {
            ProfileAnalyticsImpl(observer).presentScreen(ProfileScreen.Settings)
        })
    }

    @Test
    fun closeFailureReachesExistingLifecycleBoundaryWithoutWrapping() {
        val error = IllegalStateException("Close unavailable")
        val presentation = object : ScreenPresentation {
            override fun close(): Unit = throw error
        }
        val observer = object : Analytics by RecordingProfileAnalytics() {
            override fun presentScreen(navigation: NavigationContext, flow: FlowContext?): ScreenPresentation =
                presentation
        }
        val actual = ProfileAnalyticsImpl(observer).presentScreen(ProfileScreen.Social)
        assertSame(presentation, actual)
        assertSame(error, assertFailsWith<IllegalStateException> { actual.close() })
    }
}
