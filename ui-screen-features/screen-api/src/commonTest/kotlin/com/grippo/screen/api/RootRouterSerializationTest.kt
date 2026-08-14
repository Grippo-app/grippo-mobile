package com.grippo.screen.api

import com.grippo.core.state.stage.StageState
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Anchor: `test:root-router-serialization`.
 *
 * Every [RootRouter] subtype (and its payload) is persisted by Decompose's
 * StateKeeper into the platform saved-state bundle, so the router graph must
 * survive a kotlinx-serialization round-trip. This guards the post-auth root
 * restructure (`Home` -> `Main`) and the payload-bearing configs.
 */
internal class RootRouterSerializationTest {

    private val json = Json { }

    private fun roundTrip(value: RootRouter) {
        val encoded = json.encodeToString(RootRouter.serializer(), value)
        val decoded = json.decodeFromString(RootRouter.serializer(), encoded)
        assertEquals(value, decoded)
    }

    @Test
    fun main_roundTrips() = roundTrip(RootRouter.Main)

    @Test
    fun trainings_roundTrips() = roundTrip(RootRouter.Trainings)

    @Test
    fun debug_roundTrips() = roundTrip(RootRouter.Debug)

    @Test
    fun auth_roundTrips() = roundTrip(RootRouter.Auth(AuthRouter.AuthProcess))

    @Test
    fun profileBody_roundTrips() = roundTrip(RootRouter.Profile(ProfileRouter.Body))

    @Test
    fun trainingAdd_roundTrips() = roundTrip(RootRouter.Training(StageState.Add))

    @Test
    fun trainingEdit_roundTrips() = roundTrip(RootRouter.Training(StageState.Edit("x")))

    @Test
    fun trainingDraft_roundTrips() = roundTrip(RootRouter.Training(StageState.Draft))
}
