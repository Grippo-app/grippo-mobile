package com.grippo.profile

import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.core.state.profile.PersonalizationKeyEnumState
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.goal.models.GoalPrimaryGoalEnum
import com.grippo.data.features.api.goal.models.GoalSecondaryGoalEnum
import com.grippo.data.features.api.goal.models.PersonalizationKeyEnum
import com.grippo.dialog.api.DialogConfig
import com.grippo.profile.body.ProfileBodyState
import com.grippo.profile.body.ProfileBodyViewModel
import com.grippo.profile.equipments.ProfileEquipmentsState
import com.grippo.profile.equipments.ProfileEquipmentsViewModel
import com.grippo.profile.experience.ProfileExperienceState
import com.grippo.profile.experience.ProfileExperienceViewModel
import com.grippo.profile.goal.ProfileGoalState
import com.grippo.profile.goal.ProfileGoalViewModel
import com.grippo.profile.menu.ProfileMenuViewModel
import com.grippo.profile.muscles.ProfileMusclesState
import com.grippo.profile.muscles.ProfileMusclesViewModel
import com.grippo.screen.api.ProfileRouter
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

class ProfileStateRestorationTest {
    @Test
    fun everyIndependentProfileScreenPresentsOnceAndRestoresWithoutContainerViews() = runTest {
        val screens = listOf(
            ProfileRouter.Menu to "profile_menu",
            ProfileRouter.Body to "profile_body",
            ProfileRouter.Goal to "profile_goal",
            ProfileRouter.Experience to "profile_experience",
            ProfileRouter.Muscles to "profile_muscles",
            ProfileRouter.Equipments to "profile_equipments",
            ProfileRouter.Settings to "profile_settings",
            ProfileRouter.Social to "profile_social",
        )
        for ((route, identifier) in screens) {
            val presentations = mutableListOf<ProfileRestorationFixture.View>()
            withProfileRestorationFixture(route) { fixture ->
                assertEquals(identifier, fixture.views.single().navigation.screen.analyticsCode)
                assertEquals(NavigationReason.Initial, fixture.views.single().navigation.reason)
                assertEquals(1, fixture.screenBindings)
                fixture.archive()
                assertEquals(1, fixture.views.size)
                fixture.restore()
                runCurrent()
                assertEquals(listOf(identifier, identifier), fixture.views.map { it.navigation.screen.analyticsCode })
                assertEquals(listOf(NavigationReason.Initial, NavigationReason.Initial), fixture.views.map { it.navigation.reason })
                assertEquals(2, fixture.screenBindings)
                assertEquals(1, fixture.views.first().closes)
                assertEquals(0, fixture.views.last().closes)
                fixture.views.forEach { view ->
                    assertNull(view.navigation.referrer)
                    assertNull(view.navigation.dialog)
                    assertNull(view.flow)
                }
                presentations += fixture.views
                fixture.assertNoBusinessWrites()
            }
            assertEquals(listOf(1, 1), presentations.map { it.closes })
        }
    }

    @Test
    fun pauseAndResumeUseRestoreReasonAndCloseEachPresentationOnce() = runTest {
        val presentations = mutableListOf<ProfileRestorationFixture.View>()
        withProfileRestorationFixture(ProfileRouter.Experience) { fixture ->
            fixture.pause()
            runCurrent()
            assertEquals(1, fixture.views.single().closes)
            fixture.resume()
            runCurrent()
            assertEquals(listOf(NavigationReason.Initial, NavigationReason.Restore), fixture.views.map { it.navigation.reason })
            assertEquals(1, fixture.screenBindings)
            fixture.pause()
            runCurrent()
            assertEquals(listOf(1, 1), fixture.views.map { it.closes })
            presentations += fixture.views
        }
        assertEquals(listOf(1, 1), presentations.map { it.closes })
    }

    @Test
    fun navigatingBackPresentsOnlyTheResumedLeafAndSavingAddsNoViews() = runTest {
        withProfileRestorationFixture { fixture ->
            fixture.open(ProfileMenu.Body)
            runCurrent()
            fixture.open(ProfileMenu.Goal)
            runCurrent()
            fixture.model<ProfileGoalViewModel>().onBack()
            runCurrent()
            assertEquals(
                listOf("profile_menu", "profile_body", "profile_goal", "profile_body"),
                fixture.views.map { it.navigation.screen.analyticsCode },
            )
            assertEquals(NavigationReason.Restore, fixture.views.last().navigation.reason)
            assertEquals(3, fixture.screenBindings)
            val views = fixture.views.size
            val presentations = fixture.presentationAttempts.size
            fixture.archive()
            fixture.archive()
            assertEquals(views, fixture.views.size)
            assertEquals(presentations, fixture.presentationAttempts.size)
        }
    }

    @Test
    fun ordinaryPresentationFailureDoesNotBlockEditingOrBusinessCompletion() = runTest {
        withProfileRestorationFixture(ProfileRouter.Body) { fixture ->
            fixture.pause()
            fixture.presentationFailure = IllegalStateException("Reporter unavailable")
            fixture.resume()
            runCurrent()
            val model = fixture.model<ProfileBodyViewModel>()
            model.onHeightPickerClick()
            assertIs<DialogConfig.HeightPicker>(fixture.dialogs.last()).onResult(184)
            model.onApplyClick()
            advanceUntilIdle()
            assertEquals(listOf(184), fixture.savedHeights)
            assertEquals(1, fixture.closeCalls)
            assertEquals(1, fixture.views.size)
            assertEquals(2, fixture.presentationAttempts.size)
            fixture.presentationFailure = null
        }
    }

    @Test
    fun ordinaryCloseFailureDoesNotBlockTheNextScreenPresentation() = runTest {
        withProfileRestorationFixture(ProfileRouter.Experience) { fixture ->
            fixture.closeFailure = IllegalStateException("Close unavailable")
            fixture.pause()
            fixture.resume()
            runCurrent()
            assertEquals(listOf(1, 0), fixture.views.map { it.closes })
            assertEquals(NavigationReason.Restore, fixture.views.last().navigation.reason)
            fixture.closeFailure = null
        }
    }

    @Test
    fun fatalPresentationFailureRemainsFatalAtTheLifecycleBoundary() = runTest {
        withProfileRestorationFixture(ProfileRouter.Experience) { fixture ->
            fixture.pause()
            val failure = AssertionError("Fatal reporter failure")
            fixture.presentationFailure = failure
            try {
                assertSame(failure, assertFailsWith<AssertionError> { fixture.resume() })
            } finally {
                fixture.presentationFailure = null
            }
        }
    }

    @Test
    fun fatalCloseFailureRemainsFatalAtTheLifecycleBoundary() = runTest {
        withProfileRestorationFixture(ProfileRouter.Experience) { fixture ->
            val failure = AssertionError("Fatal close failure")
            fixture.closeFailure = failure
            try {
                assertSame(failure, assertFailsWith<AssertionError> { fixture.pause() })
            } finally {
                fixture.closeFailure = null
            }
        }
    }

    @Test
    fun bodyDraftSurvivesFreshRootAndSuppliesFinalBusinessRequests() = runTest {
        withProfileRestorationFixture { fixture ->
            fixture.open(ProfileMenu.Body)
            runCurrent()
            val previous = fixture.model<ProfileBodyViewModel>()
            previous.onWeightPickerClick()
            assertIs<DialogConfig.WeightPicker>(fixture.dialogs.last()).onResult(81.5f)
            previous.onHeightPickerClick()
            assertIs<DialogConfig.HeightPicker>(fixture.dialogs.last()).onResult(185)
            fixture.restore { fixture.users.value = ProfileRestorationFixture.user(weight = 72f, height = 172) }
            runCurrent()

            val restored = fixture.model<ProfileBodyViewModel>()
            assertNotSame(previous, restored)
            assertEquals(WeightFormatState.of(81.5f), restored.state.value.weight)
            assertEquals(HeightFormatState.of(185), restored.state.value.height)
            assertEquals(72f, restored.state.value.user?.weight?.value)
            fixture.assertNoBusinessWrites()

            restored.onApplyClick()
            advanceUntilIdle()
            assertEquals(listOf(81.5f), fixture.savedWeights)
            assertEquals(listOf(185), fixture.savedHeights)
            assertEquals(1, fixture.notifications.size)
            assertEquals(ProfileRouter.Menu, fixture.component.childStack.value.active.configuration)
        }
    }

    @Test
    fun bodyKeepsTheWholeDraftWhileRebuildingCurrentUserMetadata() = runTest {
        withProfileRestorationFixture(ProfileRouter.Body) { fixture ->
            val model = fixture.model<ProfileBodyViewModel>()
            model.onWeightPickerClick()
            assertIs<DialogConfig.WeightPicker>(fixture.dialogs.last()).onResult(82f)
            fixture.users.value = ProfileRestorationFixture.user(weight = 73f, height = 180)
            runCurrent()
            assertEquals(82f, model.state.value.weight.value)
            assertEquals(170, model.state.value.height.value)
            assertEquals(180, model.state.value.user?.height?.value)

            fixture.restore { fixture.users.value = null }
            runCurrent()
            fixture.model<ProfileBodyViewModel>().onApplyClick()
            runCurrent()
            fixture.assertNoBusinessWrites()
            fixture.users.value = ProfileRestorationFixture.user(weight = 74f, height = 182)
            runCurrent()
            val restored = fixture.model<ProfileBodyViewModel>()
            assertEquals(82f, restored.state.value.weight.value)
            assertEquals(170, restored.state.value.height.value)
        }
    }

    @Test
    fun bodyPreservesEmptyAndInvalidFormatStatesExactly() = runTest {
        withProfileRestorationFixture(ProfileRouter.Body) { fixture ->
            val model = fixture.model<ProfileBodyViewModel>()
            model.onWeightPickerClick()
            assertIs<DialogConfig.WeightPicker>(fixture.dialogs.last()).onResult(0f)
            model.onHeightPickerClick()
            assertIs<DialogConfig.HeightPicker>(fixture.dialogs.last()).onResult(-1)
            val weight = model.state.value.weight
            val height = model.state.value.height
            assertIs<WeightFormatState.Empty>(weight)
            assertIs<HeightFormatState.Invalid>(height)
            fixture.restore()
            runCurrent()
            val restored = fixture.model<ProfileBodyViewModel>()
            assertEquals(weight, restored.state.value.weight)
            assertEquals(height, restored.state.value.height)
            fixture.assertNoBusinessWrites()
        }
    }

    @Test
    fun restoredBodyKeepsTheCompleteSavedSnapshotAndRebuildsUserBaseline() = runTest {
        withProfileRestorationFixture(ProfileRouter.Body) { fixture ->
            fixture.restore { fixture.users.value = ProfileRestorationFixture.user(weight = 75f, height = 177) }
            runCurrent()
            val restored = fixture.model<ProfileBodyViewModel>()
            assertEquals(70f, restored.state.value.weight.value)
            assertEquals(170, restored.state.value.height.value)
            assertEquals(75f, restored.state.value.user?.weight?.value)
            assertEquals(177, restored.state.value.user?.height?.value)
        }
    }

    @Test
    fun bodyPreservesPreloadEditsAndKeepsSourceEqualDraftValuesOnRefresh() = runTest {
        withProfileRestorationFixture { fixture ->
            fixture.users.value = null
            runCurrent()
            fixture.open(ProfileMenu.Body)
            runCurrent()
            val model = fixture.model<ProfileBodyViewModel>()
            model.onWeightPickerClick()
            assertIs<DialogConfig.WeightPicker>(fixture.dialogs.last()).onResult(82f)
            fixture.users.value = ProfileRestorationFixture.user(weight = 75f, height = 177)
            runCurrent()
            assertEquals(82f, model.state.value.weight.value)
            assertEquals(177, model.state.value.height.value)

            model.onWeightPickerClick()
            assertIs<DialogConfig.WeightPicker>(fixture.dialogs.last()).onResult(75f)
            model.onHeightPickerClick()
            assertIs<DialogConfig.HeightPicker>(fixture.dialogs.last()).onResult(177)
            fixture.users.value = ProfileRestorationFixture.user(weight = 76f, height = 180)
            runCurrent()
            assertEquals(75f, model.state.value.weight.value)
            assertEquals(177, model.state.value.height.value)
            assertEquals(76f, model.state.value.user?.weight?.value)
            assertEquals(180, model.state.value.user?.height?.value)
            fixture.assertNoBusinessWrites()
        }
    }

    @Test
    fun restoredDefaultBodyAndNullExperienceAreAuthoritativeDrafts() = runTest {
        withProfileRestorationFixture(ProfileRouter.Body) { fixture ->
            val keeper = fixture.component.childStack.value.active.instance.component.stateKeeper
            keeper.unregister("ProfileBodyDraft")
            keeper.register("ProfileBodyDraft", ProfileBodyState.serializer()) { ProfileBodyState() }
            fixture.restore()
            runCurrent()
            val restored = fixture.model<ProfileBodyViewModel>().state.value
            assertEquals(WeightFormatState.Empty(), restored.weight)
            assertEquals(HeightFormatState.Empty(), restored.height)
            assertEquals(70f, restored.user?.weight?.value)
            fixture.model<ProfileBodyViewModel>().onApplyClick()
            runCurrent()
            fixture.assertNoBusinessWrites()
        }
        withProfileRestorationFixture(ProfileRouter.Experience) { fixture ->
            val keeper = fixture.component.childStack.value.active.instance.component.stateKeeper
            keeper.unregister("ProfileExperienceDraft")
            keeper.register("ProfileExperienceDraft", ProfileExperienceState.serializer()) { ProfileExperienceState() }
            fixture.restore()
            runCurrent()
            val restored = fixture.model<ProfileExperienceViewModel>()
            assertNull(restored.state.value.selected)
            restored.onApply()
            runCurrent()
            fixture.assertNoBusinessWrites()
        }
    }

    @Test
    fun goalAllEditedSelectionsSurviveAndSupplyFinalRequest() = runTest {
        withProfileRestorationFixture { fixture ->
            fixture.open(ProfileMenu.Goal)
            runCurrent()
            val model = fixture.model<ProfileGoalViewModel>()
            model.onPrimaryGoalPickerClick()
            assertIs<DialogConfig.PrimaryGoalPicker>(fixture.dialogs.last()).onResult(GoalPrimaryGoalEnumState.GET_STRONGER)
            model.onSecondaryGoalPickerClick()
            assertIs<DialogConfig.SecondaryGoalPicker>(fixture.dialogs.last()).onResult(GoalSecondaryGoalEnumState.MAINTAIN)
            model.onTargetDatePickerClick()
            val target = model.state.value.limitations.to
            assertIs<DialogConfig.DatePicker>(fixture.dialogs.last()).onResult(target)
            model.onPersonalizationClick(PersonalizationKeyEnumState.LOW_FREQUENCY)
            assertTrue(model.state.value.selectedPersonalization.isEmpty())

            fixture.restore { fixture.goals.value = ProfileRestorationFixture.goal(GoalPrimaryGoalEnum.LOSE_FAT) }
            runCurrent()
            val restored = fixture.model<ProfileGoalViewModel>()
            assertNotSame(model, restored)
            assertEquals(GoalPrimaryGoalEnumState.GET_STRONGER, restored.state.value.selectedPrimary)
            assertEquals(GoalSecondaryGoalEnumState.MAINTAIN, restored.state.value.selectedSecondary)
            assertEquals(target, restored.state.value.selectedTarget.value)
            assertTrue(restored.state.value.selectedPersonalization.isEmpty())
            fixture.assertNoBusinessWrites()

            restored.onSave()
            runCurrent()
            val request = fixture.savedGoals.single()
            assertEquals(GoalPrimaryGoalEnum.GET_STRONGER, request.primaryGoal)
            assertEquals(GoalSecondaryGoalEnum.MAINTAIN, request.secondaryGoal)
            assertEquals(target, request.target)
            assertTrue(request.personalizations.isEmpty())
            assertEquals(ProfileRouter.Menu, fixture.component.childStack.value.active.configuration)
        }
    }

    @Test
    fun goalKeepsTheWholeDraftAcrossSourceRefreshAndRestoration() = runTest {
        withProfileRestorationFixture(ProfileRouter.Goal) { fixture ->
            val model = fixture.model<ProfileGoalViewModel>()
            model.onPrimaryGoalPickerClick()
            assertIs<DialogConfig.PrimaryGoalPicker>(fixture.dialogs.last()).onResult(GoalPrimaryGoalEnumState.GET_STRONGER)
            val draft = model.state.value
            val newGoal = ProfileRestorationFixture.goal(GoalPrimaryGoalEnum.LOSE_FAT).copy(
                secondaryGoal = null,
                personalizations = listOf(PersonalizationKeyEnum.MODERATE_FREQUENCY, PersonalizationKeyEnum.MODERATE_FREQUENCY),
                target = LocalDateTime(2027, 5, 1, 12, 0),
            )
            fixture.goals.value = newGoal
            runCurrent()
            assertEquals(GoalPrimaryGoalEnumState.GET_STRONGER, model.state.value.selectedPrimary)
            assertEquals(draft.selectedSecondary, model.state.value.selectedSecondary)
            assertEquals(draft.selectedTarget, model.state.value.selectedTarget)
            assertEquals(draft.selectedPersonalization, model.state.value.selectedPersonalization)
            model.onPersonalizationClick(PersonalizationKeyEnumState.LOW_FREQUENCY)
            assertTrue(model.state.value.selectedPersonalization.isEmpty())
            val saved = model.state.value
            fixture.restore { fixture.goals.value = ProfileRestorationFixture.goal(GoalPrimaryGoalEnum.MAINTAIN) }
            runCurrent()
            val restored = fixture.model<ProfileGoalViewModel>().state.value
            assertEquals(saved.selectedPrimary, restored.selectedPrimary)
            assertEquals(saved.selectedSecondary, restored.selectedSecondary)
            assertEquals(saved.selectedTarget, restored.selectedTarget)
            assertEquals(saved.selectedPersonalization, restored.selectedPersonalization)
        }
    }

    @Test
    fun explicitSavedNullGoalSelectionsAndEmptyTargetAreNotReplacedBySource() = runTest {
        withProfileRestorationFixture(ProfileRouter.Goal) { fixture ->
            val keeper = fixture.component.childStack.value.active.instance.component.stateKeeper
            val draft = ProfileGoalState()
            // Current picker callbacks are non-nullable; seed the supported draft shape directly.
            keeper.unregister("ProfileGoalDraft")
            keeper.register("ProfileGoalDraft", ProfileGoalState.serializer()) { draft }
            fixture.restore()
            runCurrent()
            val restored = fixture.model<ProfileGoalViewModel>()
            assertNull(restored.state.value.selectedPrimary)
            assertNull(restored.state.value.selectedSecondary)
            assertNull(restored.state.value.selectedTarget.value)
            assertTrue(restored.state.value.selectedPersonalization.isEmpty())
            restored.onSave()
            runCurrent()
            fixture.assertNoBusinessWrites()
            assertEquals(ProfileRouter.Goal, fixture.component.childStack.value.active.configuration)
        }
    }

    @Test
    fun goalPreservesPreloadEditsAndKeepsSourceEqualDraftValuesOnRefresh() = runTest {
        withProfileRestorationFixture { fixture ->
            fixture.goals.value = null
            runCurrent()
            fixture.open(ProfileMenu.Goal)
            runCurrent()
            val model = fixture.model<ProfileGoalViewModel>()
            model.onPrimaryGoalPickerClick()
            assertIs<DialogConfig.PrimaryGoalPicker>(fixture.dialogs.last()).onResult(GoalPrimaryGoalEnumState.GET_STRONGER)
            fixture.goals.value = ProfileRestorationFixture.goal(GoalPrimaryGoalEnum.LOSE_FAT)
            runCurrent()
            assertEquals(GoalPrimaryGoalEnumState.GET_STRONGER, model.state.value.selectedPrimary)
            assertEquals(GoalSecondaryGoalEnumState.CONSISTENCY, model.state.value.selectedSecondary)
            assertEquals(fixture.goals.value?.target, model.state.value.selectedTarget.value)

            model.onPrimaryGoalPickerClick()
            assertIs<DialogConfig.PrimaryGoalPicker>(fixture.dialogs.last()).onResult(GoalPrimaryGoalEnumState.LOSE_FAT)
            model.onSecondaryGoalPickerClick()
            assertIs<DialogConfig.SecondaryGoalPicker>(fixture.dialogs.last()).onResult(GoalSecondaryGoalEnumState.CONSISTENCY)
            model.onTargetDatePickerClick()
            assertIs<DialogConfig.DatePicker>(fixture.dialogs.last()).onResult(fixture.goals.value!!.target)
            model.onPersonalizationClick(PersonalizationKeyEnumState.LOW_FREQUENCY)
            model.onPersonalizationClick(PersonalizationKeyEnumState.LOW_FREQUENCY)
            val draft = model.state.value
            val current = ProfileRestorationFixture.goal(GoalPrimaryGoalEnum.MAINTAIN).copy(
                secondaryGoal = null,
                target = LocalDateTime(2027, 5, 1, 12, 0),
                personalizations = listOf(PersonalizationKeyEnum.MODERATE_FREQUENCY),
            )
            fixture.goals.value = current
            runCurrent()
            assertEquals(draft.selectedPrimary, model.state.value.selectedPrimary)
            assertEquals(draft.selectedSecondary, model.state.value.selectedSecondary)
            assertEquals(draft.selectedTarget, model.state.value.selectedTarget)
            assertEquals(draft.selectedPersonalization, model.state.value.selectedPersonalization)
            fixture.assertNoBusinessWrites()
        }
    }

    @Test
    fun pauseAndResumeDoNotRepeatGoalOrExperienceDraftInitialization() = runTest {
        withProfileRestorationFixture(ProfileRouter.Goal) { fixture ->
            val model = fixture.model<ProfileGoalViewModel>()
            model.onPersonalizationClick(PersonalizationKeyEnumState.LOW_FREQUENCY)
            assertTrue(model.state.value.selectedPersonalization.isEmpty())
            val draft = model.state.value
            fixture.pause()
            advanceTimeBy(1001)
            runCurrent()
            fixture.goals.value = ProfileRestorationFixture.goal(GoalPrimaryGoalEnum.MAINTAIN).copy(
                personalizations = listOf(PersonalizationKeyEnum.MODERATE_FREQUENCY),
            )
            fixture.resume()
            runCurrent()
            assertSame(model, fixture.model<ProfileGoalViewModel>())
            assertEquals(draft.selectedPrimary, model.state.value.selectedPrimary)
            assertEquals(draft.selectedSecondary, model.state.value.selectedSecondary)
            assertEquals(draft.selectedTarget, model.state.value.selectedTarget)
            assertTrue(model.state.value.selectedPersonalization.isEmpty())
            fixture.assertNoBusinessWrites()
        }
        withProfileRestorationFixture(ProfileRouter.Experience) { fixture ->
            val model = fixture.model<ProfileExperienceViewModel>()
            model.onSelectExperience(ExperienceEnumState.INTERMEDIATE)
            fixture.pause()
            advanceTimeBy(1001)
            runCurrent()
            fixture.users.value = ProfileRestorationFixture.user(experience = ExperienceEnum.PRO)
            fixture.resume()
            runCurrent()
            assertSame(model, fixture.model<ProfileExperienceViewModel>())
            assertEquals(ExperienceEnumState.INTERMEDIATE, model.state.value.selected)
            fixture.assertNoBusinessWrites()
        }
    }

    @Test
    fun unconfirmedGoalPickerDoesNotBecomeAnAcceptedDraft() = runTest {
        withProfileRestorationFixture(ProfileRouter.Goal) { fixture ->
            val model = fixture.model<ProfileGoalViewModel>()
            model.onPrimaryGoalPickerClick()
            val dialogs = fixture.dialogs.size
            fixture.restore { fixture.goals.value = ProfileRestorationFixture.goal(GoalPrimaryGoalEnum.MAINTAIN) }
            runCurrent()
            assertEquals(dialogs, fixture.dialogs.size)
            assertEquals(GoalPrimaryGoalEnumState.BUILD_MUSCLE, fixture.model<ProfileGoalViewModel>().state.value.selectedPrimary)
            fixture.assertNoBusinessWrites()
        }
    }

    @Test
    fun goalFreshDatePrefillIsNotReplacedDuringEmptySourceRestoration() = runTest {
        withProfileRestorationFixture { fixture ->
            fixture.goals.value = null
            runCurrent()
            fixture.open(ProfileMenu.Goal)
            runCurrent()
            val target = fixture.model<ProfileGoalViewModel>().state.value.selectedTarget
            assertTrue(target.value != null)
            assertTrue("\"selectedTarget\"" in Json.encodeToString(fixture.model<ProfileGoalViewModel>().state.value))
            fixture.restore()
            runCurrent()
            assertEquals(target, fixture.model<ProfileGoalViewModel>().state.value.selectedTarget)
        }
    }

    @Test
    fun experienceEditedSelectionSurvivesNullThenCurrentUserAndApplies() = runTest {
        withProfileRestorationFixture { fixture ->
            fixture.open(ProfileMenu.Experience)
            runCurrent()
            val previous = fixture.model<ProfileExperienceViewModel>()
            previous.onSelectExperience(ExperienceEnumState.ADVANCED)
            fixture.restore { fixture.users.value = null }
            runCurrent()
            val restored = fixture.model<ProfileExperienceViewModel>()
            assertNotSame(previous, restored)
            assertEquals(ExperienceEnumState.ADVANCED, restored.state.value.selected)
            fixture.users.value = ProfileRestorationFixture.user(experience = ExperienceEnum.INTERMEDIATE)
            runCurrent()
            assertEquals(ExperienceEnumState.ADVANCED, restored.state.value.selected)
            fixture.assertNoBusinessWrites()
            restored.onApply()
            runCurrent()
            assertEquals(listOf(ExperienceEnum.ADVANCED), fixture.savedExperiences)
            assertEquals(ProfileRouter.Menu, fixture.component.childStack.value.active.configuration)
        }
    }

    @Test
    fun restoredExperienceKeepsTheCompleteSavedSelection() = runTest {
        withProfileRestorationFixture(ProfileRouter.Experience) { fixture ->
            fixture.restore { fixture.users.value = ProfileRestorationFixture.user(experience = ExperienceEnum.PRO) }
            runCurrent()
            assertEquals(ExperienceEnumState.BEGINNER, fixture.model<ProfileExperienceViewModel>().state.value.selected)
        }
    }

    @Test
    fun experiencePreservesPreloadEditsAndKeepsSourceEqualDraftValuesOnRefresh() = runTest {
        withProfileRestorationFixture { fixture ->
            fixture.users.value = null
            runCurrent()
            fixture.open(ProfileMenu.Experience)
            runCurrent()
            val model = fixture.model<ProfileExperienceViewModel>()
            model.onSelectExperience(ExperienceEnumState.ADVANCED)
            fixture.users.value = ProfileRestorationFixture.user(experience = ExperienceEnum.INTERMEDIATE)
            runCurrent()
            assertEquals(ExperienceEnumState.ADVANCED, model.state.value.selected)
            model.onSelectExperience(ExperienceEnumState.INTERMEDIATE)
            fixture.users.value = ProfileRestorationFixture.user(experience = ExperienceEnum.PRO)
            runCurrent()
            assertEquals(ExperienceEnumState.INTERMEDIATE, model.state.value.selected)
            fixture.assertNoBusinessWrites()
        }
    }

    @Test
    fun musclesDraftSurvivesEmptyCatalogAndSelectsNewItemsByCurrentPolicy() = runTest {
        withProfileRestorationFixture { fixture ->
            fixture.open(ProfileMenu.Muscles)
            runCurrent()
            val previous = fixture.model<ProfileMusclesViewModel>()
            previous.onGroupClick("second")
            previous.onSelect("m1")
            fixture.restore { fixture.muscles.value = emptyList() }
            runCurrent()
            var restored = fixture.model<ProfileMusclesViewModel>()
            assertEquals("second", restored.state.value.selectedGroupId)
            assertFalse("m1" in restored.state.value.selectedMuscleIds)
            restored.onApply()
            runCurrent()
            fixture.assertNoBusinessWrites()
            assertEquals(ProfileRouter.Muscles, fixture.component.childStack.value.active.configuration)
            fixture.restore()
            runCurrent()
            restored = fixture.model<ProfileMusclesViewModel>()
            assertEquals("second", restored.state.value.selectedGroupId)
            assertFalse("m1" in restored.state.value.selectedMuscleIds)
            val newGroup = ProfileRestorationFixture.muscles("second", "m3", "m4")
            fixture.excludedMuscles.value = listOf(newGroup.muscles.last())
            fixture.muscles.value = listOf(newGroup, ProfileRestorationFixture.muscles("first", "m1", "m2"))
            runCurrent()
            assertEquals("second", restored.state.value.selectedGroupId)
            assertEquals(setOf("m2", "m3"), restored.state.value.selectedMuscleIds.toSet())
            fixture.assertNoBusinessWrites()
            restored.onApply()
            runCurrent()
            assertEquals(listOf(listOf("m4", "m1")), fixture.savedExcludedMuscles)
            assertEquals(ProfileRouter.Menu, fixture.component.childStack.value.active.configuration)
        }
    }

    @Test
    fun equipmentsDraftSurvivesEmptyCatalogAndSelectsNewItemsByCurrentPolicy() = runTest {
        withProfileRestorationFixture { fixture ->
            fixture.open(ProfileMenu.Equipment)
            runCurrent()
            val previous = fixture.model<ProfileEquipmentsViewModel>()
            previous.onSelectGroup("second")
            previous.onSelectEquipment("e1")
            fixture.restore { fixture.equipments.value = emptyList() }
            runCurrent()
            var restored = fixture.model<ProfileEquipmentsViewModel>()
            assertEquals("second", restored.state.value.selectedGroupId)
            assertFalse("e1" in restored.state.value.selectedEquipmentIds)
            restored.onApply()
            runCurrent()
            fixture.assertNoBusinessWrites()
            assertEquals(ProfileRouter.Equipments, fixture.component.childStack.value.active.configuration)
            fixture.restore()
            runCurrent()
            restored = fixture.model<ProfileEquipmentsViewModel>()
            assertEquals("second", restored.state.value.selectedGroupId)
            assertFalse("e1" in restored.state.value.selectedEquipmentIds)
            fixture.equipments.value = listOf(ProfileRestorationFixture.equipments("second", "e3", "e4"), ProfileRestorationFixture.equipments("first", "e1", "e2"))
            runCurrent()
            assertEquals("second", restored.state.value.selectedGroupId)
            assertEquals(setOf("e2", "e3", "e4"), restored.state.value.selectedEquipmentIds.toSet())
            fixture.assertNoBusinessWrites()
            restored.onApply()
            runCurrent()
            assertEquals(listOf(listOf("e1")), fixture.savedExcludedEquipments)
            assertEquals(ProfileRouter.Menu, fixture.component.childStack.value.active.configuration)
        }
    }

    @Test
    fun emptyMuscleSelectionStaysEmptyAcrossExcludedSourceRefresh() = runTest {
        withProfileRestorationFixture(ProfileRouter.Muscles) { fixture ->
            val previous = fixture.model<ProfileMusclesViewModel>()
            previous.state.value.selectedMuscleIds.toList().forEach(previous::onSelect)
            fixture.restore()
            runCurrent()
            fixture.excludedMuscles.value = listOf(fixture.muscles.value.first().muscles.first())
            runCurrent()
            assertTrue(fixture.model<ProfileMusclesViewModel>().state.value.selectedMuscleIds.isEmpty())
        }
    }

    @Test
    fun emptyEquipmentSelectionStaysEmptyAcrossExcludedSourceRefresh() = runTest {
        withProfileRestorationFixture(ProfileRouter.Equipments) { fixture ->
            val previous = fixture.model<ProfileEquipmentsViewModel>()
            previous.state.value.selectedEquipmentIds.toList().forEach(previous::onSelectEquipment)
            fixture.restore()
            runCurrent()
            fixture.excludedEquipments.value = listOf(fixture.equipments.value.first().equipments.first())
            runCurrent()
            assertTrue(fixture.model<ProfileEquipmentsViewModel>().state.value.selectedEquipmentIds.isEmpty())
        }
    }

    @Test
    fun restoredCatalogSelectionKeepsKnownValuesAndRebuildsNewItemsAndGroup() = runTest {
        withProfileRestorationFixture(ProfileRouter.Muscles) { fixture ->
            fixture.model<ProfileMusclesViewModel>().onGroupClick("second")
            fixture.restore {
                fixture.muscles.value = listOf(ProfileRestorationFixture.muscles("fresh", "m2", "m4"))
                fixture.excludedMuscles.value = listOf(fixture.muscles.value.single().muscles.first())
            }
            runCurrent()
            val restored = fixture.model<ProfileMusclesViewModel>().state.value
            assertEquals("fresh", restored.selectedGroupId)
            assertEquals(listOf("m2", "m4"), restored.selectedMuscleIds)
        }
    }

    @Test
    fun muscleKnownChoicesStayDraftOwnedAcrossExclusionAndEmptyCatalogRefreshes() = runTest {
        withProfileRestorationFixture(ProfileRouter.Muscles) { fixture ->
            val model = fixture.model<ProfileMusclesViewModel>()
            val catalog = fixture.muscles.value
            model.onSelect("m1")
            model.onSelect("m1")
            fixture.excludedMuscles.value = listOf(catalog.first().muscles.first())
            runCurrent()
            assertEquals(setOf("m1", "m2", "m3"), model.state.value.selectedMuscleIds.toSet())
            model.onSelect("m2")
            fixture.muscles.value = emptyList()
            runCurrent()
            fixture.excludedMuscles.value = listOf(catalog.last().muscles.single())
            fixture.muscles.value = catalog
            runCurrent()
            assertEquals(setOf("m1", "m3"), model.state.value.selectedMuscleIds.toSet())

            fixture.excludedMuscles.value = listOf(catalog.first().muscles.last())
            runCurrent()
            assertEquals(setOf("m1", "m3"), model.state.value.selectedMuscleIds.toSet())
            fixture.excludedMuscles.value = emptyList()
            runCurrent()
            assertEquals(setOf("m1", "m3"), model.state.value.selectedMuscleIds.toSet())
            model.onApply()
            runCurrent()
            assertEquals(listOf(listOf("m2")), fixture.savedExcludedMuscles)
        }
    }

    @Test
    fun equipmentKnownChoicesStayDraftOwnedAcrossExclusionAndEmptyCatalogRefreshes() = runTest {
        withProfileRestorationFixture(ProfileRouter.Equipments) { fixture ->
            val model = fixture.model<ProfileEquipmentsViewModel>()
            val catalog = fixture.equipments.value
            model.onSelectEquipment("e1")
            model.onSelectEquipment("e1")
            fixture.excludedEquipments.value = listOf(catalog.first().equipments.first())
            runCurrent()
            assertEquals(setOf("e1", "e2", "e3"), model.state.value.selectedEquipmentIds.toSet())
            model.onSelectEquipment("e2")
            fixture.equipments.value = emptyList()
            runCurrent()
            fixture.excludedEquipments.value = listOf(catalog.last().equipments.single())
            fixture.equipments.value = catalog
            runCurrent()
            assertEquals(setOf("e1", "e3"), model.state.value.selectedEquipmentIds.toSet())
            fixture.excludedEquipments.value = listOf(catalog.first().equipments.last())
            runCurrent()
            assertEquals(setOf("e1", "e3"), model.state.value.selectedEquipmentIds.toSet())
            fixture.excludedEquipments.value = emptyList()
            runCurrent()
            assertEquals(setOf("e1", "e3"), model.state.value.selectedEquipmentIds.toSet())
            model.onApply()
            runCurrent()
            assertEquals(listOf(listOf("e2")), fixture.savedExcludedEquipments)
        }
    }

    @Test
    fun archivedBackStackDraftIsRestoredAlongsideActiveScreen() = runTest {
        withProfileRestorationFixture { fixture ->
            fixture.open(ProfileMenu.Body)
            runCurrent()
            val previous = fixture.model<ProfileBodyViewModel>()
            previous.onWeightPickerClick()
            assertIs<DialogConfig.WeightPicker>(fixture.dialogs.last()).onResult(84f)
            fixture.open(ProfileMenu.Experience)
            runCurrent()
            fixture.model<ProfileExperienceViewModel>().onSelectExperience(ExperienceEnumState.PRO)
            val views = fixture.views.size
            fixture.restore()
            runCurrent()
            assertEquals(views + 1, fixture.views.size)
            assertEquals("profile_experience", fixture.views.last().navigation.screen.analyticsCode)
            assertEquals(NavigationReason.Initial, fixture.views.last().navigation.reason)
            fixture.model<ProfileExperienceViewModel>().onBack()
            runCurrent()
            assertEquals(views + 2, fixture.views.size)
            assertEquals("profile_body", fixture.views.last().navigation.screen.analyticsCode)
            assertEquals(NavigationReason.Initial, fixture.views.last().navigation.reason)
            assertEquals(ProfileRouter.Body, fixture.component.childStack.value.active.configuration)
            val restored = fixture.model<ProfileBodyViewModel>()
            assertNotSame(previous, restored)
            assertEquals(84f, restored.state.value.weight.value)
        }
    }

    @Test
    fun poppedChildDraftIsDiscardedAndFreshEntryUsesDomainSource() = runTest {
        withProfileRestorationFixture { fixture ->
            fixture.open(ProfileMenu.Body)
            runCurrent()
            val previous = fixture.model<ProfileBodyViewModel>()
            previous.onWeightPickerClick()
            assertIs<DialogConfig.WeightPicker>(fixture.dialogs.last()).onResult(85f)
            previous.onBack()
            runCurrent()
            fixture.users.value = ProfileRestorationFixture.user(weight = 76f)
            runCurrent()
            fixture.open(ProfileMenu.Body)
            runCurrent()
            val fresh = fixture.model<ProfileBodyViewModel>()
            assertNotSame(previous, fresh)
            assertEquals(76f, fresh.state.value.weight.value)
            fixture.assertNoBusinessWrites()
        }
    }

    @Test
    fun retainedRecreationKeepsExistingModelAndDraft() = runTest {
        withProfileRestorationFixture(ProfileRouter.Experience) { fixture ->
            val previous = fixture.model<ProfileExperienceViewModel>()
            previous.onSelectExperience(ExperienceEnumState.ADVANCED)
            fixture.recreateRetained()
            runCurrent()
            assertSame(previous, fixture.model<ProfileExperienceViewModel>())
            assertEquals(ExperienceEnumState.ADVANCED, previous.state.value.selected)
            fixture.assertNoBusinessWrites()
        }
    }

    @Test
    fun savingDoesNotRecreateActiveScreenOrSubmitBusinessOperations() = runTest {
        withProfileRestorationFixture(ProfileRouter.Goal) { fixture ->
            val child = fixture.component.childStack.value.active.instance
            val model = fixture.model<ProfileGoalViewModel>()
            fixture.archive()
            fixture.archive()
            assertSame(child, fixture.component.childStack.value.active.instance)
            assertSame(model, fixture.model<ProfileGoalViewModel>())
            fixture.assertNoBusinessWrites()
        }
    }

    @Test
    fun menuHasNoDraftAndRebuildsUpdatedDataAfterFreshRestoration() = runTest {
        withProfileRestorationFixture { fixture ->
            fixture.restore { fixture.users.value = ProfileRestorationFixture.user(weight = 78f) }
            runCurrent()
            assertEquals(ProfileRouter.Menu, fixture.component.childStack.value.active.configuration)
            assertEquals(78f, fixture.model<ProfileMenuViewModel>().state.value.user?.weight?.value)
            fixture.assertNoBusinessWrites()
        }
    }

    @Test
    fun removedEditMetadataDoesNotBreakFreshComponentDraftRestoration() = runTest {
        val drafts = listOf(
            Triple(ProfileRouter.Body, "ProfileBodyDraft", listOf("hasEditedWeight", "hasEditedHeight")),
            Triple(ProfileRouter.Goal, "ProfileGoalDraft", listOf("hasEditedPrimary", "hasEditedSecondary", "hasEditedTarget", "hasEditedPersonalization")),
            Triple(ProfileRouter.Experience, "ProfileExperienceDraft", listOf("selectionEdited")),
            Triple(ProfileRouter.Muscles, "ProfileMusclesDraft", listOf("selectionEdited")),
            Triple(ProfileRouter.Equipments, "ProfileEquipmentsDraft", listOf("selectionEdited")),
        )
        for ((route, key, flags) in drafts) {
            withProfileRestorationFixture(route) { fixture ->
                val previous = fixture.component.childStack.value.active.instance.component
                val saved = draftPayload(fixture, route)
                val legacy = JsonObject(saved + flags.associateWith { JsonPrimitive(true) })
                previous.stateKeeper.unregister(key)
                previous.stateKeeper.register(key, JsonObject.serializer()) { legacy }
                fixture.restore {
                    fixture.users.value = ProfileRestorationFixture.user(weight = 75f, height = 177, experience = ExperienceEnum.PRO)
                    fixture.goals.value = ProfileRestorationFixture.goal(GoalPrimaryGoalEnum.MAINTAIN)
                    fixture.excludedMuscles.value = listOf(fixture.muscles.value.first().muscles.first())
                    fixture.excludedEquipments.value = listOf(fixture.equipments.value.first().equipments.first())
                }
                runCurrent()
                assertNotSame(previous, fixture.component.childStack.value.active.instance.component)
                assertEquals(saved, draftPayload(fixture, route))
                fixture.assertNoBusinessWrites()
            }
        }
    }

    @Test
    fun malformedKnownDraftValueStillFailsStrictRestoration() = runTest {
        withProfileRestorationFixture(ProfileRouter.Body) { fixture ->
            val keeper = fixture.component.childStack.value.active.instance.component.stateKeeper
            val malformed = JsonObject(
                draftPayload(fixture, ProfileRouter.Body) + mapOf(
                    "weight" to JsonPrimitive("invalid-format-state"),
                    "hasEditedWeight" to JsonPrimitive(true),
                )
            )
            keeper.unregister("ProfileBodyDraft")
            keeper.register("ProfileBodyDraft", JsonObject.serializer()) { malformed }
            assertFailsWith<SerializationException> { fixture.restore() }
            fixture.assertNoBusinessWrites()
        }
    }

    @Test
    fun draftPayloadsExcludeRebuiltAndRuntimeFields() = runTest {
        withProfileRestorationFixture(ProfileRouter.Body) { fixture ->
            val body = Json.encodeToString(fixture.model<ProfileBodyViewModel>().state.value)
            assertFalse("user-catalog-only" in body)
            assertFalse("user@example.com" in body)
            assertFalse("\"history\"" in body)
            assertFalse("\"user\"" in body)
        }
        val goal = Json.encodeToString(ProfileGoalState())
        assertFalse("\"limitations\"" in goal)
        assertFalse("\"primaryGoals\"" in goal)
        assertFalse("\"secondaryGoals\"" in goal)
        assertFalse("\"personalization\"" in goal)
        assertFalse("\"suggestions\"" in Json.encodeToString(ProfileExperienceState()))
        assertFalse("\"suggestions\"" in Json.encodeToString(ProfileMusclesState()))
        assertFalse("\"suggestions\"" in Json.encodeToString(ProfileEquipmentsState()))
        val complete = Json { encodeDefaults = true }
        val drafts = listOf(
            complete.encodeToString(ProfileBodyState()),
            complete.encodeToString(ProfileGoalState()),
            complete.encodeToString(ProfileExperienceState()),
            complete.encodeToString(ProfileMusclesState()),
            complete.encodeToString(ProfileEquipmentsState()),
        )
        drafts.forEach { draft ->
            assertFalse("hasEdited" in draft)
            assertFalse("selectionEdited" in draft)
        }
        assertEquals(ProfileBodyState(), Json.decodeFromString(ProfileBodyState.serializer(), Json.encodeToString(ProfileBodyState())))
    }

    private fun draftPayload(fixture: ProfileRestorationFixture, route: ProfileRouter): JsonObject = when (route) {
        ProfileRouter.Body -> Json.encodeToJsonElement(fixture.model<ProfileBodyViewModel>().state.value)
        ProfileRouter.Goal -> Json.encodeToJsonElement(fixture.model<ProfileGoalViewModel>().state.value)
        ProfileRouter.Experience -> Json.encodeToJsonElement(fixture.model<ProfileExperienceViewModel>().state.value)
        ProfileRouter.Muscles -> Json.encodeToJsonElement(fixture.model<ProfileMusclesViewModel>().state.value)
        ProfileRouter.Equipments -> Json.encodeToJsonElement(fixture.model<ProfileEquipmentsViewModel>().state.value)
        else -> error("Only draft-owning screens have a saved draft")
    }.jsonObject
}
