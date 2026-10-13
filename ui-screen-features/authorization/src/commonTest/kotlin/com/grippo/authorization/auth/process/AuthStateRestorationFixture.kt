package com.grippo.authorization.auth.process

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.statekeeper.SerializableContainer
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.grippo.analytics.authorization.AuthAnalyticsModule
import com.grippo.authorization.analytics.AuthorizationFixture
import com.grippo.authorization.analytics.RecordingAuthAnalytics
import com.grippo.authorization.registration.RegistrationComponent
import com.grippo.authorization.registration.RegistrationViewModel
import com.grippo.core.foundation.CoreModule
import com.grippo.toolkit.analytics.Analytics
import kotlin.reflect.typeOf
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module

internal suspend fun TestScope.withAuthStateRestorationFixture(
    block: suspend TestScope.(AuthStateRestorationFixture) -> Unit,
) {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val fixture = AuthStateRestorationFixture()
    try {
        fixture.start()
        runCurrent()
        block(fixture)
    } finally {
        fixture.destroy()
        runCurrent()
        stopKoin()
        Dispatchers.resetMain()
    }
}

/** Registration uses portable domain dependencies and does not initialize native auth providers. */
internal class AuthStateRestorationFixture {
    val recorder = RecordingAuthAnalytics()
    val pendingProfile = CompletableDeferred<Unit>()
    val businessJobs = mutableListOf<Job>()
    val domain = AuthorizationFixture().apply {
        hasProfile = false
        beforeProfile = {
            businessJobs += assertNotNull(currentCoroutineContext()[Job])
            pendingProfile.await()
        }
    }
    var homeNavigations = 0
        private set
    var profileNavigations = 0
        private set
    var backNavigations = 0
        private set
    lateinit var component: RegistrationComponent
        private set
    private lateinit var lifecycle: LifecycleRegistry
    private lateinit var stateKeeper: StateKeeperDispatcher
    private lateinit var instanceKeeper: InstanceKeeperDispatcher

    fun start() {
        startKoin {
            modules(CoreModule().module, AuthAnalyticsModule().module, module {
                single<Analytics> { recorder }
                single { domain.registerUseCase() }
            })
        }
        create()
    }

    fun model(): RegistrationViewModel = assertIs<RegistrationViewModel>(
        component.instanceKeeper.get(typeOf<RegistrationViewModel>())
    )

    fun restore(vararg passwords: String) {
        val retained = model()
        val views = recorder.views.size
        val events = recorder.names
        val archive = Json.encodeToString(stateKeeper.save())
        // Consume materialized containers with JSON serializers, so compressed Base64 cannot hide a password.
        val root = assertNotNull(Json.decodeFromString<SerializableContainer>(archive).consume(JsonObject.serializer()))
        val state = assertNotNull(root.getValue("map").jsonObject.values.singleOrNull(), "Registration draft was not saved")
        val payload = assertNotNull(Json.decodeFromJsonElement<SerializableContainer>(state).consume(JsonObject.serializer()))
        assertFalse("password" in payload)
        assertFalse(passwords.any { it in payload.toString() })
        assertSame(retained, model())
        assertEquals(views, recorder.views.size)
        assertEquals(events, recorder.names)
        destroy()
        create(Json.decodeFromString<SerializableContainer>(archive))
    }

    fun destroy() {
        if (::lifecycle.isInitialized) lifecycle.destroy()
        if (::instanceKeeper.isInitialized) instanceKeeper.destroy()
    }

    private fun create(saved: SerializableContainer? = null) {
        lifecycle = LifecycleRegistry()
        stateKeeper = StateKeeperDispatcher(saved)
        instanceKeeper = InstanceKeeperDispatcher()
        component = RegistrationComponent(
            componentContext = DefaultComponentContext(lifecycle, stateKeeper, instanceKeeper),
            email = ROUTE_EMAIL,
            toCreateProfile = { profileNavigations++ },
            toHome = { homeNavigations++ },
            back = { backNavigations++ },
        )
        lifecycle.resume()
    }

    companion object {
        const val ROUTE_EMAIL = "route-prefill@example.com"
    }
}
