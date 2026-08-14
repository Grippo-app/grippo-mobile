package com.grippo.shared.root

import com.grippo.core.state.stage.StageState
import com.grippo.screen.api.AuthRouter
import com.grippo.screen.api.ProfileRouter
import com.grippo.screen.api.RootRouter
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Anchor: `test:root-post-auth-navigation`.
 *
 * Verifies the pure [RootDirection.toNav] seam that the root component uses to
 * translate navigation intents into stack operations. The key acceptance is that
 * the post-auth root is [RootRouter.Main] (reached via ReplaceAll, not Push), so
 * the bottom-bar host replaces the auth stack rather than layering on top of it.
 */
internal class RootDirectionMappingTest {

    @Test
    fun main_replacesAllToMain() {
        assertEquals(RootNav.ReplaceAll(RootRouter.Main), RootDirection.Main.toNav())
    }

    @Test
    fun login_replacesAllToAuthProcess() {
        assertEquals(
            RootNav.ReplaceAll(RootRouter.Auth(AuthRouter.AuthProcess)),
            RootDirection.Login.toNav(),
        )
    }

    @Test
    fun training_pushesTrainingWithStage() {
        assertEquals(
            RootNav.Push(RootRouter.Training(StageState.Add)),
            RootDirection.Training(StageState.Add).toNav(),
        )
    }

    @Test
    fun back_pops() {
        assertEquals(RootNav.Pop, RootDirection.Back.toNav())
    }

    @Test
    fun close_closes() {
        assertEquals(RootNav.Close, RootDirection.Close.toNav())
    }

    @Test
    fun weightHistory_pushesProfileBody() {
        assertEquals(
            RootNav.Push(RootRouter.Profile(ProfileRouter.Body)),
            RootDirection.WeightHistory.toNav(),
        )
    }

    @Test
    fun missingEquipment_pushesProfileEquipments() {
        assertEquals(
            RootNav.Push(RootRouter.Profile(ProfileRouter.Equipments)),
            RootDirection.MissingEquipment.toNav(),
        )
    }

    @Test
    fun excludedMuscles_pushesProfileMuscles() {
        assertEquals(
            RootNav.Push(RootRouter.Profile(ProfileRouter.Muscles)),
            RootDirection.ExcludedMuscles.toNav(),
        )
    }

    /**
     * Exhaustive mapping table: every [RootDirection] subtype must map to the
     * EXACT [RootNav] value dictated by the production contract. Expected values
     * are written from the contract (not obtained by calling [toNav]), so a wrong
     * production mapping (e.g. `Settings` -> a different ProfileRouter, or `Debug`
     * -> the wrong router) fails this test. Covers all three [StageState] payloads
     * for `Training`.
     */
    @Test
    fun everyDirection_mapsToExactNav() {
        val expected: List<Pair<RootDirection, RootNav>> = listOf(
            RootDirection.Main to RootNav.ReplaceAll(RootRouter.Main),
            RootDirection.Login to RootNav.ReplaceAll(RootRouter.Auth(AuthRouter.AuthProcess)),
            RootDirection.Training(StageState.Add) to
                RootNav.Push(RootRouter.Training(StageState.Add)),
            RootDirection.Training(StageState.Edit("x")) to
                RootNav.Push(RootRouter.Training(StageState.Edit("x"))),
            RootDirection.Training(StageState.Draft) to
                RootNav.Push(RootRouter.Training(StageState.Draft)),
            RootDirection.Profile to RootNav.Push(RootRouter.Profile(ProfileRouter.Body)),
            RootDirection.WeightHistory to RootNav.Push(RootRouter.Profile(ProfileRouter.Body)),
            RootDirection.MissingEquipment to
                RootNav.Push(RootRouter.Profile(ProfileRouter.Equipments)),
            RootDirection.ExcludedMuscles to
                RootNav.Push(RootRouter.Profile(ProfileRouter.Muscles)),
            RootDirection.Experience to RootNav.Push(RootRouter.Profile(ProfileRouter.Experience)),
            RootDirection.Settings to RootNav.Push(RootRouter.Profile(ProfileRouter.Settings)),
            RootDirection.Social to RootNav.Push(RootRouter.Profile(ProfileRouter.Social)),
            RootDirection.Goal to RootNav.Push(RootRouter.Profile(ProfileRouter.Goal)),
            RootDirection.Debug to RootNav.Push(RootRouter.Debug),
            RootDirection.Trainings to RootNav.Push(RootRouter.Trainings),
            RootDirection.Back to RootNav.Pop,
            RootDirection.Close to RootNav.Close,
        )
        expected.forEach { (direction, nav) ->
            assertEquals(nav, direction.toNav(), "wrong RootNav for $direction")
        }
    }
}
