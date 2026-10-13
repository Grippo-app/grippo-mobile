package com.grippo.authorization.profile.creation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.grippo.authorization.analytics.RecordingAuthAnalytics
import com.grippo.authorization.analytics.factory
import com.grippo.authorization.profile.creation.experience.ExperienceDirection
import com.grippo.authorization.profile.creation.experience.ExperienceViewModel
import com.grippo.toolkit.analytics.transport.AnalyticsValue
import com.grippo.authorization.profile.creation.excluded.muscles.ExcludedMusclesDirection
import com.grippo.authorization.profile.creation.excluded.muscles.ExcludedMusclesViewModel
import com.grippo.authorization.profile.creation.missing.equipments.MissingEquipmentsDirection
import com.grippo.authorization.profile.creation.missing.equipments.MissingEquipmentsViewModel
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.CoreModule
import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.equipment.models.EquipmentEnum
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.data.features.api.equipment.models.EquipmentGroupEnum
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.ksp.generated.module

class ProfileSelectionViewModelTest {
    @Test
    fun profileCreationKeepsBusinessDataInStateAndEmitsOnlyDestinations() = runTest {
        withActiveViewModel(create = { ProfileCreationViewModel() }) { viewModel, directions ->
            viewModel.toExperienceWithUser(name = "Person", weight = 70f, height = 175)
            runCurrent()
            viewModel.toExcludedMusclesWithExperience(ExperienceEnumState.BEGINNER)
            runCurrent()

            val excludedMuscles = mutableListOf("muscle")
            viewModel.toMissingEquipmentWithMuscles(excludedMuscles)
            excludedMuscles.clear()
            runCurrent()

            val missingEquipments = mutableListOf("equipment")
            viewModel.toCompletedWithEquipment(missingEquipments)
            missingEquipments.clear()
            runCurrent()

            val state = viewModel.state.value
            assertEquals("Person", state.name)
            assertEquals(70f, state.weight)
            assertEquals(175, state.height)
            assertEquals(ExperienceEnumState.BEGINNER, state.experience)
            assertEquals(listOf("muscle"), state.excludedMuscleIds)
            assertEquals(listOf("equipment"), state.missingEquipmentIds)
            assertEquals(
                listOf(
                    ProfileCreationDirection.ToExperience,
                    ProfileCreationDirection.ToExcludedMuscles,
                    ProfileCreationDirection.ToMissingEquipments,
                    ProfileCreationDirection.ToCompleted,
                ),
                directions,
            )
        }
    }

    @Test
    fun muscleUpdatesKeepAnEmptySelectionAndSelectOnlyNewItems() = runTest {
        val groups = MutableSharedFlow<List<MuscleGroup>>()
        val feature = object : MuscleFeature {
            override fun observeMuscles(): Flow<List<MuscleGroup>> = groups
            override suspend fun getMuscles(): Result<Unit> = error("Unexpected catalog request")
        }

        val recorder = RecordingAuthAnalytics()
        val analytics = factory(recorder)
        withActiveViewModel(create = { ExcludedMusclesViewModel(analytics, feature) }) { viewModel, directions ->
            val initial = listOf(muscles("initial", "first", "second"), muscles("other"))
            groups.emit(initial)
            runCurrent()
            assertEquals(listOf("first", "second"), viewModel.state.value.selectedMuscleIds)
            assertEquals("initial", viewModel.state.value.selectedGroupId)
            assertTrue(recorder.events.isEmpty())

            viewModel.onGroupClick("other")
            assertEquals("other", viewModel.state.value.selectedGroupId)
            viewModel.onGroupClick("initial")
            assertEquals("initial", viewModel.state.value.selectedGroupId)
            assertEquals(listOf("first", "second"), viewModel.state.value.selectedMuscleIds)
            assertEquals(listOf("auth_control_clicked", "auth_control_clicked"), recorder.names)
            assertEquals(listOf("group_selected", "group_selected"), recorder.events
                .map { it.text("interaction_action") })

            viewModel.onSelect("first")
            assertEquals(listOf("second"), viewModel.state.value.selectedMuscleIds)
            viewModel.onSelect("first")
            assertEquals(listOf("second", "first"), viewModel.state.value.selectedMuscleIds)
            viewModel.onSelect("first")
            assertEquals(listOf("second"), viewModel.state.value.selectedMuscleIds)
            viewModel.onSelect("second")
            groups.emit(initial)
            runCurrent()
            assertTrue(viewModel.state.value.selectedMuscleIds.isEmpty())
            viewModel.onNextClick()
            runCurrent()
            assertEquals(
                ExcludedMusclesDirection.MissingEquipment(listOf("first", "second")),
                directions.single(),
            )

            groups.emit(listOf(muscles("replacement", "second", "third")))
            runCurrent()
            assertEquals(listOf("third"), viewModel.state.value.selectedMuscleIds)
            assertEquals("replacement", viewModel.state.value.selectedGroupId)
            viewModel.onNextClick()
            runCurrent()
            assertEquals(ExcludedMusclesDirection.MissingEquipment(listOf("second")), directions.last())

            groups.emit(emptyList())
            runCurrent()
            assertTrue(viewModel.state.value.suggestions.isEmpty())
            assertEquals(listOf("third"), viewModel.state.value.selectedMuscleIds)
            assertEquals("replacement", viewModel.state.value.selectedGroupId)
            groups.emit(listOf(muscles("replacement", "second", "third")))
            runCurrent()
            assertEquals(listOf("third"), viewModel.state.value.selectedMuscleIds)
            val selections = recorder.events.filter { it.event.schema.name == "auth_selection_changed" }
            assertEquals(listOf(0L to 1L, 1L to 2L, 0L to 1L, 0L to 0L), selections.map { captured ->
                val parameters = captured.event.parameters()
                (parameters.single { it.key.name == "selected" }.value as AnalyticsValue.Integer).value to
                    (parameters.single { it.key.name == "selected_count" }.value as AnalyticsValue.Integer).value
            })
            assertEquals(
                listOf("auth_control_clicked", "auth_control_clicked", "auth_selection_changed",
                    "auth_selection_changed", "auth_selection_changed", "auth_selection_changed",
                    "auth_control_clicked", "auth_control_clicked"),
                recorder.names,
            )
            assertEquals(listOf("group_selected", "group_selected", "next", "next"), recorder.events
                .filter { it.event.schema.name == "auth_control_clicked" }
                .map { it.text("interaction_action") })
            assertTrue(recorder.events.all {
                it.text("interaction_screen") == "profile_creation_excluded_muscles" && it.flow == null && it.operation == null
            })
            assertTrue(recorder.events.flatMap { it.event.parameters() }.all { parameter ->
                val value = parameter.value
                value !is AnalyticsValue.Text || value.value in setOf("profile_creation_excluded_muscles", "group_selected", "next")
            })
        }
    }

    @Test
    fun equipmentUpdatesKeepAnEmptySelectionAndSelectOnlyNewItems() = runTest {
        val groups = MutableSharedFlow<List<EquipmentGroup>>()
        val feature = object : EquipmentFeature {
            override fun observeEquipments(): Flow<List<EquipmentGroup>> = groups
            override suspend fun getEquipments(): Result<Unit> = error("Unexpected catalog request")
        }

        val recorder = RecordingAuthAnalytics()
        val analytics = factory(recorder)
        withActiveViewModel(create = { MissingEquipmentsViewModel(analytics, feature) }) { viewModel, directions ->
            val initial = listOf(equipments("initial", "first", "second"), equipments("other"))
            groups.emit(initial)
            runCurrent()
            assertEquals(listOf("first", "second"), viewModel.state.value.selectedEquipmentIds)
            assertEquals("initial", viewModel.state.value.selectedGroupId)
            assertTrue(recorder.events.isEmpty())

            viewModel.onGroupClick("other")
            assertEquals("other", viewModel.state.value.selectedGroupId)
            viewModel.onGroupClick("initial")
            assertEquals("initial", viewModel.state.value.selectedGroupId)
            assertEquals(listOf("first", "second"), viewModel.state.value.selectedEquipmentIds)
            assertEquals(listOf("auth_control_clicked", "auth_control_clicked"), recorder.names)
            assertEquals(listOf("group_selected", "group_selected"), recorder.events
                .map { it.text("interaction_action") })

            viewModel.onEquipmentClick("first")
            assertEquals(listOf("second"), viewModel.state.value.selectedEquipmentIds)
            viewModel.onEquipmentClick("first")
            assertEquals(listOf("second", "first"), viewModel.state.value.selectedEquipmentIds)
            viewModel.onEquipmentClick("first")
            assertEquals(listOf("second"), viewModel.state.value.selectedEquipmentIds)
            viewModel.onEquipmentClick("second")
            groups.emit(initial)
            runCurrent()
            assertTrue(viewModel.state.value.selectedEquipmentIds.isEmpty())
            viewModel.onNextClick()
            runCurrent()
            assertEquals(
                MissingEquipmentsDirection.Completed(listOf("first", "second")),
                directions.single(),
            )

            groups.emit(listOf(equipments("replacement", "second", "third")))
            runCurrent()
            assertEquals(listOf("third"), viewModel.state.value.selectedEquipmentIds)
            assertEquals("replacement", viewModel.state.value.selectedGroupId)
            viewModel.onNextClick()
            runCurrent()
            assertEquals(MissingEquipmentsDirection.Completed(listOf("second")), directions.last())

            groups.emit(emptyList())
            runCurrent()
            assertTrue(viewModel.state.value.suggestions.isEmpty())
            assertEquals(listOf("third"), viewModel.state.value.selectedEquipmentIds)
            assertEquals("replacement", viewModel.state.value.selectedGroupId)
            groups.emit(listOf(equipments("replacement", "second", "third")))
            runCurrent()
            assertEquals(listOf("third"), viewModel.state.value.selectedEquipmentIds)
            val selections = recorder.events.filter { it.event.schema.name == "auth_selection_changed" }
            assertEquals(listOf(0L to 1L, 1L to 2L, 0L to 1L, 0L to 0L), selections.map { captured ->
                val parameters = captured.event.parameters()
                (parameters.single { it.key.name == "selected" }.value as AnalyticsValue.Integer).value to
                    (parameters.single { it.key.name == "selected_count" }.value as AnalyticsValue.Integer).value
            })
            assertEquals(
                listOf("auth_control_clicked", "auth_control_clicked", "auth_selection_changed",
                    "auth_selection_changed", "auth_selection_changed", "auth_selection_changed",
                    "auth_control_clicked", "auth_control_clicked"),
                recorder.names,
            )
            assertEquals(listOf("group_selected", "group_selected", "next", "next"), recorder.events
                .filter { it.event.schema.name == "auth_control_clicked" }
                .map { it.text("interaction_action") })
            assertTrue(recorder.events.all {
                it.text("interaction_screen") == "profile_creation_missing_equipments" && it.flow == null && it.operation == null
            })
            assertTrue(recorder.events.flatMap { it.event.parameters() }.all { parameter ->
                val value = parameter.value
                value !is AnalyticsValue.Text || value.value in setOf("profile_creation_missing_equipments", "group_selected", "next")
            })
        }
    }

    @Test
    fun experienceSelectionReportsBoundedFactsWithoutChoosingBusinessNavigation() = runTest {
        val recorder = RecordingAuthAnalytics()
        val analytics = factory(recorder)
        withActiveViewModel(create = { ExperienceViewModel(analytics) }) { viewModel, directions ->
            viewModel.onNextClick()
            runCurrent()
            assertTrue(directions.isEmpty())
            assertTrue(recorder.events.isEmpty())

            viewModel.onExperienceClick(ExperienceEnumState.ADVANCED)
            assertEquals(ExperienceEnumState.ADVANCED, viewModel.state.value.selected)
            viewModel.onNextClick()
            runCurrent()
            assertEquals(ExperienceDirection.ExcludedMuscles(ExperienceEnumState.ADVANCED), directions.single())
            val selection = recorder.events.single { it.event.schema.name == "auth_experience_selected" }
            assertEquals("advanced", selection.text("profile_experience"))
            assertEquals("profile_creation_experience", selection.text("interaction_screen"))
            assertEquals(1, recorder.events.count { it.text("interaction_action") == "next" })
        }
    }

    private suspend fun <DIRECTION : BaseDirection, VM : BaseViewModel<*, DIRECTION, *>> TestScope.withActiveViewModel(
        create: () -> VM,
        verify: suspend (VM, List<DIRECTION>) -> Unit,
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        startKoin {
            modules(CoreModule().module)
        }
        val lifecycle = LifecycleRegistry()
        var viewModel: VM? = null
        try {
            val created = create()
            viewModel = created
            val host = ViewModelHost(DefaultComponentContext(lifecycle), created)
            lifecycle.resume()
            runCurrent()
            verify(created, host.directions)
        } finally {
            lifecycle.destroy()
            viewModel?.onDestroy()
            runCurrent()
            stopKoin()
            Dispatchers.resetMain()
        }
    }

    private fun muscles(groupId: String, vararg ids: String): MuscleGroup = MuscleGroup(
        id = groupId,
        name = groupId,
        muscles = ids.map { id ->
            Muscle(
                id = id,
                name = id,
                recovery = Duration.ZERO,
                size = 1f,
                sensitivity = 1f,
                type = MuscleEnum.BICEPS,
            )
        },
        type = MuscleGroupEnum.ARMS_AND_FOREARMS,
    )

    private fun equipments(groupId: String, vararg ids: String): EquipmentGroup = EquipmentGroup(
        id = groupId,
        name = groupId,
        type = EquipmentGroupEnum.FREE_WEIGHT,
        equipments = ids.map { id -> Equipment(id = id, name = id, type = EquipmentEnum.DUMBBELLS) },
    )

    private class ViewModelHost<DIRECTION : BaseDirection>(
        context: ComponentContext,
        override val viewModel: BaseViewModel<*, DIRECTION, *>,
    ) : BaseComponent<DIRECTION>(context) {
        val directions = mutableListOf<DIRECTION>()

        override suspend fun eventListener(direction: DIRECTION) {
            directions += direction
        }

        @Composable
        override fun Render() = Unit
    }
}
