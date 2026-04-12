package com.grippo.profile.body

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.data.features.api.weight.history.UpdateWeightUseCase
import com.grippo.data.features.api.weight.history.WeightHistoryFeature
import com.grippo.data.features.api.weight.history.models.WeightHistory
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.notification_weight_description
import com.grippo.design.resources.provider.notification_weight_title
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.user.toState
import com.grippo.screen.api.deeplink.Deeplink
import com.grippo.toolkit.local.notification.AppNotification
import com.grippo.toolkit.local.notification.NotificationKey
import com.grippo.toolkit.local.notification.NotificationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.days

internal class ProfileBodyViewModel(
    private val dialogController: DialogController,
    private val weightHistoryFeature: WeightHistoryFeature,
    private val userFeature: UserFeature,
    private val updateWeightUseCase: UpdateWeightUseCase,
    private val notificationManager: NotificationManager,
    private val stringProvider: StringProvider,
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

            delay(500)

            val notification = AppNotification(
                id = NotificationKey.ChangeWeight,
                title = stringProvider.get(Res.string.notification_weight_title),
                body = stringProvider.get(Res.string.notification_weight_description),
                deeplink = Deeplink.WeightHistory.key
            )
            notificationManager.show(notification, 3.days)

            navigateTo(ProfileBodyDirection.Back)
        }
    }

    override fun onBack() {
        navigateTo(ProfileBodyDirection.Back)
    }
}