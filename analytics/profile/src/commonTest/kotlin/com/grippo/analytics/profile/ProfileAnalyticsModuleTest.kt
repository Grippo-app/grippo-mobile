package com.grippo.analytics.profile

import com.grippo.analytics.profile.internal.ProfileAnalyticsImpl
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.ksp.generated.module

class ProfileAnalyticsModuleTest {
    @Test
    fun annotatedModuleBindsOneStatelessServiceWithoutCustomSchemas() {
        val recorder = RecordingProfileAnalytics()
        val application = koinApplication {
            modules(module { single<Analytics> { recorder } }, ProfileAnalyticsModule().module)
        }
        try {
            val analytics = application.koin.get<ProfileAnalytics>()
            assertIs<ProfileAnalyticsImpl>(analytics)
            assertSame(analytics, application.koin.get<ProfileAnalytics>())
            assertTrue(application.koin.getAll<AnalyticsSchema>().isEmpty())
            analytics.presentScreen(ProfileScreen.Menu)
            assertEquals("profile_menu", recorder.views.single().first.screen.analyticsCode)
        } finally {
            application.close()
        }
    }
}
