package com.grippo.profile.body

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.data.features.api.weight.history.UpdateWeightUseCase
import com.grippo.data.features.api.weight.history.WeightHistoryFeature
import com.grippo.data.features.api.weight.history.models.WeightHistory
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.user.toState
import kotlinx.coroutines.flow.onEach

internal class ProfileBodyViewModel(
    private val dialogController: DialogController,
    private val weightHistoryFeature: WeightHistoryFeature,
    private val userFeature: UserFeature,
    private val updateWeightUseCase: UpdateWeightUseCase
) : BaseViewModel<ProfileBodyState, ProfileBodyDirection, ProfileBodyLoader>(
    ProfileBodyState()
), ProfileBodyContract {

    init {
        safeLaunch {
            weightHistoryFeature.getWeightHistory().getOrThrow()
        }

        safeLaunch {
            userFeature.getUser().getOrThrow()
        }

        userFeature
            .observeUser()
            .onEach(::provideUser)
            .safeLaunch()

        weightHistoryFeature
            .observeWeightHistory()
            .onEach(::provideWeightHistory)
            .safeLaunch()
    }

    private fun provideWeightHistory(value: List<WeightHistory>) {
        val history = value.toState()
        update { it.copy(history = history) }
    }

    private fun provideUser(value: User?) {
        val user = value?.toState() ?: return
        update {
            it.copy(
                weight = user.weight,
                height = user.height,
                user = user
            )
        }
    }

    override fun onHeightPickerClick() {
        val dialog = DialogConfig.HeightPicker(
            initial = state.value.height,
            onResult = { value -> update { it.copy(height = value) } }
        )
        dialogController.show(dialog)
    }

    override fun onWeightPickerClick() {
        val dialog = DialogConfig.WeightPicker(
            initial = state.value.weight,
            onResult = { value -> update { it.copy(weight = value) } }
        )
        dialogController.show(dialog)
    }

    override fun onApplyClick() {
        val weight = state.value.weight.value ?: return
        val height = state.value.height.value ?: return
        val user = state.value.user ?: return

        safeLaunch(
            loader = ProfileBodyLoader.ApplyBodyChanges
        ) {
            val hasNewWeight = user.weight.value != weight
            val hasNewHeight = user.height.value != height

            if (hasNewWeight) {
                updateWeightUseCase.execute(weight).getOrThrow()
            }

            if (hasNewHeight) {
                userFeature.setHeight(height).getOrThrow()
            }
        }
    }

    override fun onBack() {
        navigateTo(ProfileBodyDirection.Back)
    }
}