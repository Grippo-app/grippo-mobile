package com.grippo.authorization.profile.creation

import com.grippo.authorization.profile.creation.completed.ProfileCompletedLoader
import com.grippo.authorization.profile.creation.completed.ProfileCompletedViewModel
import com.grippo.authorization.profile.creation.excluded.muscles.ExcludedMusclesViewModel
import com.grippo.authorization.profile.creation.experience.ExperienceViewModel
import com.grippo.authorization.profile.creation.missing.equipments.MissingEquipmentsViewModel
import com.grippo.authorization.profile.creation.user.UserViewModel
import com.grippo.core.state.formatters.NameFormatState
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.local.settings.models.HomeWelcomeStatus
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.screen.api.ProfileCreationRouter
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class ProfileCreationRestorationTest {
    @Test
    fun restoresCurrentInvalidNameAndAcceptedMeasurementsWithoutNextOrNewInputEvents() = runTest {
        withProfileRestorationFixture { fixture ->
            val original = fixture.model<UserViewModel>()
            val views = fixture.recorder.views.size
            original.onNameChange("x".repeat(61))
            fixture.measurements(84.6f, 187)
            original.onNextClick()
            runCurrent()
            assertSame(original, fixture.model<UserViewModel>())
            assertEquals(views, fixture.recorder.views.size)
            val draft = original.state.value
            assertIs<NameFormatState.Invalid>(draft.name)
            val events = fixture.recorder.names
            val dialogs = fixture.dialogs.size

            fixture.restore()
            runCurrent()
            val restored = fixture.model<UserViewModel>()
            assertNotSame(original, restored)
            assertEquals(ProfileCreationRouter.User, fixture.component.childStack.value.active.configuration)
            assertEquals(draft, restored.state.value)
            assertEquals(events, fixture.recorder.names)
            assertEquals(dialogs, fixture.dialogs.size)
            assertEquals(NavigationReason.Initial, fixture.recorder.views.last().navigation.reason)
            restored.onNextClick()
            runCurrent()
            assertEquals(events, fixture.recorder.names)
            assertEquals(ProfileCreationRouter.User, fixture.component.childStack.value.active.configuration)
            assertTrue(fixture.profiles.isEmpty())

            restored.onNameChange("Corrected draft")
            restored.onNextClick()
            runCurrent()
            assertEquals(ProfileCreationRouter.Experience, fixture.component.childStack.value.active.configuration)
        }
    }

    @Test
    fun restoresExperienceSelectedBeforeNextAndBackReturnsToTheRestoredUserInputs() = runTest {
        withProfileRestorationFixture { fixture ->
            enterExperience(fixture)
            val original = fixture.model<ExperienceViewModel>()
            original.onExperienceClick(ExperienceEnumState.PRO)
            val events = fixture.recorder.names

            fixture.restore()
            runCurrent()
            val restored = fixture.model<ExperienceViewModel>()
            assertNotSame(original, restored)
            assertEquals(ProfileCreationRouter.Experience, fixture.component.childStack.value.active.configuration)
            assertEquals(ExperienceEnumState.PRO, restored.state.value.selected)
            assertEquals(ExperienceEnumState.entries.toList(), restored.state.value.suggestions)
            assertEquals(events, fixture.recorder.names)
            assertTrue(fixture.profiles.isEmpty())
            restored.onBack()
            runCurrent()
            val user = fixture.model<UserViewModel>()
            assertEquals("Draft person", user.state.value.name.value)
            assertEquals(84.6f, user.state.value.weight.value)
            assertEquals(187, user.state.value.height.value)
            user.onNameChange("Edited after restoration")
            fixture.restore()
            runCurrent()
            assertEquals("Edited after restoration", fixture.model<UserViewModel>().state.value.name.value)
        }
    }

    @Test
    fun restoresBothDeselectedCatalogDraftsAcrossEmptyAndChangedCatalogsAndBack() = runTest {
        withProfileRestorationFixture { fixture ->
            enterMuscles(fixture)
            val originalMuscles = fixture.model<ExcludedMusclesViewModel>()
            originalMuscles.onGroupClick("second")
            listOf("m1", "m2", "m3").forEach(originalMuscles::onSelect)
            val muscleEvents = fixture.recorder.names
            fixture.restore { fixture.muscles.value = emptyList() }
            runCurrent()
            val muscles = fixture.model<ExcludedMusclesViewModel>()
            assertNotSame(originalMuscles, muscles)
            assertTrue(muscles.state.value.suggestions.isEmpty())
            assertTrue(muscles.state.value.selectedMuscleIds.isEmpty())
            assertEquals("second", muscles.state.value.selectedGroupId)
            fixture.muscles.value = listOf(
                ProfileCreationRestorationFixture.muscles("first", "m1", "m2"),
                ProfileCreationRestorationFixture.muscles("second", "m3"),
            )
            runCurrent()
            assertTrue(muscles.state.value.selectedMuscleIds.isEmpty())
            assertEquals("second", muscles.state.value.selectedGroupId)
            fixture.muscles.value = listOf(ProfileCreationRestorationFixture.muscles("replacement", "m2", "m4"))
            runCurrent()
            assertEquals(listOf("m4"), muscles.state.value.selectedMuscleIds)
            assertEquals("replacement", muscles.state.value.selectedGroupId)
            assertEquals(muscleEvents, fixture.recorder.names)
            muscles.onNextClick()
            runCurrent()

            val originalEquipments = fixture.model<MissingEquipmentsViewModel>()
            originalEquipments.onGroupClick("second")
            listOf("e1", "e2", "e3").forEach(originalEquipments::onEquipmentClick)
            val equipmentEvents = fixture.recorder.names
            fixture.restore { fixture.equipments.value = emptyList() }
            runCurrent()
            val equipments = fixture.model<MissingEquipmentsViewModel>()
            assertNotSame(originalEquipments, equipments)
            assertTrue(equipments.state.value.suggestions.isEmpty())
            assertTrue(equipments.state.value.selectedEquipmentIds.isEmpty())
            assertEquals("second", equipments.state.value.selectedGroupId)
            fixture.equipments.value = listOf(
                ProfileCreationRestorationFixture.equipments("first", "e1", "e2"),
                ProfileCreationRestorationFixture.equipments("second", "e3"),
            )
            runCurrent()
            assertTrue(equipments.state.value.selectedEquipmentIds.isEmpty())
            assertEquals("second", equipments.state.value.selectedGroupId)
            fixture.equipments.value = listOf(ProfileCreationRestorationFixture.equipments("replacement", "e2", "e4"))
            runCurrent()
            assertEquals(listOf("e4"), equipments.state.value.selectedEquipmentIds)
            assertEquals("replacement", equipments.state.value.selectedGroupId)
            assertEquals(equipmentEvents, fixture.recorder.names)
            equipments.onBack()
            runCurrent()
            assertEquals(ProfileCreationRouter.ExcludedMuscles, fixture.component.childStack.value.active.configuration)
            val previousMuscles = fixture.model<ExcludedMusclesViewModel>()
            assertEquals(listOf("m4"), previousMuscles.state.value.selectedMuscleIds)
            assertEquals("replacement", previousMuscles.state.value.selectedGroupId)
            assertTrue(fixture.profiles.isEmpty())
        }
    }

    @Test
    fun nextBeforeCatalogReloadUsesBothRestoredPartialSelectionsInTheActualProfile() = runTest {
        withProfileRestorationFixture { fixture ->
            enterMuscles(fixture)
            val originalMuscles = fixture.model<ExcludedMusclesViewModel>()
            originalMuscles.onGroupClick("second")
            originalMuscles.onSelect("m1")
            val muscleEvents = fixture.recorder.names
            fixture.restore { fixture.muscles.value = emptyList() }
            runCurrent()
            val muscles = fixture.model<ExcludedMusclesViewModel>()
            assertNotSame(originalMuscles, muscles)
            assertTrue(muscles.state.value.suggestions.isEmpty())
            assertEquals(listOf("m2", "m3"), muscles.state.value.selectedMuscleIds)
            assertEquals("second", muscles.state.value.selectedGroupId)
            assertEquals(muscleEvents, fixture.recorder.names)
            muscles.onNextClick()
            runCurrent()
            assertEquals(listOf("m1"), fixture.model<ProfileCreationViewModel>(fixture.component)
                .state.value.excludedMuscleIds)

            val originalEquipments = fixture.model<MissingEquipmentsViewModel>()
            originalEquipments.onGroupClick("second")
            originalEquipments.onEquipmentClick("e2")
            val equipmentEvents = fixture.recorder.names
            fixture.restore { fixture.equipments.value = emptyList() }
            runCurrent()
            val equipments = fixture.model<MissingEquipmentsViewModel>()
            assertNotSame(originalEquipments, equipments)
            assertTrue(equipments.state.value.suggestions.isEmpty())
            assertEquals(listOf("e1", "e3"), equipments.state.value.selectedEquipmentIds)
            assertEquals("second", equipments.state.value.selectedGroupId)
            assertEquals(equipmentEvents, fixture.recorder.names)
            equipments.onNextClick()
            runCurrent()
            val parent = fixture.model<ProfileCreationViewModel>(fixture.component).state.value
            assertEquals(listOf("m1"), parent.excludedMuscleIds)
            assertEquals(listOf("e2"), parent.missingEquipmentIds)
            assertEquals(ProfileCreationRouter.Completed, fixture.component.childStack.value.active.configuration)
            assertEquals(listOf(CreateUserProfile("Draft person", 84.6f, ExperienceEnum.ADVANCED, 187,
                listOf("m1"), listOf("e2"))), fixture.profiles)
            assertEquals(0, fixture.homeNavigations)
            fixture.pendingProfile.complete(Unit)
            runCurrent()
            assertEquals(1, fixture.homeNavigations)
            assertEquals(listOf(HomeWelcomeStatus.PendingCelebration), fixture.welcomeStatuses)
            assertEquals(listOf("profile_operation_started", "profile_operation_completed"), fixture.recorder.events
                .filter { it.text("profile_action") == "create" }.map { it.event.schema.name })
        }
    }

    @Test
    fun restoresCompletedWithTheExactProfileAndRestartsItsOwnedBusinessOperation() = runTest {
        withProfileRestorationFixture { fixture ->
            enterMuscles(fixture)
            val muscles = fixture.model<ExcludedMusclesViewModel>()
            muscles.onSelect("m2")
            muscles.onSelect("m3")
            muscles.onNextClick()
            runCurrent()
            val equipments = fixture.model<MissingEquipmentsViewModel>()
            equipments.onEquipmentClick("e1")
            equipments.onEquipmentClick("e3")
            equipments.onNextClick()
            runCurrent()
            val expected = CreateUserProfile("Draft person", 84.6f, ExperienceEnum.ADVANCED, 187,
                listOf("m2", "m3"), listOf("e1", "e3"))
            assertEquals(listOf(expected), fixture.profiles)
            val original = fixture.model<ProfileCompletedViewModel>()
            assertTrue(fixture.domain.calls.isEmpty())
            assertEquals(0, fixture.homeNavigations)

            fixture.restore()
            runCurrent()
            val restored = fixture.model<ProfileCompletedViewModel>()
            assertNotSame(original, restored)
            assertEquals(ProfileCreationRouter.Completed, fixture.component.childStack.value.active.configuration)
            assertEquals(listOf(expected, expected), fixture.profiles)
            assertTrue(original.loaders.value.isEmpty())
            assertEquals(setOf<ProfileCompletedLoader>(ProfileCompletedLoader.ProfileCreation), restored.loaders.value)
            assertTrue(fixture.domain.calls.isEmpty())
            assertTrue(fixture.welcomeStatuses.isEmpty())
            assertEquals(0, fixture.homeNavigations)

            fixture.pendingProfile.complete(Unit)
            runCurrent()
            assertEquals(1, fixture.homeNavigations)
            assertTrue(restored.loaders.value.isEmpty())
            assertEquals(listOf("profile", "excluded_muscles", "excluded_equipments", "exercise_examples", "weight_history"),
                fixture.domain.calls)
            assertEquals(listOf(HomeWelcomeStatus.PendingCelebration), fixture.welcomeStatuses)
            val operations = fixture.recorder.events.filter { it.text("profile_action") == "create" }
            assertEquals(listOf("profile_operation_started", "profile_operation_interrupted",
                "profile_operation_started", "profile_operation_completed"), operations.map { it.event.schema.name })
            assertNotNull(operations[0].operation)
            assertEquals(operations[0].operation, operations[1].operation)
            assertNotEquals(operations[0].operation, operations[2].operation)
            assertEquals(operations[2].operation, operations[3].operation)
        }
    }

    @Test
    fun acceptedExperienceSeedsANewChildAfterBackAndParentRestoration() = runTest {
        withProfileRestorationFixture { fixture ->
            enterExperience(fixture)
            val original = fixture.model<ExperienceViewModel>()
            original.onExperienceClick(ExperienceEnumState.ADVANCED)
            original.onNextClick()
            runCurrent()
            fixture.model<ExcludedMusclesViewModel>().onBack()
            runCurrent()
            fixture.model<ExperienceViewModel>().onBack()
            runCurrent()
            assertEquals(ProfileCreationRouter.User, fixture.component.childStack.value.active.configuration)
            assertEquals(ExperienceEnumState.ADVANCED,
                fixture.model<ProfileCreationViewModel>(fixture.component).state.value.experience)

            fixture.restore()
            runCurrent()
            assertEquals(ExperienceEnumState.ADVANCED,
                fixture.model<ProfileCreationViewModel>(fixture.component).state.value.experience)
            val selectionEvents = fixture.recorder.names.count { it == "auth_experience_selected" }
            fixture.model<UserViewModel>().onNextClick()
            runCurrent()
            val entered = fixture.model<ExperienceViewModel>()
            assertNotSame(original, entered)
            assertEquals(ExperienceEnumState.ADVANCED, entered.state.value.selected)
            assertEquals(selectionEvents, fixture.recorder.names.count { it == "auth_experience_selected" })
            assertTrue(fixture.profiles.isEmpty())
            assertTrue(fixture.domain.calls.isEmpty())
            assertEquals(0, fixture.homeNavigations)

            entered.onExperienceClick(ExperienceEnumState.PRO)
            val events = fixture.recorder.names
            fixture.restore()
            runCurrent()
            val restoredLocal = fixture.model<ExperienceViewModel>()
            assertNotSame(entered, restoredLocal)
            assertEquals(ExperienceEnumState.PRO, restoredLocal.state.value.selected)
            assertEquals(ExperienceEnumState.ADVANCED,
                fixture.model<ProfileCreationViewModel>(fixture.component).state.value.experience)
            assertEquals(events, fixture.recorder.names)
            assertTrue(fixture.profiles.isEmpty())
            assertTrue(fixture.domain.calls.isEmpty())
            assertEquals(0, fixture.homeNavigations)
        }
    }

    @Test
    fun acceptedExcludedMusclesSeedANewChildAfterBackAndParentRestoration() = runTest {
        withProfileRestorationFixture { fixture ->
            enterMuscles(fixture)
            val original = fixture.model<ExcludedMusclesViewModel>()
            original.onSelect("m1")
            original.onNextClick()
            runCurrent()
            fixture.model<MissingEquipmentsViewModel>().onBack()
            runCurrent()
            fixture.model<ExcludedMusclesViewModel>().onBack()
            runCurrent()
            assertEquals(ProfileCreationRouter.Experience, fixture.component.childStack.value.active.configuration)
            assertEquals(listOf("m1"),
                fixture.model<ProfileCreationViewModel>(fixture.component).state.value.excludedMuscleIds)

            fixture.restore()
            runCurrent()
            assertEquals(listOf("m1"),
                fixture.model<ProfileCreationViewModel>(fixture.component).state.value.excludedMuscleIds)
            fixture.model<ExperienceViewModel>().onNextClick()
            runCurrent()
            val entered = fixture.model<ExcludedMusclesViewModel>()
            assertNotSame(original, entered)
            assertEquals(listOf("m2", "m3"), entered.state.value.selectedMuscleIds)
            assertEquals(listOf("m1", "m2", "m3"), entered.state.value.knownMuscleIds)
            assertEquals(listOf("m1"),
                fixture.model<ProfileCreationViewModel>(fixture.component).state.value.excludedMuscleIds)
            assertTrue(fixture.profiles.isEmpty())
            assertTrue(fixture.domain.calls.isEmpty())
            assertEquals(0, fixture.homeNavigations)

            entered.onSelect("m1")
            val selected = entered.state.value.selectedMuscleIds
            val events = fixture.recorder.names
            fixture.restore { fixture.muscles.value = emptyList() }
            runCurrent()
            val restoredLocal = fixture.model<ExcludedMusclesViewModel>()
            assertNotSame(entered, restoredLocal)
            assertTrue(restoredLocal.state.value.suggestions.isEmpty())
            assertEquals(selected, restoredLocal.state.value.selectedMuscleIds)
            assertEquals(listOf("m1", "m2", "m3"), restoredLocal.state.value.knownMuscleIds)
            assertEquals(listOf("m1"),
                fixture.model<ProfileCreationViewModel>(fixture.component).state.value.excludedMuscleIds)
            fixture.muscles.value = listOf(
                ProfileCreationRestorationFixture.muscles("first", "m1", "m2"),
                ProfileCreationRestorationFixture.muscles("second", "m3"),
            )
            runCurrent()
            assertEquals(listOf("m1", "m2", "m3"), restoredLocal.state.value.selectedMuscleIds)
            assertEquals(listOf("m1"),
                fixture.model<ProfileCreationViewModel>(fixture.component).state.value.excludedMuscleIds)
            assertEquals(events, fixture.recorder.names)
            assertTrue(fixture.profiles.isEmpty())
            assertTrue(fixture.domain.calls.isEmpty())
            assertEquals(0, fixture.homeNavigations)
        }
    }

    @Test
    fun acceptedMissingEquipmentSeedsANewChildWithoutRestartingTheCanceledCreate() = runTest {
        withProfileRestorationFixture { fixture ->
            enterMuscles(fixture)
            fixture.model<ExcludedMusclesViewModel>().onNextClick()
            runCurrent()
            val original = fixture.model<MissingEquipmentsViewModel>()
            original.onEquipmentClick("e2")
            original.onNextClick()
            runCurrent()
            val completed = fixture.model<ProfileCompletedViewModel>()
            val expected = CreateUserProfile("Draft person", 84.6f, ExperienceEnum.ADVANCED, 187,
                emptyList(), listOf("e2"))
            assertEquals(listOf(expected), fixture.profiles)
            assertEquals(setOf<ProfileCompletedLoader>(ProfileCompletedLoader.ProfileCreation), completed.loaders.value)
            completed.onBack()
            runCurrent()
            assertTrue(completed.loaders.value.isEmpty())
            fixture.model<MissingEquipmentsViewModel>().onBack()
            runCurrent()
            assertEquals(ProfileCreationRouter.ExcludedMuscles, fixture.component.childStack.value.active.configuration)
            assertEquals(listOf("e2"),
                fixture.model<ProfileCreationViewModel>(fixture.component).state.value.missingEquipmentIds)

            fixture.restore()
            runCurrent()
            assertEquals(listOf("e2"),
                fixture.model<ProfileCreationViewModel>(fixture.component).state.value.missingEquipmentIds)
            fixture.model<ExcludedMusclesViewModel>().onNextClick()
            runCurrent()
            val entered = fixture.model<MissingEquipmentsViewModel>()
            assertNotSame(original, entered)
            assertEquals(listOf("e1", "e3"), entered.state.value.selectedEquipmentIds)
            assertEquals(listOf("e1", "e2", "e3"), entered.state.value.knownEquipmentIds)
            assertEquals(listOf(expected), fixture.profiles)
            assertTrue(fixture.domain.calls.isEmpty())
            assertTrue(fixture.welcomeStatuses.isEmpty())
            assertEquals(0, fixture.homeNavigations)
            val operations = fixture.recorder.events.filter { it.text("profile_action") == "create" }
            assertEquals(listOf("profile_operation_started", "profile_operation_interrupted"),
                operations.map { it.event.schema.name })
            assertNotNull(operations[0].operation)
            assertEquals(operations[0].operation, operations[1].operation)

            entered.onEquipmentClick("e2")
            val selected = entered.state.value.selectedEquipmentIds
            val events = fixture.recorder.names
            fixture.restore { fixture.equipments.value = emptyList() }
            runCurrent()
            val restoredLocal = fixture.model<MissingEquipmentsViewModel>()
            assertNotSame(entered, restoredLocal)
            assertTrue(restoredLocal.state.value.suggestions.isEmpty())
            assertEquals(selected, restoredLocal.state.value.selectedEquipmentIds)
            assertEquals(listOf("e1", "e2", "e3"), restoredLocal.state.value.knownEquipmentIds)
            assertEquals(listOf("e2"),
                fixture.model<ProfileCreationViewModel>(fixture.component).state.value.missingEquipmentIds)
            fixture.equipments.value = listOf(
                ProfileCreationRestorationFixture.equipments("first", "e1", "e2"),
                ProfileCreationRestorationFixture.equipments("second", "e3"),
            )
            runCurrent()
            assertEquals(listOf("e1", "e2", "e3"), restoredLocal.state.value.selectedEquipmentIds)
            assertEquals(listOf("e2"),
                fixture.model<ProfileCreationViewModel>(fixture.component).state.value.missingEquipmentIds)
            assertEquals(events, fixture.recorder.names)
            assertEquals(listOf(expected), fixture.profiles)
            assertTrue(fixture.domain.calls.isEmpty())
            assertTrue(fixture.welcomeStatuses.isEmpty())
            assertEquals(0, fixture.homeNavigations)
        }
    }

    private fun TestScope.enterExperience(fixture: ProfileCreationRestorationFixture) {
        fixture.model<UserViewModel>().onNameChange("Draft person")
        fixture.measurements(84.6f, 187)
        fixture.model<UserViewModel>().onNextClick()
        runCurrent()
        assertEquals(ProfileCreationRouter.Experience, fixture.component.childStack.value.active.configuration)
    }

    private fun TestScope.enterMuscles(fixture: ProfileCreationRestorationFixture) {
        enterExperience(fixture)
        fixture.model<ExperienceViewModel>().onExperienceClick(ExperienceEnumState.ADVANCED)
        fixture.model<ExperienceViewModel>().onNextClick()
        runCurrent()
        assertIs<ProfileCreationComponent.Child.ExcludedMuscles>(fixture.component.childStack.value.active.instance)
    }
}
